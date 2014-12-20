/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.habmin;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.ws.rs.core.Application;
import org.atmosphere.cpr.AtmosphereServlet;

import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.io.habmin.services.bundle.BindingConfigResource;
import org.openhab.io.habmin.services.chart.PersistenceResource;
import org.openhab.io.habmin.services.designer.DesignerResource;
import org.openhab.io.habmin.services.events.EventResource;
import org.openhab.io.habmin.services.icon.ItemIconResource;
import org.openhab.io.habmin.services.item.ItemConfigResource;
import org.openhab.io.habmin.services.osgi.BundleResource;
import org.openhab.io.habmin.services.persistence.PersistenceConfigResource;
import org.openhab.io.habmin.services.rule.RuleConfigResource;
import org.openhab.io.habmin.services.sitemap.SitemapConfigResource;
import org.openhab.io.habmin.services.status.StatusResource;
import org.openhab.io.habmin.services.zwave.ZWaveConfigResource;
import org.openhab.io.net.http.SecureHttpContext;
import org.openhab.io.servicediscovery.DiscoveryService;
import org.openhab.io.servicediscovery.ServiceDescription;
import org.openhab.model.core.ModelRepository;
import org.openhab.ui.items.ItemUIRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.binding.zwave.internal.config.OpenHABConfigurationService;

import com.sun.jersey.core.util.FeaturesAndProperties;

/**
 * This is the main component of the REST API; it gets all required services injected,
 * registers itself as a servlet on the HTTP service and adds the different rest resources
 * to this service.
 * 
 * @author Chris Jackson
 * @since 1.4.0
 */
public class HABminApplication extends Application  {

	public static final String HABMIN_DATA_DIR = "etc/habmin/";
	public static final String HABMIN_SERVLET_ALIAS = "/services/habmin";

	private static final Logger logger = LoggerFactory.getLogger(HABminApplication.class);
	
	private int httpSSLPort;

	private int httpPort;

	private HttpService httpService;

	private DiscoveryService discoveryService;

	static private EventPublisher eventPublisher;
	
	static private ItemUIRegistry itemUIRegistry;

	static private ModelRepository modelRepository;
	
	static private Map<String, PersistenceService> persistenceServices = new HashMap<String, PersistenceService>();
	static private Map<String, OpenHABConfigurationService> configurationServices = new HashMap<String, OpenHABConfigurationService>();

