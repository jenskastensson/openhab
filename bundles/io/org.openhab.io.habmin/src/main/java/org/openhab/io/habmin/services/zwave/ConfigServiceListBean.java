package org.openhab.io.habmin.services.zwave;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.openhab.binding.zwave.internal.config.OpenHABConfigurationRecord;

@XmlRootElement(name="record")
public class ConfigServiceListBean {
	public List<OpenHABConfigurationRecord> records;
}
