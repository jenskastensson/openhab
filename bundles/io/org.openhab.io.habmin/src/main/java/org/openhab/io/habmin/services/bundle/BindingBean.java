/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.bundle;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is a java bean that is used with JAXB to serialize items
 * to XML or JSON.
 *
 * @author Chris Jackson
 * @since 1.3.0
 *
 */
@XmlRootElement(name="binding")
public class BindingBean {

	public String bundle;
	public String name;
	public String pid;
	public String type;
	public String author;
	public String version;
	public String oh_version;
	public String link;
	public String osgiVersion;

	public List<BindingConfigBean> generalconfig = new ArrayList<BindingConfigBean>();
	public List<BindingConfigBean> interfaceconfig = new ArrayList<BindingConfigBean>();
	public List<BindingConfigBean> itemconfig = new ArrayList<BindingConfigBean>();

	public BindingBean() {}
}
