/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.sitemap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is a java bean that is used with JAXB to serialize a list of widgets
 * to XML or JSON.
 *  
 * @author Oliver Mazur
 * @author Chris Jackson
 * @since 1.0.0
 *
 */

@XmlRootElement(name="widgets")
public class WidgetConfigListBean {

	public WidgetConfigListBean() {}
	
	public WidgetConfigListBean(Collection<WidgetConfigBean> list) {
		entries.addAll(list);
	}
	
	@XmlElement(name="widget")
	public final List<WidgetConfigBean> entries = new ArrayList<WidgetConfigBean>();
	
}
	