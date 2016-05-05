package org.skyve.impl.content;

import org.skyve.impl.content.AttachmentContent;
import org.skyve.impl.content.BeanContent;
import org.skyve.impl.content.ContentIterable;
import org.skyve.impl.content.SearchResults;

public interface ContentManager extends AutoCloseable {
	public void put(BeanContent content) throws Exception;
	public void put(AttachmentContent content) throws Exception;
	public void put(AttachmentContent content, boolean index) throws Exception;
	public AttachmentContent get(String id) throws Exception;
	public void remove(BeanContent content) throws Exception;
	public void remove(String contentId) throws Exception;
	public SearchResults google(String search, int maxResults) throws Exception;
	public void truncate(String customerName) throws Exception;
	public ContentIterable all() throws Exception;
}