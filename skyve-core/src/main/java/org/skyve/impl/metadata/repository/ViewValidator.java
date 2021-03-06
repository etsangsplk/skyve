package org.skyve.impl.metadata.repository;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.skyve.CORE;
import org.skyve.domain.Bean;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.module.ModuleImpl;
import org.skyve.impl.metadata.view.ActionImpl;
import org.skyve.impl.metadata.view.Inject;
import org.skyve.impl.metadata.view.ViewImpl;
import org.skyve.impl.metadata.view.ViewVisitor;
import org.skyve.impl.metadata.view.component.Component;
import org.skyve.impl.metadata.view.container.HBox;
import org.skyve.impl.metadata.view.container.Tab;
import org.skyve.impl.metadata.view.container.TabPane;
import org.skyve.impl.metadata.view.container.VBox;
import org.skyve.impl.metadata.view.container.form.Form;
import org.skyve.impl.metadata.view.container.form.FormColumn;
import org.skyve.impl.metadata.view.container.form.FormItem;
import org.skyve.impl.metadata.view.container.form.FormRow;
import org.skyve.impl.metadata.view.event.Addable;
import org.skyve.impl.metadata.view.event.Changeable;
import org.skyve.impl.metadata.view.event.Editable;
import org.skyve.impl.metadata.view.event.EventAction;
import org.skyve.impl.metadata.view.event.EventSource;
import org.skyve.impl.metadata.view.event.Focusable;
import org.skyve.impl.metadata.view.event.Removable;
import org.skyve.impl.metadata.view.event.RerenderEventAction;
import org.skyve.impl.metadata.view.event.Selectable;
import org.skyve.impl.metadata.view.event.ServerSideActionEventAction;
import org.skyve.impl.metadata.view.event.SetDisabledEventAction;
import org.skyve.impl.metadata.view.event.SetInvisibleEventAction;
import org.skyve.impl.metadata.view.event.ToggleDisabledEventAction;
import org.skyve.impl.metadata.view.event.ToggleVisibilityEventAction;
import org.skyve.impl.metadata.view.reference.ActionReference;
import org.skyve.impl.metadata.view.reference.ContentReference;
import org.skyve.impl.metadata.view.reference.DefaultListViewReference;
import org.skyve.impl.metadata.view.reference.EditViewReference;
import org.skyve.impl.metadata.view.reference.ExternalReference;
import org.skyve.impl.metadata.view.reference.ImplicitActionReference;
import org.skyve.impl.metadata.view.reference.QueryListViewReference;
import org.skyve.impl.metadata.view.reference.ReferenceProcessor;
import org.skyve.impl.metadata.view.reference.ReportReference;
import org.skyve.impl.metadata.view.reference.ResourceReference;
import org.skyve.impl.metadata.view.widget.Blurb;
import org.skyve.impl.metadata.view.widget.Button;
import org.skyve.impl.metadata.view.widget.DialogButton;
import org.skyve.impl.metadata.view.widget.DynamicImage;
import org.skyve.impl.metadata.view.widget.GeoLocator;
import org.skyve.impl.metadata.view.widget.Link;
import org.skyve.impl.metadata.view.widget.MapDisplay;
import org.skyve.impl.metadata.view.widget.Spacer;
import org.skyve.impl.metadata.view.widget.StaticImage;
import org.skyve.impl.metadata.view.widget.bound.Label;
import org.skyve.impl.metadata.view.widget.bound.ProgressBar;
import org.skyve.impl.metadata.view.widget.bound.input.CheckBox;
import org.skyve.impl.metadata.view.widget.bound.input.CheckMembership;
import org.skyve.impl.metadata.view.widget.bound.input.ColourPicker;
import org.skyve.impl.metadata.view.widget.bound.input.Combo;
import org.skyve.impl.metadata.view.widget.bound.input.Comparison;
import org.skyve.impl.metadata.view.widget.bound.input.ContentImage;
import org.skyve.impl.metadata.view.widget.bound.input.ContentLink;
import org.skyve.impl.metadata.view.widget.bound.input.Geometry;
import org.skyve.impl.metadata.view.widget.bound.input.HTML;
import org.skyve.impl.metadata.view.widget.bound.input.ListMembership;
import org.skyve.impl.metadata.view.widget.bound.input.Lookup;
import org.skyve.impl.metadata.view.widget.bound.input.LookupDescription;
import org.skyve.impl.metadata.view.widget.bound.input.Password;
import org.skyve.impl.metadata.view.widget.bound.input.Radio;
import org.skyve.impl.metadata.view.widget.bound.input.RichText;
import org.skyve.impl.metadata.view.widget.bound.input.Slider;
import org.skyve.impl.metadata.view.widget.bound.input.Spinner;
import org.skyve.impl.metadata.view.widget.bound.input.TextArea;
import org.skyve.impl.metadata.view.widget.bound.input.TextField;
import org.skyve.impl.metadata.view.widget.bound.tabular.AbstractDataWidget;
import org.skyve.impl.metadata.view.widget.bound.tabular.DataGrid;
import org.skyve.impl.metadata.view.widget.bound.tabular.DataGridBoundColumn;
import org.skyve.impl.metadata.view.widget.bound.tabular.DataGridContainerColumn;
import org.skyve.impl.metadata.view.widget.bound.tabular.DataRepeater;
import org.skyve.impl.metadata.view.widget.bound.tabular.ListGrid;
import org.skyve.impl.metadata.view.widget.bound.tabular.ListRepeater;
import org.skyve.impl.metadata.view.widget.bound.tabular.TreeGrid;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.Attribute.AttributeType;
import org.skyve.metadata.model.Extends;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Reference;
import org.skyve.metadata.model.document.Relation;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.query.MetaDataQueryDefinition;
import org.skyve.metadata.module.query.MetaDataQueryProjectedColumn;
import org.skyve.metadata.module.query.MetaDataQueryColumn;
import org.skyve.metadata.view.View.ViewType;
import org.skyve.metadata.view.widget.bound.Bound;
import org.skyve.metadata.view.widget.bound.FilterParameter;
import org.skyve.metadata.view.widget.bound.Parameter;
import org.skyve.util.Binder;
import org.skyve.util.Binder.TargetMetaData;

// TODO check suggestion attributes on text fields etc
class ViewValidator extends ViewVisitor {
	private String viewIdentifier;
	private String uxui;
	
	// These 2 variables are used when validating the contents of a data grid / data repeater
	private String dataWidgetIdentifier;
	private String dataWidgetBinding;
	
	ViewValidator(ViewImpl view, CustomerImpl customer, DocumentImpl document, String uxui) {
		super(customer, (ModuleImpl) customer.getModule(document.getOwningModuleName()), document, view);
		viewIdentifier = view.getName() + " view for UX/UI " + uxui + " for document " + module.getName() + '.' + document.getName();
		this.uxui = uxui;
		visit();
	}

	private void validateBinding(String bindingPrefix,
									String binding, 
									boolean bindingRequired,
									boolean compoundBindingInvalid, 
									boolean domainValuesRequired,
									boolean scalarBindingOnly,
									String widgetIdentifier,
									AttributeType... assertTypes) {
		if (bindingRequired && (binding == null)) {
			throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " - binding is required.");
		}

		if (binding != null) {
			if (compoundBindingInvalid) {
				if (binding.indexOf('.') >= 0) {
					throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " - Compound binding is not allowed here");
				}
			}
			String bindingToTest = binding;
			if (bindingPrefix != null) {
				bindingToTest = new StringBuilder(64).append(bindingPrefix).append('.').append(binding).toString();
			}
			else {
				// conditions can be used in parameter bindings for reports etc
				String testConditionName = bindingToTest;
				if (testConditionName.startsWith("not")) {
					testConditionName = Character.toLowerCase(testConditionName.charAt(3)) + testConditionName.substring(4);
				}

				if (document.getConditionNames().contains(testConditionName)) {
					return;
				}
			}
			
