package org.skyve.wildcat.metadata.view.container.form;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.skyve.metadata.MetaData;
import org.skyve.metadata.view.Disableable;
import org.skyve.metadata.view.Invisible;
import org.skyve.wildcat.metadata.view.Bordered;
import org.skyve.wildcat.metadata.view.HorizontalAlignment;
import org.skyve.wildcat.metadata.view.Identifiable;
import org.skyve.wildcat.metadata.view.RelativeSize;
import org.skyve.wildcat.util.UtilImpl;
import org.skyve.wildcat.util.XMLUtil;

@XmlRootElement(namespace = XMLUtil.VIEW_NAMESPACE)
@XmlType(namespace = XMLUtil.VIEW_NAMESPACE,
			propOrder = {"widgetId",
							"pixelWidth", 
							"percentageWidth", 
							"pixelHeight", 
							"percentageHeight", 
							"border",
							"borderTitle",
							"labelDefaultHorizontalAlignment",
							"disabledConditionName",
							"invisibleConditionName", 
							"columns", 
							"rows"})
public final class Form implements MetaData, Identifiable, RelativeSize, Disableable, Invisible, Bordered {
	/**
	 * For Serialization
	 */
	private static final long serialVersionUID = 8677483284773272582L;

	private String widgetId;
	
	private Boolean border;
	private String borderTitle;
	
	private Integer pixelWidth;
	private Integer percentageWidth;
	private Integer pixelHeight;
	private Integer percentageHeight;
	
	private HorizontalAlignment labelDefaultHorizontalAlignment;
	
	private String disabledConditionName;
	private String invisibleConditionName;
	
	private List<FormColumn> columns = new ArrayList<>();
	private List<FormRow> rows = new ArrayList<>();

	@XmlElement(namespace = XMLUtil.VIEW_NAMESPACE, name = "column", required = true)
	public List<FormColumn> getColumns() {
		return columns;
	}

	@XmlElement(namespace = XMLUtil.VIEW_NAMESPACE, name = "row", required = true)
	public List<FormRow> getRows() {
		return rows;
	}

	@Override
	public String getWidgetId() {
		return widgetId;
	}

	@XmlAttribute(required = false)
	public void setWidgetId(String widgetId) {
		this.widgetId = UtilImpl.processStringValue(widgetId);
	}

	@Override
	public Boolean getBorder() {
		return border;
	}

	@Override
	@XmlAttribute(required = false)
	public void setBorder(Boolean border) {
		this.border = border;
	}

	@Override
	public String getBorderTitle() {
		return borderTitle;
	}

	@Override
	@XmlAttribute(required = false)
	public void setBorderTitle(String borderTitle) {
		this.borderTitle = borderTitle;
	}

	@Override
	public Integer getPercentageHeight() {
		return percentageHeight;
	}

	@Override
	@XmlAttribute(required = false)
	public void setPercentageHeight(Integer percentageHeight) {
		this.percentageHeight = percentageHeight;
	}

	@Override
	public Integer getPercentageWidth() {
		return percentageWidth;
	}

	@Override
	@XmlAttribute(required = false)
	public void setPercentageWidth(Integer percentageWidth) {
		this.percentageWidth = percentageWidth;
	}

	@Override
	public Integer getPixelHeight() {
		return pixelHeight;
	}

	@Override
	@XmlAttribute(required = false)
	public void setPixelHeight(Integer pixelHeight) {
		this.pixelHeight = pixelHeight;
	}

	@Override
	public Integer getPixelWidth() {
		return pixelWidth;
	}

	@Override
	@XmlAttribute(required = false)
	public void setPixelWidth(Integer pixelWidth) {
		this.pixelWidth = pixelWidth;
	}

	public HorizontalAlignment getLabelDefaultHorizontalAlignment() {
		return labelDefaultHorizontalAlignment;
	}

	@XmlAttribute(name = "defaultLabelAlign", required = false)
	public void setLabelDefaultHorizontalAlignment(HorizontalAlignment labelDefaultHorizontalAlignment) {
		this.labelDefaultHorizontalAlignment = labelDefaultHorizontalAlignment;
	}

	@Override
	public String getDisabledConditionName() {
		return disabledConditionName;
	}

	@Override
	@XmlAttribute(name = "disabled", required = false)
	public void setDisabledConditionName(String disabledConditionName) {
		this.disabledConditionName = disabledConditionName;
	}

	@Override
	public String getInvisibleConditionName() {
		return invisibleConditionName;
	}

	@Override
	@XmlAttribute(name = "invisible", required = false)
	public void setInvisibleConditionName(String invisibleConditionName) {
		this.invisibleConditionName = invisibleConditionName;
	}
}