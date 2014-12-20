package org.openhab.io.habmin.services.events;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.BroadcasterLifeCyclePolicyListener;
import org.atmosphere.jersey.JerseyBroadcaster;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.StateChangeListener;
import org.openhab.core.types.State;
import org.openhab.io.habmin.HABminApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventBroadcaster extends JerseyBroadcaster  {
	private static final Logger logger = LoggerFactory.getLogger(EventBroadcaster.class);

	public EventBroadcaster(String id, org.atmosphere.cpr.AtmosphereConfig config) {
		super(id, config);
/*
		this.addBroadcasterLifeCyclePolicyListener(new BroadcasterLifeCyclePolicyListener() {
			
			@Override
			public void onIdle() {
				logger.debug("broadcaster '{}' is idle", this.toString());
			}
			
			@Override
			public void onEmpty() {
				logger.debug("broadcaster '{}' is empty", this.toString());
			}
			
			@Override
			public void onDestroy() {
				logger.debug("broadcaster '{}' destroyed", this.toString());
//				for (ResourceStateChangeListener l : listeners){
//					l.unregisterItems();
//					listeners.remove(l);
//				}
			}
		});*/
	}

	public void register() {
		// register us on all items which are already available in the
		// registry
		for (Item item : HABminApplication.getItemUIRegistry().getItems()) {
			if (item instanceof GenericItem) {
				GenericItem genericItem = (GenericItem) item;
//				genericItem.addStateChangeListener(this);
			}
		}
	}
/*
	@Override
	public void stateChanged(Item item, State oldState, State newState) {

	}

	@Override
	public void stateUpdated(Item item, State state) {

	}*/
}