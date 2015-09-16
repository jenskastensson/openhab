/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.proserv;

import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;




/**
 * Represents all valid commands which could be processed by this binding
 * 
 * @author JEKA
 * @since 1.0.0
*/
public enum ProservCommandType {

	TYPE_PROSERV_BACKUP_RESET_RRD {
		{
			command = "proserv_backup_reset_rrd";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_BACKUP_RRD {
		{
			command = "proserv_backup_rrd";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_SEND_RRD_BACKUP {
		{
			command = "proserv_send_rrd_backup";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_EXPORT_CSV_FILES {
		{
			command = "proserv_export_csv_files";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_SEND_CSV_FILES {
		{
			command = "proserv_send_csv_files";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_LANGUAGE {
		{
			command = "proserv_language";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_EMAIL {
		{
			command = "proserv_email";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_IP {
		{
			command = "proserv_ip";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_CRONJOB {
		{
			command = "proserv_cronjobs";
			itemClass = StringItem.class;
		}
	},
	TYPE_PROSERV_TIMER {
		{
			command = "proserv_timer";
			itemClass = SwitchItem.class;
		}
	},
	TYPE_PROSERV_RESTART {
		{
			command = "proserv_restart";
			itemClass = StringItem.class;
		}
	},	
	TYPE_PROSERV_TEST {
		{
			command = "proserv_test";
			itemClass = StringItem.class;
		}
	};

	
	/** Represents the proServ command as it will be used in *.items configuration */
	String command;
	Class<? extends Item> itemClass;

	public String getCommand() {
		return command;
	}

	public Class<? extends Item> getItemClass() {
		return itemClass;
	}

	/**
	 * 
	 * @param bindingConfig command string
	 * @param itemClass class to validate
	 * @return true if item class can bound to proServCommand
	 */
	public static boolean validateBinding(ProservCommandType bindingConfig, Class<? extends Item> itemClass) {
		boolean ret = false;
		for (ProservCommandType c : ProservCommandType.values()) {
			if (c.getCommand().equals(bindingConfig.getCommand())
					&& c.getItemClass().equals(itemClass)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	public static ProservCommandType fromString(String proServCommand) {

		if ("".equals(proServCommand)) {
			return null;
		}
		for (ProservCommandType c : ProservCommandType.values()) {

			if (c.getCommand().equals(proServCommand)) {
				return c;
			}
		}

		throw new IllegalArgumentException("cannot find proServCommand for '"
				+ proServCommand + "'");

	}

}
