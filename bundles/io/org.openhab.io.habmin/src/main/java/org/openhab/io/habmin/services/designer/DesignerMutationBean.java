/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin.services.designer;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Chris Jackson
 * @since 1.5.0
 *
 */
@XmlRootElement(name="mutation")
public class DesignerMutationBean {
	public String name;
	public String value;
}
