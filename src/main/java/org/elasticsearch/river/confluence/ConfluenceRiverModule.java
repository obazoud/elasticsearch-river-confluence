package org.elasticsearch.river.confluence;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Binder;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.river.River;

/**
 * @author @obazoud
 */
public class ConfluenceRiverModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(River.class).to(ConfluenceRiver.class).asEagerSingleton();
  }
}
