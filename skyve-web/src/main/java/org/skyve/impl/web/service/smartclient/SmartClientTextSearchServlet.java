package org.skyve.impl.web.service.smartclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.skyve.EXT;
import org.skyve.content.ContentManager;
import org.skyve.content.MimeType;
import org.skyve.content.SearchResult;
import org.skyve.content.SearchResults;
import org.skyve.domain.Bean;
import org.skyve.domain.PersistentBean;
import org.skyve.impl.persistence.AbstractPersistence;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.user.User;
import org.skyve.util.JSON;
import org.skyve.util.Util;
import org.skyve.web.WebContext;

public class SmartClientTextSearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException {
		Enumeration<String> parameterNames = request.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			String name = parameterNames.nextElement();
			String value = request.getParameter(name);
			UtilImpl.LOGGER.info(name + " = " + value);
		}

		String criteria = request.getParameter("query");
		
		AbstractPersistence persistence = null;
		try {
			try (ContentManager cm = EXT.newContentManager()) {
				User user = (User) request.getSession().getAttribute(WebContext.USER_SESSION_ATTRIBUTE_NAME);
				persistence = AbstractPersistence.get();
				persistence.setUser(user);
				Customer customer = user.getCustomer();

				SearchResults results = cm.google(criteria, 100);

	            response.setContentType(MimeType.json.toString());
	            response.setCharacterEncoding(Util.UTF8);
	            response.addHeader("Cache-control", "private,no-cache,no-store"); // never
	            response.addDateHeader("Expires", 0); // never

	            try (PrintWriter pw = response.getWriter()) {
		            StringBuilder message = new StringBuilder(512);
			    	message.append(SmartClientListServlet.ISC_JSON_PREFIX);
		            message.append("{response:{data:[");
		
		            Iterator<SearchResult> resultIterator = results.getResults().iterator();
		            StringBuilder url = new StringBuilder(128);
		            StringBuilder iconMarkup = new StringBuilder(64);
		            while (resultIterator.hasNext()) {
						SearchResult result = resultIterator.next();
						try {
							String moduleName = result.getModuleName();
							String documentName = result.getDocumentName();
							String bizId = result.getBizId();
							
							Module module = customer.getModule(moduleName);
							Document document = module.getDocument(customer, documentName);
							PersistentBean bean = persistence.retrieve(document, bizId, false);
		
							// Use JSONUtil here to ensure that everything is escaped properly
							
				            Map<String, Object> row = new TreeMap<>();
				            String icon16 = document.getIcon16x16RelativeFileName();
				            String icon = document.getIconStyleClass();
				            iconMarkup.setLength(0);
				            if (icon != null) {
				            	iconMarkup.append("<i class=\"bizhubFontIcon ").append(icon).append("\"></i>");
				            }
				            else if (icon16 != null) {
					            iconMarkup.append("<img style=\"width:16px;height:16px\" src=\"resources?_doc=");
					            iconMarkup.append(moduleName).append('.').append(documentName);
					            iconMarkup.append("&_n=").append(icon16).append("\"/>");
				            }
				            row.put("icon", iconMarkup.toString());
				            row.put("doc", document.getSingularAlias());
				            row.put(Bean.BIZ_KEY, (bean != null) ? bean.getBizKey() : null);
				            row.put("excerpt", result.getExcerpt());
				            row.put("score", new Integer(result.getScore()));
		                    url.setLength(0);
		                    url.append("?m=");
		                    url.append(moduleName).append("&d=").append(documentName);
		                    url.append("&i=").append(bizId);
				            row.put("data", url.toString());
		
				            String attributeName = result.getAttributeName();
				            if (attributeName == null) { // bean content
				            	row.put("content", null);
				            }
				            else { // attachment content
					            url.setLength(0);
			                    url.append("content?_doc=");
			                    url.append(moduleName).append('.').append(documentName);
			                    url.append("&_n=").append(result.getContentId());
			                    url.append("&_b=").append(attributeName);
					            row.put("content", url.toString());
				            }
				            message.append(JSON.marshall(customer, row, null)).append(',');
						}
						catch (Exception e) { // don't allow anything that goes wrong to stop us returning the searches
							e.printStackTrace();
							resultIterator.remove(); // remove the offending result
						}
					}
	
					// append summary row
					message.append("{time:").append(results.getSearchTimeInSecs()).append(",suggestion:");
					String suggestion = results.getSuggestion();
					if (suggestion != null) {
						message.append('\'').append(suggestion).append('\'');
					}
					else {
						message.append("null");
					}
				
					message.append("}],status:0,");
					message.append("startRow:0,endRow:");
					// rows could have been removed above if the bizkey couldn't be found
					message.append(results.getResults().size());
					message.append(",totalRows:");
					message.append(results.getResults().size());
					message.append("}}");
			    	message.append(SmartClientListServlet.ISC_JSON_SUFFIX);
	
					pw.append(message);
					pw.flush();
	            }
            }
		}
		catch (Exception e) {
			throw new ServletException("Could not search the content repository", e);
		}
		finally {
			persistence.commit(true);
		}
    }
}