			TargetMetaData target = null;
			try {
				target = BindUtil.getMetaDataForBinding(customer, module, document, bindingToTest);
			}
			catch (MetaDataException e) {
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " has an invalid binding of " + binding, e);
			}
			
			if (target == null) {
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " - Binding points nowhere");
			}
			Attribute attribute = target.getAttribute();
			if (((assertTypes != null) && (assertTypes.length > 0)) || domainValuesRequired) {
				if (attribute == null) {
					throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
													" - Binding points to an implicit attribute or a condition that cannot have domain values defined.");
				}
			}
			
			if ((assertTypes != null) && (assertTypes.length > 0)) {
				AttributeType type = attribute.getAttributeType();
				boolean typeMatch = false;
				for (AttributeType assertType : assertTypes) {
					if (assertType.equals(type)) {
						typeMatch = true;
						break;
					}
				}
				if (! typeMatch) {
					StringBuilder msg = new StringBuilder(128);
					msg.append(widgetIdentifier).append(" in ").append(viewIdentifier);
					msg.append(" - Binding points to an attribute of type ").append(type).append(", not one of ");
					for (AttributeType assertType : assertTypes) {
						msg.append(assertType).append(", ");
					}
					msg.setLength(msg.length() - 2); // remove last comma
					msg.append('.');
					throw new MetaDataException(msg.toString());
				}
			}
			
			if (domainValuesRequired) {
				if (attribute.getDomainType() == null) {
					throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
													" - Binding points to an attribute that does not have domain values defined.");
				}
			}
			
			// Can only check this if the attribute is defined.
			// Bindings to implicit attributes are always scalar.
			// NB check assert type in outer if coz we dont need to do the test if we are asserting a type
			if (scalarBindingOnly && ((assertTypes == null) || (assertTypes.length == 0)) && (attribute != null)) {
				AttributeType type = attribute.getAttributeType();
				if (AttributeType.association.equals(type) || 
						AttributeType.collection.equals(type) || 
						AttributeType.inverseMany.equals(type) ||
						AttributeType.inverseOne.equals(type)) {
					throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier +
													" - Binding points to an attribute that is not scalar (pointing to an association or collection or inverse)");
				}
			}
		}
	}
	
	private void validateConditionName(String conditionName, String widgetIdentifier) {
		// ignore true and false when checking the condition exists
		if ((conditionName != null) && (! "true".equals(conditionName)) && (! "false".equals(conditionName))) {
			// reverse the sense of the condition if it starts with "not"
			String testConditionName = conditionName;
			if (testConditionName.startsWith("not")) {
				if (! Character.isUpperCase(testConditionName.charAt(3))) {
					throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
													"references condition " + conditionName + " which is not correctly camel cased (eg notTrue)");
				}
				testConditionName = Character.toLowerCase(testConditionName.charAt(3)) + testConditionName.substring(4);
			}

			// ignore implicit conditions when checking the condition exists
			if ((! Bean.PERSISTED_KEY.equals(testConditionName)) && 
					(! Bean.CREATED_KEY.equals(testConditionName))) {
				validateCondition(module, document, testConditionName, widgetIdentifier);
			}
		}
	}
	
	private void validateCondition(ModuleImpl currentModule, 
									DocumentImpl currentDocument,
									String testConditionName,
									String widgetIdentifier) {
		if (! currentDocument.getConditionNames().contains(testConditionName)) {
			Extends extension = currentDocument.getExtends();
			if (extension == null) {
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
												" references condition " + testConditionName + " which does not exist");
			}
			
			DocumentImpl baseDocument = (DocumentImpl) currentModule.getDocument(customer, extension.getDocumentName());
			ModuleImpl baseModule = (ModuleImpl) customer.getModule(baseDocument.getOwningModuleName());
			validateCondition(baseModule, baseDocument, testConditionName, widgetIdentifier);
		}
	}

	private void validateParameterBindings(List<? extends Parameter> parameters, String parentWidgetIdentifier) {
		if (parameters != null) {
			for (Parameter parameter : parameters) {
				validateBinding(null,
									parameter.getBinding(),
									false,
									false,
									false,
									false,
									"Parameter " + parameter.getName() + " in " + parentWidgetIdentifier);
			}
		}
	}
	
	private void validateMessageBindings(String message, 
											String widgetIdentifier,
											String description) {
		if (message != null) {
			Module testModule = module;
			Document testDocument = document;
			if (dataWidgetBinding != null) {
				TargetMetaData target = BindUtil.getMetaDataForBinding(customer, module, document, dataWidgetBinding);
				Attribute targetAttribute = target.getAttribute();
				// Collection and Inverse are appropriate here...
				if (targetAttribute instanceof Relation) {
					Relation relation = (Relation) targetAttribute;
					testDocument = module.getDocument(customer, relation.getDocumentName());
					testModule = customer.getModule(testDocument.getOwningModuleName());
				}
			}
			if (! BindUtil.messageBindingsAreValid(customer, testModule, testDocument, message)) {
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
												" has " + description + " containing malformed binding expressions.");
			}
		}
	}
	
	private void validateQueryOrModel(String queryName, String modelName, String widgetIdentifier) {
		if (queryName != null) {
			if (modelName != null) {
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " has a query and a model name.");
			}
			validateQueryName(queryName, widgetIdentifier);
		}
		else if (modelName != null) {
			validateListModelName(modelName, widgetIdentifier);
		}
		else {
			throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " requires a query name or a model name.");
		}
	}

	private void validateQueryName(String queryName, String widgetIdentifier) {
		if ((queryName != null) && (module.getMetaDataQuery(queryName) == null)) {
			throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " does not reference a valid query of " + queryName);
		}
	}
	
	private void validateListModelName(String modelName, String widgetIdentifier) {
		if (modelName != null) {
			try {
				StringBuilder fullyQualifiedJavaCodeName = new StringBuilder(128);
				fullyQualifiedJavaCodeName.append(document.getOwningModuleName()).append('.').append(document.getName());
				fullyQualifiedJavaCodeName.append(".models.").append(modelName);
				if (AbstractRepository.get().getJavaClass(customer, fullyQualifiedJavaCodeName.toString()) == null) {
					throw new MetaDataException(fullyQualifiedJavaCodeName + " not found.");
				}
			}
			catch (Exception e) { // NB could be class cast problems
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " does not reference a valid list model of " + modelName, e);
			}
		}
	}

	private void validateMapModelName(String modelName, String widgetIdentifier) {
		if (modelName != null) {
			try {
				StringBuilder fullyQualifiedJavaCodeName = new StringBuilder(128);
				fullyQualifiedJavaCodeName.append(document.getOwningModuleName()).append('.').append(document.getName());
				fullyQualifiedJavaCodeName.append(".models.").append(modelName);
				if (AbstractRepository.get().getJavaClass(customer, fullyQualifiedJavaCodeName.toString()) == null) {
					throw new MetaDataException(fullyQualifiedJavaCodeName + " not found.");
				}
			}
			catch (Exception e) { // NB could be class cast problems
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " does not reference a valid map model of " + modelName, e);
			}
		}
	}

	private void validateComparisonModelName(String modelName, String widgetIdentifier) {
		if (modelName != null) {
			try {
				StringBuilder fullyQualifiedJavaCodeName = new StringBuilder(128);
				fullyQualifiedJavaCodeName.append(document.getOwningModuleName()).append('.').append(document.getName());
				fullyQualifiedJavaCodeName.append(".models.").append(modelName);
				if (AbstractRepository.get().getJavaClass(customer, fullyQualifiedJavaCodeName.toString()) == null) {
					throw new MetaDataException(fullyQualifiedJavaCodeName + " not found.");
				}
			}
			catch (Exception e) { // NB could be class cast problems
				throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " does not reference a valid comparison model of " + modelName, e);
			}
		}
	}
	
	private void validateActionName(String actionName, String widgetIdentifier) {
		if ((actionName != null) && (view.getAction(actionName) == null)) {
			throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " references a non-existent action " + actionName);
		}
	}
	
	/**
	 * Override to validate the component instead of resolving and including the edit view structure.
	 */
	@Override
	public void visitComponent(Component component, boolean parentVisible, boolean parentEnabled) {
		String binding = component.getBinding();
		String name = component.getName();
		String componentIdentifier = "Component";
		if (name != null) {
			componentIdentifier += " named " + name;
		}
		if (binding != null) {
			componentIdentifier += " for binding " + binding;
			validateBinding(null, 
								binding, 
								false, 
								false, 
								false, 
								false, 
								componentIdentifier, 
								AttributeType.association, 
								AttributeType.inverseOne);
		}

		try {
			component.setContained(uxui, customer, module, document, view.getName());
		}
		catch (Exception e) {
			throw new MetaDataException(componentIdentifier + " in " + viewIdentifier + " cannot be resolved.", e);
		}
	}
	
	@Override
	public void visitButton(Button button, boolean parentVisible, boolean parentEnabled) {
		String actionName = button.getActionName();
		String buttonIdentifier = "A button " + button.getActionName();
		validateActionName(actionName, buttonIdentifier);
	}

	@Override
	public void visitDynamicImage(DynamicImage image, boolean parentVisible, boolean parentEnabled) {
		String imageIdentifier = "Dynamic Image " + image.getName();
		validateConditionName(image.getInvisibleConditionName(), imageIdentifier);
		validateParameterBindings(image.getParameters(), imageIdentifier);
	}

	@Override
	public void visitCheckBox(CheckBox checkBox, boolean parentVisible, boolean parentEnabled) {
		String binding = checkBox.getBinding();
		String checkBoxIdentifier = "CheckBox " + binding;
		if (dataWidgetBinding != null) {
			checkBoxIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding, 
							binding, 
							true, 
							false, 
							false, 
							true,
							checkBoxIdentifier,
							AttributeType.bool);
		validateConditionName(checkBox.getDisabledConditionName(), checkBoxIdentifier);
		validateConditionName(checkBox.getInvisibleConditionName(), checkBoxIdentifier);
	}

	@Override
	public void visitedCheckBox(CheckBox checkBox,
									boolean parentVisible,
									boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitCheckMembership(CheckMembership membership, boolean parentVisible, boolean parentEnabled) {
		String membershipIdentifier = "CheckBox " + membership.getBinding();
		validateBinding(null,
							membership.getBinding(),
							true,
							false,
							true,
							false,
							membershipIdentifier,
							AttributeType.collection);
		validateConditionName(membership.getDisabledConditionName(), membershipIdentifier);
		validateConditionName(membership.getInvisibleConditionName(), membershipIdentifier);
	}

	@Override
	public void visitedCheckMembership(CheckMembership membership,
										boolean parentVisible,
										boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitColourPicker(ColourPicker colour,
									boolean parentVisible,
									boolean parentEnabled) {
		String binding = colour.getBinding();
		String colourIdentifier = "Colour " + binding;
		if (dataWidgetBinding != null) {
			colourIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							colourIdentifier,
							AttributeType.colour);
		validateConditionName(colour.getDisabledConditionName(), colourIdentifier);
		validateConditionName(colour.getInvisibleConditionName(), colourIdentifier);
	}

	@Override
	public void visitedColourPicker(ColourPicker colour,
										boolean parentVisible,
										boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitCombo(Combo combo, boolean parentVisible, boolean parentEnabled) {
		String binding = combo.getBinding();
		String comboIdentifier = "Combo " + binding;
		if (dataWidgetBinding != null) {
			comboIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							true,
							false,
							comboIdentifier,
							AttributeType.enumeration,
							AttributeType.text,
							AttributeType.association,
							AttributeType.inverseOne,
							AttributeType.bool,
							AttributeType.integer,
							AttributeType.longInteger);
		validateConditionName(combo.getDisabledConditionName(), comboIdentifier);
		validateConditionName(combo.getInvisibleConditionName(), comboIdentifier);
	}

	@Override
	public void visitedCombo(Combo combo, boolean parentVisible, boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitContentImage(ContentImage image, boolean parentVisible, boolean parentEnabled) {
		String binding = image.getBinding();
		String imageIdentifier = "ContentImage " + binding;
		if (dataWidgetBinding != null) {
			imageIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding, 
							binding,
							true,
							false,
							false,
							true,
							imageIdentifier,
							AttributeType.content);
		validateConditionName(image.getDisabledConditionName(), imageIdentifier);
		validateConditionName(image.getInvisibleConditionName(), imageIdentifier);
	}

	@Override
	public void visitContentLink(ContentLink link, boolean parentVisible, boolean parentEnabled) {
		String binding = link.getBinding();
		String linkIdentifier = "ContentLink " + link.getBinding();
		if (dataWidgetBinding != null) {
			linkIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							false,
							false,
							false,
							true,
							linkIdentifier,
							AttributeType.content);
		validateConditionName(link.getDisabledConditionName(), linkIdentifier);
		validateConditionName(link.getInvisibleConditionName(), linkIdentifier);
		validateParameterBindings(link.getParameters(), linkIdentifier);
	}

	@Override
	public void visitDataGrid(DataGrid grid, boolean parentVisible, boolean parentEnabled) {
		visitDataWidget(grid, "DataGrid");
		validateBinding(null, grid.getSelectedIdBinding(), false, false, false, true, dataWidgetIdentifier, AttributeType.id);
		validateConditionName(grid.getDisabledConditionName(), dataWidgetIdentifier);
		validateConditionName(grid.getDisableAddConditionName(), dataWidgetIdentifier);
		validateConditionName(grid.getDisableEditConditionName(), dataWidgetIdentifier);
		validateConditionName(grid.getDisableRemoveConditionName(), dataWidgetIdentifier);
		validateConditionName(grid.getDisableZoomConditionName(), dataWidgetIdentifier);
	}

	@Override
	public void visitDataRepeater(DataRepeater repeater, boolean parentVisible, boolean parentEnabled) {
		visitDataWidget(repeater, "DataRepeater");
	}

	private void visitDataWidget(AbstractDataWidget widget, String widgetDescription) {
		String title = widget.getTitle();
		dataWidgetBinding = widget.getBinding();
		StringBuilder sb = new StringBuilder(64);
		sb.append(widgetDescription);
		String widgetId = widget.getWidgetId();
		if (widgetId != null) {
			sb.append(" with id ").append(widgetId);
		}
		if (title != null) {
			sb.append((sb.length() > 12) ? " and " : " with ").append("title ").append(title);
		}
		sb.append((sb.length() > 12) ? " and " : " with ").append("binding ").append(dataWidgetBinding);
		dataWidgetIdentifier = sb.toString();
		validateBinding(null,
							dataWidgetBinding,
							true,
							false,
							false,
							false,
							dataWidgetIdentifier);
		validateConditionName(widget.getInvisibleConditionName(), dataWidgetIdentifier);
	}

	@Override
	public void visitDataGridBoundColumn(DataGridBoundColumn column,
											boolean parentVisible,
											boolean parentEnabled) {
		String columnIdentifier = "Column " + column.getTitle() + " of " + dataWidgetIdentifier;
		validateBinding(dataWidgetBinding,
							column.getBinding(),
							false,
							false,
							false,
							false,
							columnIdentifier);
	}

	@Override
	public void visitedDataGridBoundColumn(DataGridBoundColumn column,
											boolean parentVisible, 
											boolean parentEnabled) {
		// nothing to do
	}

	@Override
	public void visitDataGridContainerColumn(DataGridContainerColumn column,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do
	}

	@Override
	public void visitedDataGridContainerColumn(DataGridContainerColumn column,
												boolean parentVisible, 
												boolean parentEnabled) {
		// nothing to do
	}

	@Override
	public void visitDialogButton(DialogButton button, boolean parentVisible, boolean parentEnabled) {
		String buttonIdentifier = "A Dialog Button" + button.getDialogName();
		validateConditionName(button.getDisabledConditionName(), buttonIdentifier);
		validateConditionName(button.getInvisibleConditionName(), buttonIdentifier);
		validateParameterBindings(button.getParameters(), buttonIdentifier);
	}

	@Override
	public void visitForm(Form form, boolean parentVisible, boolean parentEnabled) {
		String formIdentifier = form.getWidgetId();
		if (formIdentifier == null) {
			formIdentifier = "A Form";
		}
		else {
			formIdentifier = "Form " + formIdentifier;
		}
		validateConditionName(form.getDisabledConditionName(), formIdentifier);
		validateConditionName(form.getInvisibleConditionName(), formIdentifier);
		validateMessageBindings(form.getBorderTitle(), formIdentifier, "borderTitle");
	}

	@Override
	public void visitFormColumn(FormColumn column, boolean parentVisible, boolean parentEnabled) {
		// nothing to validate
	}

	@Override
	public void visitFormRow(FormRow row, boolean parentVisible, boolean parentEnabled) {
		if (row.getItems().isEmpty()) {
			throw new MetaDataException("A form row in " + viewIdentifier + " is empty (has not items declared).");
		}
	}

	@Override
	public void visitFormItem(FormItem item, boolean parentVisible, boolean parentEnabled) {
		// TODO Implement FormItem validation
	}

	@Override
	public void visitGeoLocator(GeoLocator locator, boolean parentVisible, boolean parentEnabled) {
		String locatorIdentifier = "A GeoLocator";
		validateBinding(null,
							locator.getAddressBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.text);
		validateBinding(null,
							locator.getCityBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.text);
		validateBinding(null,
							locator.getCountryBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.text);
		validateBinding(null,
							locator.getDescriptionBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.text);
		validateConditionName(locator.getDisabledConditionName(), locatorIdentifier);
		validateConditionName(locator.getInvisibleConditionName(), locatorIdentifier);
		validateBinding(null,
							locator.getLatitudeBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.decimal10);
		validateBinding(null,
							locator.getLongitudeBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.decimal10);
		validateBinding(null,
							locator.getPostcodeBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.text);
		validateBinding(null,
							locator.getStateBinding(),
							false,
							false,
							false,
							true,
							locatorIdentifier,
							AttributeType.text);
	}

	@Override
	public void visitGeometry(Geometry geometry, boolean parentVisible, boolean parentEnabled) {
		String geometryIdentifier = "Geometry " + geometry.getBinding();
		validateBinding(null,
							geometry.getBinding(),
							true,
							false,
							false,
							true,
							geometryIdentifier,
							AttributeType.geometry);
		validateConditionName(geometry.getDisabledConditionName(), geometryIdentifier);
		validateConditionName(geometry.getInvisibleConditionName(), geometryIdentifier);
	}

	@Override
	public void visitMap(MapDisplay map, boolean parentVisible, boolean parentEnabled) {
		String geometryIdentifier = "Map with model " + map.getModelName();
		validateConditionName(map.getInvisibleConditionName(), geometryIdentifier);
		validateMapModelName(map.getModelName(), geometryIdentifier);
	}

	@Override
	public void visitHBox(HBox hbox, boolean parentVisible, boolean parentEnabled) {
		String borderTitle = hbox.getBorderTitle();
		String id = hbox.getWidgetId();
		String boxIdentifier = ((id == null) ? "A HBox" : "HBox " + id) + ((borderTitle == null) ? "" : " titled " + borderTitle);
		validateConditionName(hbox.getInvisibleConditionName(), boxIdentifier);
		validateMessageBindings(hbox.getBorderTitle(), boxIdentifier, "borderTitle");
	}

	@Override
	public void visitHTML(HTML html, boolean parentVisible, boolean parentEnabled) {
		String binding = html.getBinding();
		String htmlIdentifier = "HTML " + html.getBinding();
		if (dataWidgetBinding != null) {
			htmlIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							htmlIdentifier,
							AttributeType.markup);
		validateConditionName(html.getDisabledConditionName(), htmlIdentifier);
		validateConditionName(html.getInvisibleConditionName(), htmlIdentifier);
	}

	@Override
	public void visitBlurb(Blurb blurb,
							boolean parentVisible,
							boolean parentEnabled) {
		String blurbIdentifier = "A Blurb";
		if (dataWidgetBinding != null) {
			blurbIdentifier += " in" + dataWidgetIdentifier;
		}
		String markup = blurb.getMarkup();
		if (markup == null) {
			throw new MetaDataException(blurbIdentifier + " in " + viewIdentifier + " has no markup specified.");
		}
		validateMessageBindings(blurb.getMarkup(), blurbIdentifier, "markup");
		validateConditionName(blurb.getInvisibleConditionName(), blurbIdentifier);
	}

	@Override
	public void visitLabel(Label label, boolean parentVisible, boolean parentEnabled) {
		String labelIdentifier = "A Label";
		if (dataWidgetBinding != null) {
			labelIdentifier += " in " + dataWidgetIdentifier;
		}

		validateBinding(dataWidgetBinding,
							label.getBinding(),
							false,
							false,
							false,
							true,
							labelIdentifier);
		validateBinding(dataWidgetBinding,
							label.getFor(),
							false,
							false,
							false,
							true,
							labelIdentifier);
		validateMessageBindings(label.getValue(), labelIdentifier, "a value");
		validateConditionName(label.getInvisibleConditionName(), labelIdentifier);
	}

	@Override
	public void visitListGrid(ListGrid grid, boolean parentVisible, boolean parentEnabled) {
		String queryName = grid.getQueryName();
		String modelName = grid.getModelName();
		String listGridIdentifier = "ListGrid " + ((modelName != null) ? modelName : queryName);
		validateConditionName(grid.getDisabledConditionName(), listGridIdentifier);
		validateConditionName(grid.getInvisibleConditionName(), listGridIdentifier);
		validateConditionName(grid.getDisableAddConditionName(), listGridIdentifier);
		validateConditionName(grid.getDisableEditConditionName(), listGridIdentifier);
		validateConditionName(grid.getDisableZoomConditionName(), listGridIdentifier);
		validateConditionName(grid.getDisableRemoveConditionName(), listGridIdentifier);
		validateConditionName(grid.getPostRefreshConditionName(), listGridIdentifier);
		validateBinding(null, grid.getSelectedIdBinding(), false, false, false, true, listGridIdentifier, AttributeType.id);
		validateParameterBindings(grid.getParameters(), listGridIdentifier);
		validateQueryOrModel(queryName, modelName, listGridIdentifier);
	}

	@Override
	public void visitListRepeater(ListRepeater repeater, boolean parentVisible, boolean parentEnabled) {
		String queryName = repeater.getQueryName();
		String modelName = repeater.getModelName();
		String listRepeaterIdentifier = "ListRepeater " + ((modelName != null) ? modelName : queryName);
		validateConditionName(repeater.getInvisibleConditionName(), listRepeaterIdentifier);
		validateConditionName(repeater.getPostRefreshConditionName(), listRepeaterIdentifier);
		validateParameterBindings(repeater.getParameters(), listRepeaterIdentifier);
		validateQueryOrModel(queryName, modelName, listRepeaterIdentifier);
	}

	@Override
	public void visitTreeGrid(TreeGrid grid, boolean parentVisible, boolean parentEnabled) {
		String queryName = grid.getQueryName();
		String modelName = grid.getModelName();
		String treeGridIdentifier = "TreeGrid " + ((modelName != null) ? modelName : queryName);
		validateConditionName(grid.getDisabledConditionName(), treeGridIdentifier);
		validateConditionName(grid.getInvisibleConditionName(), treeGridIdentifier);
		validateConditionName(grid.getDisableAddConditionName(), treeGridIdentifier);
		validateConditionName(grid.getDisableEditConditionName(), treeGridIdentifier);
		validateConditionName(grid.getDisableZoomConditionName(), treeGridIdentifier);
		validateConditionName(grid.getDisableRemoveConditionName(), treeGridIdentifier);
		validateConditionName(grid.getPostRefreshConditionName(), treeGridIdentifier);
		validateBinding(null, grid.getSelectedIdBinding(), false, false, false, true, treeGridIdentifier, AttributeType.id);
		validateBinding(null, grid.getRootIdBinding(), false, false, false, true, treeGridIdentifier);
		validateParameterBindings(grid.getParameters(), treeGridIdentifier);
		validateQueryOrModel(queryName, modelName, treeGridIdentifier);
	}

	@Override
	public void visitListMembership(ListMembership membership, boolean parentVisible, boolean parentEnabled) {
		String membershipIdentifier = "ListMembership " + membership.getBinding();
		validateBinding(null,
							membership.getBinding(),
							true,
							false,
							true,
							false,
							membershipIdentifier,
							AttributeType.collection,
							AttributeType.inverseMany);
		validateConditionName(membership.getDisabledConditionName(), membershipIdentifier);
		validateConditionName(membership.getInvisibleConditionName(), membershipIdentifier);
	}

	@Override
	public void visitedListMembership(ListMembership membership,
										boolean parentVisible,
										boolean parentEnabled) {
		// do nothing
	}
	
	@Override
	public void visitComparison(Comparison comparison,
									boolean parentVisible,
									boolean parentEnabled) {
		String comparisonIdentifier = "Comparison " + comparison.getBinding();
		validateBinding(null,
							comparison.getBinding(),
							true,
							true,
							false,
							false,
							comparisonIdentifier,
							AttributeType.association);
		validateConditionName(comparison.getDisabledConditionName(), comparisonIdentifier);
		validateConditionName(comparison.getInvisibleConditionName(), comparisonIdentifier);
		validateComparisonModelName(comparison.getModelName(), comparisonIdentifier);
	}

	@Override
	public void visitLookup(Lookup lookup, boolean parentVisible, boolean parentEnabled) {
		String binding = lookup.getBinding();
		String lookupIdentifier = "Lookup " + binding;
		if (dataWidgetBinding != null) {
			lookupIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							true,
							false,
							false,
							lookupIdentifier,
							AttributeType.association);
		validateConditionName(lookup.getDisabledConditionName(), lookupIdentifier);
		validateConditionName(lookup.getInvisibleConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisablePickConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisableEditConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisableAddConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisableClearConditionName(), lookupIdentifier);
		validateParameterBindings(lookup.getParameters(), lookupIdentifier);
		validateQueryName(lookup.getQuery(), lookupIdentifier);
	}

	@Override
	public void visitedLookup(Lookup lookup, boolean parentVisible, boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitLookupDescription(LookupDescription lookup, boolean parentVisible, boolean parentEnabled) {
		String binding = lookup.getBinding();
		String descriptionBinding = lookup.getDescriptionBinding();
		String lookupIdentifier = "LookupDescription " + binding;
		// A lookupDescription in a data grid bound to an aggregated collection 
		// doesn't have to have a binding
		validateBinding(dataWidgetBinding,
							binding,
							(dataWidgetBinding == null),
							false,
							false,
							false,
							lookupIdentifier,
							AttributeType.association,
							AttributeType.inverseOne);
		validateBinding(dataWidgetBinding,
							// binding can be null if dataGridBinding is set and this 
							// is a lookup to the elements in the collection
							(binding == null) ? descriptionBinding : BindUtil.createCompoundBinding(binding, descriptionBinding),
							true,
							false,
							false,
							true,
							lookupIdentifier);
		validateConditionName(lookup.getDisabledConditionName(), lookupIdentifier);
		validateConditionName(lookup.getInvisibleConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisablePickConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisableEditConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisableAddConditionName(), lookupIdentifier);
		validateConditionName(lookup.getDisableClearConditionName(), lookupIdentifier);
		validateParameterBindings(lookup.getParameters(), lookupIdentifier);
		validateQueryName(lookup.getQuery(), lookupIdentifier);
		
		// determine the query that will be used
		MetaDataQueryDefinition query = null;
		if (lookup.getQuery() != null) {
    		query = module.getMetaDataQuery(lookup.getQuery());
    	}
		else {
			// NB Use getMetaDataForBinding() to ensure we find attributes from base documents inherited
			String fullBinding = binding;
			if (dataWidgetBinding != null) {
				if (binding == null) {
					fullBinding = dataWidgetBinding;
				}
				else {
					fullBinding = BindUtil.createCompoundBinding(dataWidgetBinding, binding);
				}
			}
			TargetMetaData target = Binder.getMetaDataForBinding(customer, module, document, fullBinding);
    		Relation relation = (Relation) target.getAttribute();
    		String queryName = (relation instanceof Reference) ? ((Reference) relation).getQueryName() : null;
    		if (queryName != null) {
        		query = module.getMetaDataQuery(queryName);
    		}
    		else {
    			query = module.getDocumentDefaultQuery(customer, relation.getDocumentName());
    		}
		}

		// validate drop down columns and description binding
		Set<String> dropDownColumns = lookup.getDropDownColumns();
		LinkedHashSet<String> testColumns = ((dropDownColumns == null) || dropDownColumns.isEmpty()) ? 
												null : 
												new LinkedHashSet<>(dropDownColumns);
		boolean foundLookupDescription = Bean.BIZ_KEY.equals(descriptionBinding);
		
		for (MetaDataQueryColumn column : query.getColumns()) {
    		String alias = column.getName();
    		if (alias == null) {
    			alias = column.getBinding();
    		}
    		MetaDataQueryProjectedColumn projectedColumn = (column instanceof MetaDataQueryProjectedColumn) ? 
    															(MetaDataQueryProjectedColumn) column : 
																null;
    		if ((testColumns != null) && testColumns.contains(alias)) {
        		if ((projectedColumn != null) && (! projectedColumn.isProjected())) {
					throw new MetaDataException(lookupIdentifier + " in " + viewIdentifier + " has a drop down column of " + alias + " which is not projected in the query.");
        		}
        		testColumns.remove(alias);
            }
            if ((! foundLookupDescription) && descriptionBinding.equals(alias)) {
        		if ((projectedColumn != null) && (! projectedColumn.isProjected())) {
        			throw new MetaDataException(lookupIdentifier + " in " + viewIdentifier + " has a description binding of " + alias + " which is not projected in the query.");
        		}
            	foundLookupDescription = true;
            }
    	}
		
    	if (! foundLookupDescription) {
			throw new MetaDataException(lookupIdentifier + " in " + viewIdentifier + " has a description binding of " + descriptionBinding + " which is not defined in the query.");
    	}
    	if ((testColumns != null) && (! testColumns.isEmpty())) {
			throw new MetaDataException(lookupIdentifier + " in " + viewIdentifier + " has a drop down column of " + testColumns.iterator().next() + " which is not defined in the query.");
    	}
	}

	@Override
	public void visitedLookupDescription(LookupDescription lookup, boolean parentVisible, boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitParameter(Parameter parameter, boolean parentVisible, boolean parentEnabled) {
		// no validation required as parameters are checked by their parent widgets
	}

	@Override
	public void visitFilterParameter(FilterParameter parameter, boolean parentVisible, boolean parentEnabled) {
		// no validation required as parameters are checked by their parent widgets
	}

	@Override
	public void visitPassword(Password password, boolean parentVisible, boolean parentEnabled) {
		String binding = password.getBinding();
		String passwordIdentifier = "Password " + binding;
		if (dataWidgetBinding != null) {
			passwordIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							passwordIdentifier,
							AttributeType.text);
		validateConditionName(password.getDisabledConditionName(), passwordIdentifier);
		validateConditionName(password.getInvisibleConditionName(), passwordIdentifier);
	}

	@Override
	public void visitedPassword(Password password,
									boolean parentVisible,
									boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitProgressBar(ProgressBar progressBar, boolean parentVisible, boolean parentEnabled) {
		String progressBarIdentifier = "ProgressBar " + progressBar.getBinding();
		validateBinding(null,
							progressBar.getBinding(),
							true,
							false,
							false,
							true,
							progressBarIdentifier);
		validateConditionName(progressBar.getInvisibleConditionName(), progressBarIdentifier);
	}

	@Override
	public void visitRadio(Radio radio, boolean parentVisible, boolean parentEnabled) {
		String binding = radio.getBinding();
		String radioIdentifier = "Radio " + binding;
		if (dataWidgetBinding != null) {
			radioIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							true,
							false,
							radioIdentifier,
							AttributeType.enumeration,
							AttributeType.text,
							AttributeType.association,
							AttributeType.inverseOne,
							AttributeType.bool,
							AttributeType.integer,
							AttributeType.longInteger);
		validateConditionName(radio.getDisabledConditionName(), radioIdentifier);
		validateConditionName(radio.getInvisibleConditionName(), radioIdentifier);
	}

	@Override
	public void visitedRadio(Radio radio,
								boolean parentVisible,
								boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitRichText(RichText richText, boolean parentVisible, boolean parentEnabled) {
		String binding = richText.getBinding();
		String richTextIdentifier = "RichText " + binding;
		if (dataWidgetBinding != null) {
			richTextIdentifier += " in " + dataWidgetIdentifier;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							richTextIdentifier,
							AttributeType.memo,
							AttributeType.markup);
		validateConditionName(richText.getDisabledConditionName(), richTextIdentifier);
		validateConditionName(richText.getInvisibleConditionName(), richTextIdentifier);
	}

	@Override
	public void visitedRichText(RichText richText,
									boolean parentVisible,
									boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitSlider(Slider slider, boolean parentVisible, boolean parentEnabled) {
		String binding = slider.getBinding();
		String sliderIdentifier = "Slider " + binding;
		if (dataWidgetBinding != null) {
			sliderIdentifier += " in " + dataWidgetBinding;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							sliderIdentifier);
		validateConditionName(slider.getDisabledConditionName(), sliderIdentifier);
		validateConditionName(slider.getInvisibleConditionName(), sliderIdentifier);
	}

	@Override
	public void visitedSlider(Slider slider,
								boolean parentVisible,
								boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitSpacer(Spacer spacer) {
		validateConditionName(spacer.getInvisibleConditionName(), "A Spacer");
	}

	@Override
	public void visitSpinner(Spinner spinner, boolean parentVisible, boolean parentEnabled) {
		String binding = spinner.getBinding();
		String spinnerIdentifier = "Spinner " + binding;
		if (dataWidgetBinding != null) {
			spinnerIdentifier += " in " + dataWidgetBinding;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							spinnerIdentifier);
		validateConditionName(spinner.getDisabledConditionName(), spinnerIdentifier);
		validateConditionName(spinner.getInvisibleConditionName(), spinnerIdentifier);
	}

	@Override
	public void visitedSpinner(Spinner spinner,
								boolean parentVisible,
								boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitStaticImage(StaticImage image, boolean parentVisible, boolean parentEnabled) {
		String imageIdentifier = "StaticImage " + image.getRelativeFile();
		validateConditionName(image.getInvisibleConditionName(), imageIdentifier);
	}

	@Override
	public void visitLink(Link link, boolean parentVisible, boolean parentEnabled) {
		final String linkIdentifier = "Link " + link.getValue();
		validateConditionName(link.getInvisibleConditionName(), linkIdentifier);

		new ReferenceProcessor() {
			@SuppressWarnings("synthetic-access")
			private ModuleImpl validateReferenceModuleName(String referenceModuleName, 
															String referenceDescription) {
				ModuleImpl result = null;
				
				if (referenceModuleName.indexOf('{') < 0) {
					try {
						result = (ModuleImpl) customer.getModule(referenceModuleName);
						if (result == null) {
							throw new MetaDataException(referenceModuleName + " DNE");
						}
					}
					catch (Exception e) {
						throw new MetaDataException(linkIdentifier + " in " + 
														viewIdentifier + " has " + 
														referenceDescription + " reference with an invalid module of " + 
														referenceModuleName, e);
					}
				}
				
				return result;
			}
			
			@SuppressWarnings("synthetic-access")
			private DocumentImpl validateReferenceDocumentName(ModuleImpl referenceModule,
															String referenceDocumentName,
															String referenceDescription) {
				DocumentImpl result = null;
				
				if (referenceDocumentName.indexOf('{') < 0) {
					try {
						result = (DocumentImpl) referenceModule.getDocument(customer, referenceDocumentName);
						if (result == null) {
							throw new MetaDataException(referenceDocumentName + " DNE");
						}
						return result;
					}
					catch (Exception e) {
						throw new MetaDataException(linkIdentifier + " in " + 
														viewIdentifier + " has " + 
														referenceDescription + " reference with an invalid document of " + 
														referenceModule.getName() + '.' + referenceDocumentName, e);
					}
				}
				
				return result;
			}
			
			@SuppressWarnings("synthetic-access")
			private TargetMetaData validateReferenceBinding(String referenceBinding,
																String referenceDescription) {
				String bindingToTest = referenceBinding;
				if (dataWidgetBinding != null) {
					if (referenceBinding == null) {
						bindingToTest = dataWidgetBinding;
					}
					else {
						bindingToTest = BindUtil.createCompoundBinding(dataWidgetBinding, referenceBinding);
					}
				}

				TargetMetaData result = null;
				// NB bindingToTest can be null when a link to an edit view for a new document instance
				// and the link is NOT in a grid container column
				if (bindingToTest != null) {
					try {
						result = BindUtil.getMetaDataForBinding(customer, module, document, bindingToTest);
						if (result == null) {
							throw new IllegalStateException("Target DNE");
						}
						return result;
					}
					catch (MetaDataException e) {
						throw new MetaDataException(linkIdentifier + " in " + viewIdentifier + 
														" has " + referenceDescription + 
														" reference with an invalid binding of " + referenceBinding, e);
					}
				}
				
				return result;
			}
			
			@Override
			public void processResourceReference(ResourceReference reference) {
				// nothing to do here
			}
			
			@Override
			@SuppressWarnings("synthetic-access")
			public void processReportReference(ReportReference reference) {
				ModuleImpl reportModule = validateReferenceModuleName(reference.getModuleName(),
																	"a report");
				if (reportModule != null) { // valid module name with no '{'
					DocumentImpl reportDocument = validateReferenceDocumentName(reportModule,
																				reference.getDocumentName(),
																				"a report");
					if (reportDocument != null) { // valid module name with no '{'
						try {
							if (CORE.getRepository().getReportFileName(customer, reportDocument, reference.getReportName()) == null) {
								throw new IllegalStateException("Report DNE");
							}
						}
						catch (Exception e) { // could be NPE or IllegalArgument etc etc
							throw new MetaDataException(linkIdentifier + " in " + viewIdentifier + 
															" has a report reference with an invalid report name of " + 
															reportModule.getName() + '.' + 
															reportDocument.getName() + '.' +
															reference.getReportName(), e);
						}
					}
				}
				validateParameterBindings(reference.getParameters(), linkIdentifier);
			}
			
			@Override
			@SuppressWarnings("synthetic-access")
			public void processQueryListViewReference(QueryListViewReference reference) {
				try {
					if (module.getMetaDataQuery(reference.getQueryName()) == null) {
						throw new IllegalStateException("No such query");
					}
				}
				catch (Exception e) {
					throw new MetaDataException(linkIdentifier + " in " + viewIdentifier + 
													" has a query list view reference with an invalid query name of " + 
													reference.getQueryName(), e);
				}
			}
			
			@Override
			public void processImplicitActionReference(ImplicitActionReference reference) {
				// nothing to do here
			}
			
			@Override
			public void processExternalReference(ExternalReference reference) {
				// nothing to do here
			}
			
			@Override
			public void processEditViewReference(EditViewReference reference) {
				ModuleImpl viewModule = validateReferenceModuleName(reference.getModuleName(), "an edit view");
				if (viewModule != null) { // valid module name with no '{'
					validateReferenceDocumentName(viewModule, 
													reference.getDocumentName(),
													"an edit view");
				}
				validateReferenceBinding(reference.getBinding(), "an edit view");
			}
			
			@Override
			public void processDefaultListViewReference(DefaultListViewReference reference) {
				ModuleImpl viewModule = validateReferenceModuleName(reference.getModuleName(), "an edit view");
				if (viewModule != null) { // valid module name with no '{'
					validateReferenceDocumentName(viewModule, 
													reference.getDocumentName(),
													"an edit view");
				}
				validateReferenceBinding(reference.getBinding(), "an edit view");
			}
			
			@Override
			@SuppressWarnings("synthetic-access")
			public void processContentReference(ContentReference reference) {
				String widgetidentifier = linkIdentifier + " with a content reference";
				if (dataWidgetBinding != null) {
					widgetidentifier += " in " + dataWidgetIdentifier;
				}
				validateBinding(dataWidgetBinding,
									reference.getBinding(),
									true,
									false,
									false,
									true,
									widgetidentifier,
									AttributeType.content);
			}
			
			@Override
			@SuppressWarnings("synthetic-access")
			public void processActionReference(ActionReference reference) {
				String widgetIdentifier = linkIdentifier + " with an action reference";
				if (dataWidgetBinding != null) { // in a repeater or grid
					widgetIdentifier += " in " + dataWidgetBinding;
					String actionName = reference.getActionName();
					try {
						TargetMetaData target = validateReferenceBinding(null, "an action reference");
						Reference targetReference = (Reference) target.getAttribute();
						if (targetReference == null) {
							throw new MetaDataException("Target Reference " + dataWidgetBinding + " DNE");
						}
						ModuleImpl targetModule = (ModuleImpl) customer.getModule(target.getDocument().getOwningModuleName());
						DocumentImpl targetDocument = (DocumentImpl) targetModule.getDocument(customer, targetReference.getDocumentName());
						
						// This is a container column of an existing row in a table/grid - so get the edit view
						ViewImpl targetView = (ViewImpl) targetDocument.getView(uxui, customer, ViewType.edit.toString());
						if (targetView.getAction(actionName) == null) {
							throw new MetaDataException(actionName + " DNE");
						}
					}
					catch (Exception e) {
						throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + " references a non-existent action " + actionName, e);
					}
				}
				else {
					validateActionName(reference.getActionName(), widgetIdentifier);
				}
			}
		}.process(link.getReference());
	}

	@Override
	public void visitTab(Tab tab, boolean parentVisible, boolean parentEnabled) {
		String tabIdentifier = "Tab " + tab.getTitle();
		validateConditionName(tab.getDisabledConditionName(), tabIdentifier);
		validateConditionName(tab.getInvisibleConditionName(), tabIdentifier);
		validateMessageBindings(tab.getTitle(), tabIdentifier, "title");
	}

	@Override
	public void visitTabPane(TabPane tabPane, boolean parentVisible, boolean parentEnabled) {
		String tabPaneIdentifier = tabPane.getWidgetId();
		if (tabPaneIdentifier != null) {
			tabPaneIdentifier = "TabPane " + tabPaneIdentifier;
		}
		else {
			tabPaneIdentifier = "A TabPane";
		}
		validateConditionName(tabPane.getDisabledConditionName(), tabPaneIdentifier);
		validateConditionName(tabPane.getInvisibleConditionName(), tabPaneIdentifier);
		validateBinding(null, 
							tabPane.getSelectedTabIndexBinding(), 
							false, 
							false, 
							false, 
							true, 
							"The [selectedTabIndexBinding] of " + tabPaneIdentifier, 
							AttributeType.integer, 
							AttributeType.longInteger);
	}

	@Override
	public void visitTextArea(TextArea text, boolean parentVisible, boolean parentEnabled) {
		String binding = text.getBinding();
		String textIdentifier = "TextArea " + binding;
		if (dataWidgetBinding != null) {
			textIdentifier += " in " + dataWidgetBinding;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							textIdentifier);
		validateConditionName(text.getDisabledConditionName(), textIdentifier);
		validateConditionName(text.getInvisibleConditionName(), textIdentifier);
	}

	@Override
	public void visitedTextArea(TextArea text,
									boolean parentVisible,
									boolean parentEnabled) {
		// do nothing
	}

	@Override
	public void visitTextField(TextField text, boolean parentVisible, boolean parentEnabled) {
		String binding = text.getBinding();
		String textIdentifier = "Text " + binding;
		if (dataWidgetBinding != null) {
			textIdentifier += " in " + dataWidgetBinding;
		}
		validateBinding(dataWidgetBinding,
							binding,
							true,
							false,
							false,
							true,
							textIdentifier);
		validateConditionName(text.getDisabledConditionName(), textIdentifier);
		validateConditionName(text.getInvisibleConditionName(), textIdentifier);
	}

	@Override
	public void visitedTextField(TextField text,
									boolean parentVisible,
									boolean parentEnabled) {
		// do nothing
	}

	
	@Override
	public void visitInject(Inject inject,
								boolean parentVisible,
								boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitVBox(VBox vbox, boolean parentVisible, boolean parentEnabled) {
		String borderTitle = vbox.getBorderTitle();
		String id = vbox.getWidgetId();
		String boxIdentifier = ((id == null) ? "A VBox" : "VBox " + id) + ((borderTitle == null) ? "" : " titled " + borderTitle);
		validateConditionName(vbox.getInvisibleConditionName(), boxIdentifier);
		validateMessageBindings(vbox.getBorderTitle(), boxIdentifier, "borderTitle");
	}

	@Override
	public void visitView() {
		validateMessageBindings(view.getTitle(), viewIdentifier, "a title");
		validateParameterBindings(view.getParameters(), viewIdentifier);
		validateActionName(view.getRefreshActionName(), viewIdentifier);
		validateConditionName(view.getRefreshConditionName(), viewIdentifier);
	}

	@Override
	public void visitedDataGrid(DataGrid grid, boolean parentVisible, boolean parentEnabled) {
		dataWidgetBinding = null;
		dataWidgetIdentifier = null;
	}

	@Override
	public void visitedDataRepeater(DataRepeater repeater, boolean parentVisible, boolean parentEnabled) {
		dataWidgetBinding = null;
		dataWidgetIdentifier = null;
	}

	@Override
	public void visitedForm(Form form, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedFormItem(FormItem item, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedFormRow(FormRow row, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedHBox(HBox hbox, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedListGrid(ListGrid grid, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedListRepeater(ListRepeater repeater, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedTreeGrid(TreeGrid grid, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedTab(Tab tab, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedTabPane(TabPane tabPane, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedVBox(VBox vbox, boolean parentVisible, boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitedView() {
		// nothing to do here
	}

	private void validateAction(ActionImpl action) {
		String actionIdentifier = "Action " + action.getName();
		
		validateConditionName(action.getDisabledConditionName(), actionIdentifier);
		validateConditionName(action.getInvisibleConditionName(), actionIdentifier);
		validateParameterBindings(action.getParameters(), actionIdentifier);
	}
	
	// validate the resource name which represents the class to load for ClassActions
	private void validateClassAction(String resourceName) {
		String fullyQualifiedJavaCodeName = String.format("%s.%s.actions.%s", 
															document.getOwningModuleName(),
															document.getName(),
															resourceName);
		if (AbstractRepository.get().getJavaClass(customer, fullyQualifiedJavaCodeName) == null) {
			throw new MetaDataException(fullyQualifiedJavaCodeName + " not found.");
		}
	}
	
	@Override
	public void visitCustomAction(ActionImpl action) {
		validateClassAction(action.getResourceName());
		validateAction(action);
	}

	@Override
	public void visitAddAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitBizExportAction(ActionImpl action) {
		validateClassAction(action.getResourceName());
		validateAction(action);
	}

	@Override
	public void visitBizImportAction(ActionImpl action) {
		validateClassAction(action.getResourceName());
		validateAction(action);
	}

	@Override
	public void visitDownloadAction(ActionImpl action) {
		validateClassAction(action.getResourceName());
		validateAction(action);
	}

	@Override
	public void visitUploadAction(ActionImpl action) {
		validateClassAction(action.getResourceName());
		validateAction(action);
	}

	@Override
	public void visitCancelAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitDeleteAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitEditAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitNavigateAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitNewAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitOKAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitRemoveAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitReportAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitSaveAction(ActionImpl action) {
		validateAction(action);
	}

	@Override
	public void visitZoomOutAction(ActionImpl action) {
		validateAction(action);
	}

	private static void validateEventHandlerSequence(List<EventAction> actions, String widgetIdentifier) {
		if (actions != null) {
			Iterator<EventAction> i = actions.iterator();
			while (i.hasNext()) {
				EventAction action = i.next();
				if (i.hasNext()) {
					if (action instanceof RerenderEventAction) {
						throw new MetaDataException("[rerender] event action in " +  widgetIdentifier +
														" has to be the last action as it is a server-side action.");
					}
					else if (action instanceof ServerSideActionEventAction) {
						throw new MetaDataException("[server] event action to action " +
														((ServerSideActionEventAction) action).getActionName() +
														" in " +  widgetIdentifier +
														" has to be the last action as it is a server-side action.");
					}
				}
			}
		}
	}
	
	@Override
	public void visitOnChangedEventHandler(Changeable changeable,
											boolean parentVisible,
											boolean parentEnabled) {
		validateEventHandlerSequence(changeable.getChangedActions(),
										"[onChanged] event handler for widget with binding " + 
											changeable.getBinding());
	}

	@Override
	public void visitedOnChangedEventHandler(Changeable changeable,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnFocusEventHandler(Focusable focusable,
											boolean parentVisible,
											boolean parentEnabled) {
		String binding = (focusable instanceof Bound) ? ((Bound) focusable).getBinding() : "unknown";
		validateEventHandlerSequence(focusable.getFocusActions(),
				"[onFocus] event handler for widget with binding " + binding);
	}

	@Override
	public void visitedOnFocusEventHandler(Focusable blurable,
											boolean parentVisible,
											boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnBlurEventHandler(Focusable focusable,
											boolean parentVisible,
											boolean parentEnabled) {
		String binding = (focusable instanceof Bound) ? ((Bound) focusable).getBinding() : "unknown";
		validateEventHandlerSequence(focusable.getBlurActions(),
				"[onBlur] event handler for widget with binding " + binding);
	}

	@Override
	public void visitedOnBlurEventHandler(Focusable blurable,
											boolean parentVisible,
											boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnAddedEventHandler(Addable addable,
											boolean parentVisible,
											boolean parentEnabled) {
		String widgetIdentifier = "Unknown widget";
		if (addable instanceof Bound) {
			widgetIdentifier = "[onAdded] event handler for widget with binding " + ((Bound) addable).getBinding();
		}
		else if (addable instanceof ListGrid) {
			widgetIdentifier = "[onAdded] event handler for list grid with query " + ((ListGrid) addable).getQueryName();
		}
		validateEventHandlerSequence(addable.getAddedActions(), widgetIdentifier);
	}

	@Override
	public void visitedOnAddedEventHandler(Addable addable,
											boolean parentVisible,
											boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnEditedEventHandler(Editable editable,
											boolean parentVisible,
											boolean parentEnabled) {
		String widgetIdentifier = "Unknown widget";
		if (editable instanceof Bound) {
			widgetIdentifier = "[onEdited] event handler for widget with binding " + ((Bound) editable).getBinding();
		}
		else if (editable instanceof ListGrid) {
			widgetIdentifier = "[onEdited] event handler for list grid with query " + ((ListGrid) editable).getQueryName();
		}
		validateEventHandlerSequence(editable.getEditedActions(), widgetIdentifier);
	}

	@Override
	public void visitedOnEditedEventHandler(Editable editable,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnRemovedEventHandler(Removable removable,
											boolean parentVisible,
											boolean parentEnabled) {
		String widgetIdentifier = "Unknown widget";
		if (removable instanceof Bound) {
			widgetIdentifier = "[onRemoved] event handler for widget with binding " + ((Bound) removable).getBinding();
		}
		else if (removable instanceof ListGrid) {
			widgetIdentifier = "[onRemoved] event handler for list grid with query " + ((ListGrid) removable).getQueryName();
		}
		validateEventHandlerSequence(removable.getRemovedActions(), widgetIdentifier);
	}

	@Override
	public void visitedOnRemovedEventHandler(Removable removable,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnSelectedEventHandler(Selectable selectable,
												boolean parentVisible,
												boolean parentEnabled) {
		String widgetIdentifier = "Unknown widget";
		if (selectable instanceof Bound) {
			widgetIdentifier = "[onSelected] event handler for widget with binding " + ((Bound) selectable).getBinding();
		}
		else if (selectable instanceof ListGrid) {
			widgetIdentifier = "[onSelected] event handler for list grid with query " + ((ListGrid) selectable).getQueryName();
		}
		validateEventHandlerSequence(selectable.getSelectedActions(), widgetIdentifier);
	}

	@Override
	public void visitedOnSelectedEventHandler(Selectable editable,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnPickedEventHandler(Lookup lookup,
											boolean parentVisible,
											boolean parentEnabled) {
		validateEventHandlerSequence(lookup.getPickedActions(),
										"[onPicked] event handler for lookup with binding " + 
											lookup.getBinding());
	}

	@Override
	public void visitedOnPickedEventHandler(Lookup lookup,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitOnClearedEventHandler(Lookup lookup,
											boolean parentVisible,
											boolean parentEnabled) {
		validateEventHandlerSequence(lookup.getClearedActions(),
										"[onCleared] event handler for lookup with binding " + 
											lookup.getBinding());
	}

	@Override
	public void visitedOnClearedEventHandler(Lookup lookup,
												boolean parentVisible,
												boolean parentEnabled) {
		// nothing to do here
	}

	@Override
	public void visitRerenderEventAction(RerenderEventAction rerender,
											EventSource source,
											boolean parentVisible,
											boolean parentEnabled) {
		// no properties to check
	}

	@Override
	public void visitServerSideActionEventAction(ServerSideActionEventAction server,
													boolean parentVisible,
													boolean parentEnabled) {
		validateActionName(server.getActionName(), "[server] event action in an event handler");
	}

	@Override
	public void visitSetDisabledEventAction(SetDisabledEventAction setDisabled,
												boolean parentVisible,
												boolean parentEnabled) {
		String widgetIdentifier = "[setDisabled] event action in an event handler";
		validateBinding(null,
							setDisabled.getBinding(),
							true,
							false,
							false,
							false,
							widgetIdentifier,
							AttributeType.bool);
		String disabledConditionName = setDisabled.getDisabledConditionName();
		if (disabledConditionName == null) {
			throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
											" requires a [disabled] or [enabled] condition name.");
		}
		validateConditionName(disabledConditionName, widgetIdentifier);
	}

	@Override
	public void visitToggleDisabledEventAction(ToggleDisabledEventAction toggleDisabled,
												boolean parentVisible,
												boolean parentEnabled) {
		String widgetIdentifier = "[toggleDisabled] event action in an event handler";
		validateBinding(null,
							toggleDisabled.getBinding(),
							true,
							false,
							false,
							false,
							widgetIdentifier,
							AttributeType.bool);
	}

	@Override
	public void visitToggleVisibilityEventAction(ToggleVisibilityEventAction toggleVisibility,
													boolean parentVisible,
													boolean parentEnabled) {
		String widgetIdentifier = "[toggleVisibility] event action in an event handler";
		validateBinding(null,
							toggleVisibility.getBinding(),
							true,
							false,
							false,
							false,
							widgetIdentifier,
							AttributeType.bool);
	}

	@Override
	public void visitSetInvisibleEventAction(SetInvisibleEventAction setInvisible,
												boolean parentVisible,
												boolean parentEnabled) {
		String widgetIdentifier = "[setInvisible] event action in an event handler";
		validateBinding(null,
							setInvisible.getBinding(),
							true,
							false,
							false,
							false,
							widgetIdentifier,
							AttributeType.bool);
		String invisibleConditionName = setInvisible.getInvisibleConditionName();
		if (invisibleConditionName == null) {
			throw new MetaDataException(widgetIdentifier + " in " + viewIdentifier + 
											" requires an [invisible] or [visible] condition name.");
		}
		validateConditionName(invisibleConditionName, widgetIdentifier);
	}
}
