package org.skyve.metadata.model.document;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.skyve.impl.util.XMLMetaData;

public interface Inverse extends Relation {
	@XmlTransient
	public static enum InverseRelationship {
		oneToOne,
		oneToMany,
		manyToMany
	}
	
	@XmlType(namespace = XMLMetaData.DOCUMENT_NAMESPACE)
	public static enum InverseCardinality {
		one, many;
	}

	public String getReferenceName();
	public InverseCardinality getCardinality();
}
