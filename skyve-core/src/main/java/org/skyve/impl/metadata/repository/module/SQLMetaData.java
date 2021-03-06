package org.skyve.impl.metadata.repository.module;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.skyve.impl.util.UtilImpl;
import org.skyve.impl.util.XMLMetaData;

@XmlRootElement(namespace = XMLMetaData.MODULE_NAMESPACE, name = "sql")
@XmlType(namespace = XMLMetaData.MODULE_NAMESPACE, name = "sql", propOrder = {"query"})
public class SQLMetaData extends QueryMetaData {
	private static final long serialVersionUID = 2092696254537507474L;

	private String query;

	public String getQuery() {
		return query;
	}

	@XmlElement(namespace = XMLMetaData.MODULE_NAMESPACE, required = true)
	public void setQuery(String query) {
		this.query = UtilImpl.processStringValue(query);
	}
}
