package org.skyve.util;

import java.io.Serializable;
import java.util.logging.Logger;

import org.skyve.domain.Bean;
import org.skyve.domain.messages.DomainException;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.wildcat.util.UtilImpl;

/**
 *
 */
public class Util {
	/**
	 * 
	 */
	public static final Logger LOGGER = UtilImpl.LOGGER;

	/**
	 * Disallow instantiation
	 */
	private Util() {
		// nothing to see here
	}
	
	/**
	 * 
	 * @param object
	 * @return
	 */
	public static final <T extends Serializable> T cloneBySerialization(T object) {
		return UtilImpl.cloneBySerialization(object);
	}

	/**
	 * 
	 * @param object
	 * @return
	 * @throws Exception
	 */
	public static final <T extends Serializable> T cloneToTransientBySerialization(T object) 
	throws Exception {
		return UtilImpl.cloneToTransientBySerialization(object);
	}

	/**
	 * Recurse the bean ensuring that everything is touched and loaded from the database.
	 * 
	 * @param bean The bean to load.
	 * @throws DomainException
	 * @throws MetaDataException
	 */
	public static void populateFully(Bean bean) 
	throws DomainException, MetaDataException {
		UtilImpl.populateFully(bean);
	}

	/**
	 * Utility method that tries to properly initialise the persistence layer proxies used by lazy loading. 
	 * 
	 * @param <T>
	 * @param possibleProxy	The possible proxy
	 * @return the resolved proxy or possibleProxy
	 */
	public static <T> T deproxy(T possibleProxy) throws ClassCastException {
		return UtilImpl.deproxy(possibleProxy);
	}
	
	/**
	 * Trims and sets "" to null.
	 * @param value
	 * @return
	 */
	public static String processStringValue(String value) {
		return UtilImpl.processStringValue(value);
	}

	/**
	 * 
	 * @param object
	 * @throws Exception
	 */
	public static void setTransient(Object object) throws Exception {
		UtilImpl.setTransient(object);
	}

	/**
	 * 
	 * @param object
	 * @param bizDataGroupId
	 * @throws Exception
	 */
	// set the data group of a bean and all its children
	public static void setDataGroup(Object object, String bizDataGroupId) throws Exception {
		UtilImpl.setDataGroup(object, bizDataGroupId);
	}

	/**
	 * Make an instance of a document bean with random values for its properties.
	 * 
	 * @param <T>	The type of Document bean to produce.
	 * @param user
	 * @param module
	 * @param document	The document (corresponds to type T)
	 * @param depth	How far to traverse the object graph - through associations and collections.
	 * 				There are relationships that are never ending - ie Contact has Interactions which has User which has COntact
	 * @return	The randomly constructed bean.
	 * @throws Exception
	 */
	public static <T extends Bean> T constructRandomInstance(User user, 
																Module module,
																Document document,
																int depth)
	throws Exception {
		return UtilImpl.constructRandomInstance(user, module, document, depth);
	}
	
	public static String getServerUrl() {
		return UtilImpl.SERVER_URL;
	}
	
	public static String getWildcatContext() {
		return UtilImpl.WILDCAT_CONTEXT;
	}
	
	public static String getHomeUri() {
		return UtilImpl.HOME_URI;
	}
	
	public static String getWildcatContextUrl() {
		return UtilImpl.SERVER_URL + UtilImpl.WILDCAT_CONTEXT;
	}
	
	public static String getHomeUrl() {
		StringBuilder result = new StringBuilder(128);
		result.append(UtilImpl.SERVER_URL).append(UtilImpl.WILDCAT_CONTEXT).append(UtilImpl.HOME_URI);
		return result.toString();
	}
	
	public static String getDocumentUrl(String bizModule, String bizDocument) {
		return getDocumentUrl(bizModule, bizDocument, null);
	}

	public static String getDocumentUrl(String bizModule, String bizDocument, String bizId) {
		StringBuilder result = new StringBuilder(128);

		result.append(UtilImpl.SERVER_URL).append(UtilImpl.WILDCAT_CONTEXT).append(UtilImpl.HOME_URI);
		result.append("?a=e&m=").append(bizModule).append("&d=").append(bizDocument);
		if (bizId != null) {
			result.append("&i=").append(bizId);
		}

		return result.toString();
	}
	
	public static String getDocumentUrl(Bean bean) {
		return getDocumentUrl(bean.getBizModule(), bean.getBizDocument(), bean.getBizId());
	}
	
	public static String getGridUrl(String bizModule, String queryName) {
		StringBuilder result = new StringBuilder(128);

		result.append(UtilImpl.SERVER_URL).append(UtilImpl.WILDCAT_CONTEXT).append(UtilImpl.HOME_URI);
		result.append("?a=g&m=").append(bizModule).append("&q=").append(queryName);

		return result.toString();
	}
}