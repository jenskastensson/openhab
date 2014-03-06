/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.proserv;

import org.openhab.core.binding.BindingProvider;


/**
 * This interface is implemented by classes that can map openHAB items to
 * proServ binding types.
 * 
 * Implementing classes should register themselves as a service in order to be 
 * taken into account.
 * 
 * @author JEKA
 * @since 1.0.0
 */
public interface ProservBindingProvider extends BindingProvider {

	/**
	 * Provides an array of all item names of this provider for a given binding type
	 * @param bindingType the binding type of the items
	 * @return an array of all item names of this provider for the given binding type
	 */
	public String[] getItemNamesForType(ProservCommandType bindingType);
}
