package org.openhab.io.habmin.services.events;

import java.util.Date;

public class EventBean {
	Date time;
	EventType event;
	String text1;
	String text2;
	
	enum EventType {
		ITEMUPDATE,
		ITEMCHANGE
	}
}
