package org.skyve.impl.metadata.view.widget.bound.input;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.skyve.impl.metadata.view.event.EventAction;
import org.skyve.impl.metadata.view.event.Focusable;
import org.skyve.impl.metadata.view.event.RerenderEventAction;
import org.skyve.impl.metadata.view.event.ServerSideActionEventAction;
import org.skyve.impl.metadata.view.event.SetDisabledEventAction;
import org.skyve.impl.metadata.view.event.SetInvisibleEventAction;
import org.skyve.impl.metadata.view.event.ToggleDisabledEventAction;
import org.skyve.impl.util.XMLMetaData;
import org.skyve.impl.metadata.view.widget.bound.input.InputWidget;

@XmlType(namespace = XMLMetaData.VIEW_NAMESPACE,
			propOrder = {"focusActions", "blurActions"})
public abstract class FocusableInputWidget extends InputWidget implements Focusable {
	/**
	 * For Serialization
	 */
	private static final long serialVersionUID = -5764377627372873764L;

	private List<EventAction> focusActions = new ArrayList<>();
	private List<EventAction> blurActions = new ArrayList<>();
	
	@Override
	@XmlElementWrapper(namespace = XMLMetaData.VIEW_NAMESPACE, name = "onFocusHandlers")
	@XmlElementRefs({@XmlElementRef(type = RerenderEventAction.class), 
						@XmlElementRef(type = ServerSideActionEventAction.class),
						@XmlElementRef(type = SetDisabledEventAction.class),
						@XmlElementRef(type = SetInvisibleEventAction.class),
						@XmlElementRef(type = ToggleDisabledEventAction.class)})
	public List<EventAction> getFocusActions() {
		return focusActions;
	}

	@Override
	@XmlElementWrapper(namespace = XMLMetaData.VIEW_NAMESPACE, name = "onBlurHandlers")
	@XmlElementRefs({@XmlElementRef(type = RerenderEventAction.class), 
						@XmlElementRef(type = ServerSideActionEventAction.class),
						@XmlElementRef(type = SetDisabledEventAction.class),
						@XmlElementRef(type = SetInvisibleEventAction.class),
						@XmlElementRef(type = ToggleDisabledEventAction.class)})
	public List<EventAction> getBlurActions() {
		return blurActions;
	}
}
