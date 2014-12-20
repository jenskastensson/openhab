/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.sitemap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * This is a java bean that is used with JAXB to serialize widgets
 * to XML or JSON.
 *  
 * @author Chris Jackson
 * @since 1.3.0
 *
 */
@XmlRootElement(name="widget")
public class WidgetConfigBean {

	public String widgetId;
	public String type;
	public String name;
	
	public String label;
	public String format;
	public String units;
	public String translateService;
	public String translateRule;
	
	public String icon;

	// widget-specific attributes
	@XmlElement(name="mapping")
	public List<MappingBean> mapping = new ArrayList<MappingBean>();
	public Boolean switchSupport;
	public Integer sendFrequency;
	public String separator;
	public Integer refresh;
	public Integer height;
	public BigDecimal minValue;
	public BigDecimal maxValue;
	public BigDecimal step;
	public String url;
	public String service;
	public String period;

	public String encoding;
	
	public String item;
	
	public List<VisibilityBean> visibility;
	public List<ColorBean> labelcolor;
	public List<ColorBean> valuecolor;

	// only for frames, other linkable widgets link to a page
	@XmlElement(name="widget")
	public final List<WidgetConfigBean> widgets = new ArrayList<WidgetConfigBean>();
	
	public WidgetConfigBean() {}
}