	public void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}
	
	public void unsetHttpService(HttpService httpService) {
		this.httpService = null;
	}

	public void setEventPublisher(EventPublisher eventPublisher) {
		HABminApplication.eventPublisher = eventPublisher;
	}
	
	public void unsetEventPublisher(EventPublisher eventPublisher) {
		HABminApplication.eventPublisher = null;
	}

	static public EventPublisher getEventPublisher() {
		return eventPublisher;
	}

	public void setItemUIRegistry(ItemUIRegistry itemUIRegistry) {
		HABminApplication.itemUIRegistry = itemUIRegistry;
	}
	
	public void unsetItemUIRegistry(ItemRegistry itemUIRegistry) {
		HABminApplication.itemUIRegistry = null;
	}

	static public ItemUIRegistry getItemUIRegistry() {
		return itemUIRegistry;
	}

	public void setModelRepository(ModelRepository modelRepository) {
		HABminApplication.modelRepository = modelRepository;
	}
	
	public void unsetModelRepository(ModelRepository modelRepository) {
		HABminApplication.modelRepository = null;
	}

	static public ModelRepository getModelRepository() {
		return modelRepository;
	}

	public void setDiscoveryService(DiscoveryService discoveryService) {
		this.discoveryService = discoveryService;
	}
	
	public void unsetDiscoveryService(DiscoveryService discoveryService) {
		this.discoveryService = null;
	}


	public void addPersistenceService(PersistenceService service) {
		persistenceServices.put(service.getName(), service);
	}

	public void removePersistenceService(PersistenceService service) {
		persistenceServices.remove(service.getName());
	}
	
	static public Map<String, PersistenceService> getPersistenceServices() {
		return persistenceServices;
	}
	
	static void addConfigurationService(OpenHABConfigurationService service) {
		configurationServices.put(service.getCommonName(), service);
	}

	static public Map<String, OpenHABConfigurationService> getConfigurationServices() {
		return configurationServices;
	}

	static void removeConfigurationService(OpenHABConfigurationService service) {
		configurationServices.remove(service.getCommonName());
	}

	public void activate() {			    
        try {
        	// we need to call the activator ourselves as this bundle is included in the lib folder
        	com.sun.jersey.core.osgi.Activator jerseyActivator = new com.sun.jersey.core.osgi.Activator();
        	BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                    .getBundleContext();
        	try {
				jerseyActivator.start(bundleContext);
			} catch (Exception e) {
				logger.error("Could not start Jersey framework", e);
			}
        	
    		httpPort = Integer.parseInt(bundleContext.getProperty("jetty.port"));
    		httpSSLPort = Integer.parseInt(bundleContext.getProperty("jetty.port.ssl"));

			httpService.registerServlet(HABMIN_SERVLET_ALIAS,
			new AtmosphereServlet(), getJerseyServletParams(), createHttpContext());

 			logger.info("Started HABmin REST API at " + HABMIN_SERVLET_ALIAS);

 			if (discoveryService != null) {
 				discoveryService.registerService(getDefaultServiceDescription());
 				discoveryService.registerService(getSSLServiceDescription());
			}
        } catch (ServletException se) {
            throw new RuntimeException(se);
        } catch (NamespaceException se) {
            throw new RuntimeException(se);
        }
	}
	
	public void deactivate() {
        if (this.httpService != null) {
            httpService.unregister(HABMIN_SERVLET_ALIAS);
            logger.info("Stopped HABmin REST API");
        }
        
        if (discoveryService != null) {
 			discoveryService.unregisterService(getDefaultServiceDescription());
			discoveryService.unregisterService(getSSLServiceDescription()); 			
 		}
	}
	
	/**
	 * Creates a {@link SecureHttpContext} which handles the security for this
	 * Servlet  
	 * @return a {@link SecureHttpContext}
	 */
	protected HttpContext createHttpContext() {
		HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
		return new SecureHttpContext(defaultHttpContext, "openHAB.org");
	}
	
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();

        // Register all the resources
        result.add(BindingConfigResource.class);
        result.add(StatusResource.class);
        result.add(ItemConfigResource.class);
        result.add(PersistenceConfigResource.class);
        result.add(SitemapConfigResource.class);
        result.add(ItemIconResource.class);
        result.add(RuleConfigResource.class);
        result.add(DesignerResource.class);

        result.add(EventResource.class);
        result.add(PersistenceResource.class);

        result.add(BundleResource.class);
        result.add(ZWaveConfigResource.class);

        return result;
    }

    private Dictionary<String, String> getJerseyServletParams() {
        Dictionary<String, String> jerseyServletParams = new Hashtable<String, String>();
        jerseyServletParams.put("javax.ws.rs.Application", HABminApplication.class.getName());
        jerseyServletParams.put("org.atmosphere.core.servlet-mapping", HABMIN_SERVLET_ALIAS + "/*");
        jerseyServletParams.put("org.atmosphere.useWebSocket", "true");
        jerseyServletParams.put("org.atmosphere.useNative", "true");
        
        jerseyServletParams.put("org.atmosphere.cpr.AtmosphereInterceptor.disableDefaults", "true");
        
//        jerseyServletParams.put("org.atmosphere.cpr.padding", "whitespace");     
        
        //jerseyServletParams.put("org.atmosphere.cpr.broadcastFilterClasses", "org.atmosphere.client.FormParamFilter");
        jerseyServletParams.put("org.atmosphere.cpr.broadcasterLifeCyclePolicy", "IDLE_DESTROY");
        jerseyServletParams.put("org.atmosphere.cpr.CometSupport.maxInactiveActivity", "300000");
        
        jerseyServletParams.put("com.sun.jersey.spi.container.ResourceFilter", "org.atmosphere.core.AtmosphereFilter");
        //jerseyServletParams.put("org.atmosphere.cpr.broadcasterCacheClass", "org.atmosphere.cache.SessionBroadcasterCache");
        
        // use the default interceptors without PaddingAtmosphereInterceptor
        // see: https://groups.google.com/forum/#!topic/openhab/Z-DVBXdNiYE
        jerseyServletParams.put("org.atmosphere.cpr.AtmosphereInterceptor", "org.atmosphere.interceptor.DefaultHeadersInterceptor,org.atmosphere.interceptor.AndroidAtmosphereInterceptor,org.atmosphere.interceptor.SSEAtmosphereInterceptor,org.atmosphere.interceptor.JSONPAtmosphereInterceptor,org.atmosphere.interceptor.JavaScriptProtocol,org.atmosphere.interceptor.OnDisconnectInterceptor");

        // required because of bug http://java.net/jira/browse/JERSEY-361
        jerseyServletParams.put(FeaturesAndProperties.FEATURE_XMLROOTELEMENT_PROCESSING, "true");

        return jerseyServletParams;
    }
    
    private ServiceDescription getDefaultServiceDescription() {
		Hashtable<String, String> serviceProperties = new Hashtable<String, String>();
		serviceProperties.put("uri", HABMIN_SERVLET_ALIAS);
		return new ServiceDescription("_openhab-server._tcp.local.", "openHAB", httpPort, serviceProperties);
    }

    private ServiceDescription getSSLServiceDescription() {
    	ServiceDescription description = getDefaultServiceDescription();
    	description.serviceType = "_openhab-server-ssl._tcp.local.";
    	description.serviceName = "openHAB-ssl";
		description.servicePort = httpSSLPort;
		return description;
    }

}
