package org.skyve.impl.content.ejb;

import org.skyve.content.AttachmentContent;
import org.skyve.content.BeanContent;
import org.skyve.content.ContentIterable;
import org.skyve.content.SearchResults;
import org.skyve.impl.content.AbstractContentManager;

/**
 * This class is used to talk to another skyve server's EJB content server.
 * Implement the server EJB lookup in obtainServer() something like this...
 * <p/>
 * <code>
 * <pre>
 *	public EJBRemoteContentManagerServer obtainServer() throws Exception {
 *		Properties jndiProps = new Properties();
 *		jndiProps.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
 *		Context context = new InitialContext(jndiProps);
 *		// Lookup the Greeter bean using the ejb: namespace syntax which is explained here https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
 *		return (EJBRemoteContentManagerServer) context.lookup("ejb:skyve/apps//EJBRemoteContentManagerServerBean!org.skyve.impl.content.ejb.EJBRemoteContentManagerServer");
 *	}
 * </pre>
 * </code>
 * <pre>
 *  JSON
 *  		...
 *			// Factory settings
 *			factories: {
 *			...
 *			// Skyve content manager class
 *			contentManagerClass: "modules.MyEJBRemoteContentManagerClientImplementation"},
 *			...
 * </pre>
 * @author mike
 */
public abstract class AbstractEJBRemoteContentManagerClient extends AbstractContentManager {
	@Override
	public void init() throws Exception {
		// nothing to do here
	}

	@Override
	public void close() throws Exception {
		// nothing to do here
	}

	@Override
	public void dispose() throws Exception {
		// nothing to do here
	}

	public abstract EJBRemoteContentManagerServer obtainServer() throws Exception;
	
	@Override
	public void put(BeanContent content) throws Exception {
		EJBRemoteContentManagerServer server = obtainServer();
		server.put(content);
	}

	@Override
	public void put(AttachmentContent content, boolean index) throws Exception {
		EJBRemoteContentManagerServer server = obtainServer();
		content.setContentId(server.put(content, index));
	}

	@Override
	public AttachmentContent get(String id) throws Exception {
		EJBRemoteContentManagerServer server = obtainServer();
		return server.get(id);
	}

	@Override
	public void remove(BeanContent content) throws Exception {
		EJBRemoteContentManagerServer server = obtainServer();
		server.remove(content);
	}

	@Override
	public void remove(String contentId) throws Exception {
		EJBRemoteContentManagerServer server = obtainServer();
		server.remove(contentId);
	}

	@Override
	public SearchResults google(String search, int maxResults) throws Exception {
		EJBRemoteContentManagerServer server = obtainServer();
		return server.google(search, maxResults);
	}

	@Override
	public void truncate(String customerName) throws Exception {
		throw new UnsupportedOperationException("Truncate of a remote content repository is not supported");
	}

	@Override
	public void truncateAttachments(String customerName) throws Exception {
		throw new UnsupportedOperationException("Truncate of a remote content repository is not supported");
	}

	@Override
	public void truncateBeans(String customerName) throws Exception {
		throw new UnsupportedOperationException("Truncate of a remote content repository is not supported");
	}

	@Override
	public ContentIterable all() throws Exception {
		throw new UnsupportedOperationException("Iterating over a remote content repository is not supported");
	}
}
