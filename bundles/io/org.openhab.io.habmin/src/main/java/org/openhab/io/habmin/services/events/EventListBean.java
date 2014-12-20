/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is a java bean that is used with JAXB to serialize item lists.
 *  
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
@XmlRootElement(name="items")
public class EventListBean {
	public long index;
	
	@XmlAnyElement
	public List<JAXBElement> entries = new ArrayList<JAXBElement>();
	
	public EventListBean() {}
	
	public EventListBean(Collection<EventBean> beans) {
		for (EventBean bean : beans) {
            entries.add(new JAXBElement(null, null, null, bean));
        }
	}
	
}
