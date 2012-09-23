package org.elasticsearch.plugin.river.confluence;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.river.RiverName;
import org.elasticsearch.river.RiverSettings;
import org.elasticsearch.river.RiversModule;
import org.elasticsearch.river.confluence.ConfluenceRiverModule;

/**
 * @author @obazoud
 */
public class ConfluenceRiverPlugin extends AbstractPlugin {
  @Inject
  public ConfluenceRiverPlugin() {
    super();
  }

  @Override
  public void processModule(Module module) {
    if (module instanceof RiversModule) {
      ((RiversModule) module).registerRiver("confluence", ConfluenceRiverModule.class);
    }
  }

  @Override
  public String name() {
    return "confluence-river";
  }

  @Override
  public String description() {
    return "Confluence River Plugin";
  }


}
