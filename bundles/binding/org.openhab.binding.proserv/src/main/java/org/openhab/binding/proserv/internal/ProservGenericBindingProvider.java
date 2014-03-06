/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.proserv.internal;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.openhab.binding.proserv.ProservBindingProvider;
import org.openhab.binding.proserv.ProservCommandType;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class can parse information from the generic binding format and provides
 * proServ binding information from it. It registers as a
 * {@link ProservBindingProvider} service as well.
 * </p>
 * 
 * @author JEKA
 * 
 * @since 1.0.0
 */
public class ProservGenericBindingProvider extends
		AbstractGenericBindingProvider implements ProservBindingProvider {

	static final Logger logger = LoggerFactory
			.getLogger(ProservGenericBindingProvider.class);

	public String getBindingType() {
		return "proserv";
	}

	/**
	 * @{inheritDoc
	 */
	public void validateItemType(Item item, String bindingConfig)
			throws BindingConfigParseException {
		if (!(item instanceof NumberItem || item instanceof StringItem)) {
			throw new BindingConfigParseException(
					"item '"
							+ item.getName()
							+ "' is of type '"
							+ item.getClass().getSimpleName()
							+ "', only Number- and StringItems are allowed - please check your *.items configuration");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item,
			String proServCommand) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, proServCommand);
		if (proServCommand != null) {
			ProservBindingConfig config = parseBindingConfig(item,
					ProservCommandType.fromString(proServCommand));
			addBindingConfig(item, config);
		} else {
			logger.warn("bindingConfig is NULL (item=" + item
					+ ") -> processing bindingConfig aborted!");
		}
	}

	/**
	 * Checks if the bindingConfig contains a valid binding type and returns an
	 * appropriate instance.
	 * 
	 * @param item
	 * @param bindingConfig
	 * 
	 * @throws BindingConfigParseException
	 *             if bindingConfig is no valid binding type
	 */
	protected ProservBindingConfig parseBindingConfig(Item item,
			ProservCommandType bindingConfig)
			throws BindingConfigParseException {
		if (ProservCommandType.validateBinding(bindingConfig, item.getClass())) {
			return new ProservBindingConfig(bindingConfig);
		} else {
			throw new BindingConfigParseException("'" + bindingConfig
					+ "' is no valid binding type");
		}
	}

	public String[] getItemNamesForType(ProservCommandType eventType) {
		Set<String> itemNames = new HashSet<String>();
		for (Entry<String, BindingConfig> entry : bindingConfigs.entrySet()) {
			ProservBindingConfig proServConfig = (ProservBindingConfig) entry
					.getValue();
			if (proServConfig.getType().equals(eventType)) {
				itemNames.add(entry.getKey());
			}
		}
		return itemNames.toArray(new String[itemNames.size()]);
	}

	static class ProservBindingConfig implements BindingConfig {

		final private ProservCommandType type;

		public ProservBindingConfig(ProservCommandType type) {
			this.type = type;
		}

		public ProservCommandType getType() {
			return type;
		}
	}

}
