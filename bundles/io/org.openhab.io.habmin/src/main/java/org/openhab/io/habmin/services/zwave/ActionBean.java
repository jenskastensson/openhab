package org.openhab.io.habmin.services.zwave;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="action")
public class ActionBean {
	public String domain;
	public String action;
}
