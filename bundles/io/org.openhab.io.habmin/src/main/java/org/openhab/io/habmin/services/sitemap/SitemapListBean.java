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
 * This is a java bean that is used with JAXB to serialize sitemap lists to JSONP.
 *  
 * @author Kai Kreuzer
 * @author Chris Jackson
 * @since 0.9.0
 *
 */
@XmlRootElement(name="sitemaps")
public class SitemapListBean {

	public SitemapListBean() {}
	
	public SitemapListBean(Collection<SitemapBean> list) {
		entries.addAll(list);
	}
	
	@XmlElement(name="sitemap")
	public final List<SitemapBean> entries = new ArrayList<SitemapBean>();
	
}
