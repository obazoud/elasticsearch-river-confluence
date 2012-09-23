package org.elasticsearch.river.confluence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.river.*;
import org.elasticsearch.threadpool.ThreadPool;
import org.swift.common.soap.confluence.*;

import javax.xml.rpc.ServiceException;
import java.awt.print.Pageable;
import java.rmi.RemoteException;
import java.util.Map;

import static org.elasticsearch.client.Requests.indexRequest;

/**
 * @author @obazoud
 */
public class ConfluenceRiver extends AbstractRiverComponent implements River {
  private Client client;
  private ThreadPool threadPool;

  private String riverIndexName;
  private String indexName = null;
  private String typeName = null;

  private String url;
  private String username;
  private String password;
  private int updateRate;
  private String spaceKey;

  private String endPoint = "/rpc/soap-axis/confluenceservice-v2";

  private volatile boolean closed;
  private volatile Thread indexerThread;

  @Inject
  protected ConfluenceRiver(RiverName riverName, RiverSettings settings, @RiverIndexName String riverIndexName, Client client, ThreadPool threadPool) {
    super(riverName, settings);
    logger.info("Creating confluence river");
    this.client = client;
    this.threadPool = threadPool;
    this.riverIndexName = riverIndexName;
    if (settings.settings().containsKey("confluence")) {
      Map<String, Object> confluenceSettings = (Map<String, Object>) settings.settings().get("confluence");

      url = XContentMapValues.nodeStringValue(confluenceSettings.get("url"), null);
      username = XContentMapValues.nodeStringValue(confluenceSettings.get("username"), null);
      password = XContentMapValues.nodeStringValue(confluenceSettings.get("password"), null);
      updateRate = XContentMapValues.nodeIntegerValue(confluenceSettings.get("update_rate"), 15 * 60 * 1000);
      // spaces = (List<String>) XContentMapValues.extractRawValues("spaces", confluenceSettings);
      spaceKey = XContentMapValues.nodeStringValue(confluenceSettings.get("spaceKey"), null);

      indexName = riverName.name();
      typeName = "page";
    }
  }

  @Override
  public void start() {
    logger.info("Starting Confluence River: URL [{}], spaces [{}], updateRate [{}], indexing to [{}]/[{}]",
            url, spaceKey, updateRate, indexName, typeName);
    try {
      client.admin().indices().prepareCreate(indexName).execute().actionGet();
    } catch (Exception e) {
      if (ExceptionsHelper.unwrapCause(e) instanceof IndexAlreadyExistsException) {
        // that's fine
      } else if (ExceptionsHelper.unwrapCause(e) instanceof ClusterBlockException) {
        // ok, not recovered yet..., lets start indexing and hope we recover by the first bulk
      } else {
        logger.warn("failed to create index [{}], disabling river...", e, indexName);
        return;
      }
    }

    indexerThread = EsExecutors.daemonThreadFactory(settings.globalSettings(), "confluence_river_indexer").newThread(new Indexer());
    indexerThread.start();
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    logger.info("Stopping Confluence River");
    indexerThread.interrupt();
    closed = true;
  }

  private class Indexer implements Runnable {

    @Override
    public void run() {
      ObjectMapper mapper = new ObjectMapper();
      while (true) {
        if (closed) {
          return;
        }

        try {
          ConfluenceSoapServiceServiceLocator locator = new ConfluenceSoapServiceServiceLocator();
          locator.setConfluenceserviceV2EndpointAddress(url + endPoint);
          locator.setMaintainSession(true);
          ConfluenceSoapService service = locator.getConfluenceserviceV2();
          String token = service.login(username, password);
          RemoteServerInfo info = service.getServerInfo(token);
          logger.info("Confluence version {}.{}.{} ({})", info.getMajorVersion(), info.getMinorVersion(), info.getPatchLevel(), info.getBuildId());

          BulkRequestBuilder bulk = client.prepareBulk();

          RemoteSpaceSummary spaceSummary = service.getSpace(token, spaceKey);
          logger.info("Confluence space {} {} {}", spaceSummary.getName(), spaceSummary.getKey(), spaceSummary.getType());
          RemotePageSummary[] pageSummaries = service.getPages(token, spaceKey);
          for(RemotePageSummary pageSummary : pageSummaries) {
            RemotePage page = service.getPage(token, pageSummary.getId());
            bulk.add(indexRequest(indexName).type(typeName).id(String.valueOf(page.getId())).source(mapper.writeValueAsString(page)));
          }

          try {
            logger.info("Execute bulk {} actions", bulk.numberOfActions());
            BulkResponse response = bulk.execute().actionGet();
            if (response.hasFailures()) {
              // TODO write to exception queue?
              logger.warn("failed to execute" + response.buildFailureMessage());
            }
          } catch (Exception e) {
            logger.warn("failed to execute bulk", e);
          }
        } catch (Exception e) {
          logger.warn("Confluence river exception", e);
        }

        try {
          logger.debug("Confluence river is going to sleep for {} ms", updateRate);
          Thread.sleep(updateRate);
        } catch (InterruptedException e) {
        }

      }

    }
  }
}
