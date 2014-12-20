/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.rule;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * 
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
@XmlRootElement(name="rule")
public class RuleTemplateBean {
	@XmlElement(name="item")
	public String item;
	
	@XmlElement(name="name")
	public String name;
	
	@XmlElement(name="label")
	public String label;
	
	@XmlElement(name="type")
	public String type;

	@XmlElement(name="singleton")
	public boolean singleton;

	@XmlElement(name="description")
	public String description;

	@XmlElement(name="applicabletype")
	@XStreamImplicit(itemFieldName="applicabletype")
	public List<String> applicableType;

	@XmlElement(name="itemtype")
	@XStreamImplicit(itemFieldName="itemtype")
	public List<String> itemType;

	@XmlElement(name="variable")
	@XStreamImplicit(itemFieldName="variable")
	public List<RuleVariableBean> variable;

	@XmlElement(name="import")
	@XStreamImplicit(itemFieldName="import")
	public List<String> imports;

	@XmlElement(name="trigger")
	@XStreamImplicit(itemFieldName="trigger")
	public List<String> trigger;
	
	@XmlElement(name="action")
	@XStreamImplicit(itemFieldName="action")
	public List<String> action;

	@XmlElement(name="linkeditem")
	public String linkeditem;

	public RuleTemplateBean() {}
}

