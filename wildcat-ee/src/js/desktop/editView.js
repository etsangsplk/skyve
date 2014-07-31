isc.ClassFactory.defineClass("BizContainer", "VLayout");
// Properties
// contained - an array of members added to this container
BizContainer.addMethods({
	initWidget: function () {
	    this.Super("initWidget", arguments);
	    this.contained = [];
	},
	
	addContained: function(contained) { // the widget to be contained in this container
		this.contained.add(contained);
		this.addMember(contained);
	}
});

isc.ClassFactory.defineClass("EditView", "BizContainer");
isc.EditView.addClassProperties({
	_DATA_SOURCE: isc.RestDataSource.create({
		dataFormat: 'json',
		dataURL: "smartedit",
		// ensure that only datasource defined fields goes down with the request
		sendExtraFields: false
	})
});
// Properties
// title: null - the title of the view
// refreshTimeInSeconds: null - number of seconds until a refresh is required
// refreshConditionName: null - server-side condition name to enable refresh or not
// refreshActionName: null - action to run on the server when doing the refresh
//
// _heading: null - the heading HTML at the top of the list
// _vm: ValuesManager - the values manager for the forms in this view
// _actionPanel: ToolStrip - the top toolbar
// _editPanel: VLayout - the editing area
// _mod: null - The name of the module this view belongs to
// _doc: null - The document name this view belongs to
// _b: null - The binding within the document this view is bound to
// _saved - whether the [Save] button has been pressed
// _openedFromDataGrid - whether this view was opened from a data grid record or not
// _grids: {} - a map of grids by their binding
isc.EditView.addMethods({
	initWidget: function () {
		this.overflow = 'hidden';
		this.membersMargin = 2;
		this.layoutMargin = 2;
		this.margin = 2;
		this._grids = {}; // map of binding -> (map of ID -> dataGrid/comparisonEditor/listMembership/map widget)
		this._refreshedGrids = {}; // map of dataGrid/comparisonEditor/listMembership ID -> boolean (true if refreshed)
        this.Super("initWidget", arguments);
		this._heading = isc.HTMLFlow.create({showEdges:true});

		// not contained here as it is implicit
		this.addMember(this._heading);

		// the action panel - not contained as it is implicit
		this._actionPanel = isc.ToolStrip.create({layoutMargin: 2, membersMargin: 5, width: '100%', margin: 2});
		this.addMember(this._actionPanel);

		// the edit panel - not contained as it is implicit
		this._editPanel = isc.BizContainer.create({layoutMargin: 2, membersMargin: 5, overflow: 'auto'});
		this.addMember(this._editPanel);
		
		// init the values manager with the contained
		var thisView = this;
		this._vm = ValuesManager.create({
			dataSource: EditView._DATA_SOURCE,
			handleHiddenValidationErrors: function(errors) { // map of item name to message or message[]
				
				// collect unique messages
				var messages = [];
				for (var binding in errors) {
					var message = errors[binding];
					
					var tokens = binding.split('_');
					if (tokens[0]) {
						var grids = thisView._grids[tokens[0]];
						if (grids) {
							for (var gridID in grids) {
								var grid = grids[gridID];
								if (grid) {
									if (tokens[3]) {
										if (grid.grid) {
											grid.grid.setFieldError(tokens[1], tokens[3], message);
											grid.grid.markForRedraw();
										}
									}
								}
							}
						}
					}
					
					if (! messages.contains(message)) {
						messages.add(message);
					}
				}

				// display any messages
				if (messages.length > 0) {
					var warning = "<ul>";
					for (var i = 0, l = messages.length; i < l; i++) {
						warning += "<li>" + messages[i] + "</li>";
					}
					warning += "</ul>";
					isc.warn(warning, null, {title: 'Problems'});
				}

				return false; // dont log to console
			},
			
			// this is some bullshit overload to remove oldValues from the request context.
			// Its too big and none of the documented DSRequest parameters from ActionMethods.saveData() work.
			// This sucks arse.
			saveEditorValues: function(values, saveOperation, callback, context) {
				context.oldValues = null;
				this.Super("saveEditorValues", arguments);
			}
		});
	},
	
	addContained: function(contained) { // the widget to be contained in this container
		this._editPanel.addContained(contained);
	},
	
	// Add an action to be shown when editing
	addEditAction: function(button) { // the button to add to the action panel
		button.forCreate = false;
		this._actionPanel.addMember(button);
	},
	
	// Add an action to be shown when creating only
	addCreateAction: function(button) { // the button to add to the action panel
		button.forCreate = true;
		this._actionPanel.addMember(button);
	},

	// Add an action to be shown when creating or editing
	addAction: function(button) { // the button to add to the action panel
		button.forCreate = null;
		this._actionPanel.addMember(button);
	},

	// replace {bindings} in a string expression with the values from the instance
	toDisplay: function(expression, // the expression to parse
							instance) {	// the bean instance to get the values from
		var tokens = expression.match(/\{[A-Za-z0-9._]+\}/g);
		if (tokens) {
			for (var i = 0; i < tokens.length; i++) {
				var token = tokens[i];
				var binding = token.substring(1, token.length - 1).replaceAll('.', '_');
				var evaluation = eval('instance.' + binding);
				if ('undefined' === typeof(evaluation)) { // doesn't take care of null value which is type Object
					evaluation = '';
				}
				else {
					if ((evaluation == null) || (evaluation == 'null') || (evaluation == 'undefined')) {
						evaluation = '';
					}
					else if (evaluation.toSerializeableDate) {
						evaluation = evaluation.toSerializeableDate().replace(' ', 'T');
					}
					else if (evaluation.toString) {
						evaluation = evaluation.toString();
					}
				}
				expression = expression.replace(token, evaluation);
			}
		}
		
		return expression;
	},

	// re-render the edit view.
	// this is NOT refresh/reload - but just re-evaluate all conditions server-side and render the form again
	// this is called from windowStack
	rerender: function(successCallback) { // a function to callback on when the operation is successful
		this.saveInstance(null, successCallback);
	},
	
	newInstance: function(newParams, // a map of parameter names to values used to seed the new instance - can be null or undefined
							formBinding, // the binding of the datagrid or lookup - can be null
							parentContext, // the parent web context - can be null
							openedFromDataGrid, // true if this view was opened from a data grid row
							successCallback) { // a function to callback on when the operation was successful
		this._openedFromDataGrid = openedFromDataGrid;
		if (this._vm.members) { // must be something that is data bound
			this.hide();
			this._b = formBinding;

			var me = this;

			var params = {_mod: this._mod, _doc: this._doc, _ecnt: this._ecnt, _ccnt: this._ccnt};
			if (formBinding) {
				params._b = formBinding;
			}
			if (parentContext) {
				params._c = parentContext;
			}
			if (newParams) {
				for (var binding in newParams) {
					var value = newParams[binding];
					params[binding] = value;
				}
			}

			this._vm.fetchData(
				null, // no criteria required
				function(dsResponse, // metadata about the returned data
							data, // the returned data
							dsRequest) { // the request that was sent
					if (dsResponse.status >= 0) { // success test
						// ensure that save operation is set to add (it would be edit)
						me._vm.setSaveOperationType("add");

						// scatter the first (and only) row returned from the server
						// data parameter is an array on fetch
						me.scatter(data[0]);

						if (openedFromDataGrid) {
							me._b += 'ElementById(' + data[0].bizId + ')';
						}
						
						if (successCallback) {
							successCallback(data);
						}
					}
					else if (dsResponse.status == -1) {
						isc.warn(data, null, {title: 'Problems'});
					}

					me.show();
					me.refreshListGrids(true);
				},
				{httpMethod: 'POST', params: params, willHandleError: true});
		}
	},
	
	editInstance: function(bizId, // the ID of the bean to edit
							formBinding, // the binding of the datagrid or lookup - can be null
							parentContext, // the parent context - can be null
							openedFromDataGrid, // true if this view was opened from a data grid row
							successCallback) { // a function to call back on when the operation is successful
		this._saved = false; // [Save] button has not been pressed yet - also any server side action has not been fired
		this._editInstance(null, bizId, formBinding, parentContext, openedFromDataGrid, successCallback);
	},

	_editInstance: function(action, // the action name associated with this edit call
							bizId, // the ID of the bean to edit
							formBinding, // the binding of the datagrid or lookup - can be null
							parentContext, // the parent context - can be null
							openedFromDataGrid, // true if this view was opened from a data grid row
							successCallback) { // a function to call back on when the operation is successful
		this._openedFromDataGrid = openedFromDataGrid;
		if (this._vm.members) { // must be something that is data bound
			this.hide();
			this._b = formBinding;

			var me = this;
			
			var params = {bizId: bizId, _mod: this._mod, _doc: this._doc, _ecnt: this._ecnt, _ccnt: this._ccnt};
			if (action) {
				params._a = action;
			}
			if (formBinding) {
				params._b = formBinding;
			}
			if (parentContext) {
				params._c = parentContext;
			}
			
			this._vm.fetchData(
				null, // no criteria required
				function(dsResponse, // metadata about the returned data
							data, // the returned data
							dsRequest) { // the request that was sent
					if (dsResponse.status >= 0) { // success test
						// scatter the first (and only) row returned from the server
						// data parameter is an array on fetch
						me.scatter(data[0]);

						if (openedFromDataGrid) {
							me._b += 'ElementById(' + data[0].bizId + ')';
						}

						if (successCallback) {
							successCallback(data);
						}
					}
					else if (dsResponse.status == -1) {
						isc.warn(data, null, {title: 'Problems'});
					}

					me.show();
					me.refreshListGrids(true);
				}, 
				{httpMethod: 'POST', params: params, willHandleError: true});
		}
	},

	// action - sent to server and use to determine whether to popoff the window on the window stack
	// successCallback - successCallback(instance) called on successful save
	saveInstance: function(action, // name of the action being performed
							successCallback) { // a function to call back on when the operation is successful
		var instance = this.gather(true); // validate
		if (instance) {
			// We get the web context out of the bean in the response and put it
			// as a post parameter in its own right.
			// The server needs to reconstruct the bean before it parses and applies the JSON.
			var context = instance._c;
			delete instance._c;

			var params = {_mod: this._mod, 
							_doc: this._doc,
							_ecnt: this._ecnt,
							_ccnt: this._ccnt,
							bean: instance,
							bizId: instance.bizId,
							_c: context};
			if (action) {
				params._a = action;
				if (action == '_PUSH') {
					action = null;
				}
			}
			if (this._b) {
				params._b = this._b;
			}
			
			var me = this;
			this._vm.saveData(
				function(dsResponse, // metadata about the returned data
							data, // the returned data
							dsRequest) { // the request that was sent
					if (dsResponse.status >= 0) { // redundant success test
						// if we came from a lookupDescription, this will be not null
						var lookupDescription = null;
						
						if (action == 'ZoomOut') {
							var opener = WindowStack.getOpener();
							var openerValues = opener.gather(false);
							// copy the context to the opener
							openerValues._c = data._c;
							
							// child binding
							var childBinding = me._b;
							// remove fully qualified binding (prefix)
							var index = childBinding.lastIndexOf('.');
							if (index >= 0) {
								childBinding = childBinding.substring(index + 1);
							}
							// remove the array map notation to leave just the array
							if (childBinding.endsWith(')')) { // array map notation
								var index = childBinding.lastIndexOf('ElementById');
								if (index >= 0) { // found
									childBinding = childBinding.substring(0, index);
								}
							}

							// parent binding
							var parentBinding = opener._b;
							// remove the array map notation to leave just the array
							if (parentBinding && parentBinding.endsWith(')')) { // array map notation
								var index = parentBinding.lastIndexOf('ElementById');
								if (index >= 0) { // found
									parentBinding = parentBinding.substring(0, index);
								}
							}

							// if we have zoomed in from a grid, remove the grid data from the openerValues
							// we just sent our new updates to the server anyway, and other values were posted
							// to the server from the opener on the zoom in navigation action.
							// Removing them makes sure that the old values still present in the opener aren't
							// sent and applied to the server-side context object.
							var openerValue = openerValues[childBinding];
							if (isc.isAn.Array(openerValue)) {
								// Can't use popoff(true) to rerender the opener view as this will post the original forms values
								// We need the potentially updated object values from the server to be displayed via a 
								// fetch operation through editInstance().
								WindowStack.popoff(false);
								opener._editInstance('ZoomOut', openerValues.bizId, parentBinding, openerValues._c, opener._openedFromDataGrid);
								return;
							}
							else {
								openerValues[childBinding] = data.bizId;
								lookupDescription = opener._vm.getItem(childBinding);
								if (lookupDescription) {
									if (instance.bizId) {
										lookupDescription.bizEdited(lookupDescription.form,
																		lookupDescription,
																		data.bizId);
									}
									else {
										lookupDescription.bizAdded(lookupDescription.form,
																	lookupDescription,
																	data.bizId);
									}
								}
							}
							
							// apply the values to the form
							opener._vm.setValues(openerValues);
							delete data._c;
							delete data._title;
						}

						if (action) {
							if (action == 'Save') {
								// scatter the first (and only) row returned from the server
								// data parameter is an object on save
								me._saved = true;
								me.scatter(data);
								me.refreshListGrids(true);
							}
							else {
								if (lookupDescription) {
									if (instance.bizId) {
										if (lookupDescription.bizEditedForServer) {
											WindowStack.popoff(false); // don't rerender the opener view
											lookupDescription.bizEditedForServer(lookupDescription.form,
																					lookupDescription,
																					data.bizId);
										}
										else {
											// Can't use popoff(true) to rerender the opener view as this will post the original forms values
											// We need the potentially updated object values from the server to be displayed via a 
											// fetch operation through editInstance().
											WindowStack.popoff(false);
											opener._editInstance('ZoomOut', openerValues.bizId, parentBinding, openerValues._c, opener._openedFromDataGrid);
										}
									}
									else {
										if (lookupDescription.bizAddedForServer) {
											WindowStack.popoff(false); // don't rerender the opener view
											lookupDescription.bizAddedForServer(lookupDescription.form,
																					lookupDescription,
																					data.bizId);
										}
										else {
											// Can't use popoff(true) to rerender the opener view as this will post the original forms values
											// We need the potentially updated object values from the server to be displayed via a 
											// fetch operation through editInstance().
											WindowStack.popoff(false);
											opener._editInstance('ZoomOut', openerValues.bizId, parentBinding, openerValues._c, opener._openedFromDataGrid);
										}
									}
								}
								else {
									WindowStack.popoff(true); // rerender the opener view
								}
							}
						}
						else { // no action
							me.scatter(data);
							me.refreshListGrids(true);
						}
						
						if (successCallback) {
							successCallback(data);
						}
					}
					else if (dsResponse.status == -1) {
						isc.warn(data, null, {title: 'Problems'});
					}
				}, 
				{params: params, willHandleError: true}
			);
		}
	},

	deleteInstance: function(successCallback) { // a function  to callback on when the operation is successful
		var instance = this.gather(true); // validate
		if (instance) {
			EditView._DATA_SOURCE.removeData(
				instance,
				function(dsResponse, // metadata about the returned data
							data, // the returned data
							dsRequest) { // the request that was sent
					if (dsResponse.status >= 0) { // redundant success test
						WindowStack.popoff(true); // rerender the opener view

						if (successCallback) {
							successCallback(data);
						}
					}
					else if (dsResponse.status == -1) {
						isc.warn(data, null, {title: 'Problems'});
					}
				},
				{params:{_mod: this._mod, _doc: this._doc, _c: instance._c}, willHandleError: true}
			);
		}
	},
	
	// doAction called from edit view with action argument
	// and from within datagrids server-side (ViewJSONManipulator) with action + grid arguments.
	doAction: function(action, // the name of the action to do
						validate, // should we validate on the client before executing the action
						gridBinding,  // if the action is on a grid, this is its binding (relative to the current view)
						gridModule, // if the action is on a grid, this is the grids driving module name.
						gridDocument, // if the action is on a grid, this is the grids driving document name.
						gridRowBizId, // if the action is on a grid, this is the grid row's bizId.
						successCallback) { // a function to callback on when the operation is successful
		var instance = this.gather(validate); // validate??
		if (instance) {
			// We get the web context out of the bean in the response and put it
			// as a post parameter in its own right.
			// The server needs to reconstruct the bean before it parses and applies the JSON.
			var context = instance._c;
			delete instance._c;

			var params = {_mod: (gridModule ? gridModule : this._mod), 
							_doc: (gridDocument ? gridDocument : this._doc),
							_ecnt: this._ecnt,
							_ccnt: this._ccnt,
							bean: instance,
							bizId: (gridRowBizId ? gridRowBizId : instance.bizId),
							_c: context};
			if (action) {
				params._a = action;
			}
			if (this._b) {
				params._b = this._b;
			}
			if (gridBinding) {
				params._g = gridBinding;
			}

			var me = this;
			// temporarily disable values manager validation as saveData() calls validate
			// which is not required on all actions {and was conditionally called above during gather() anyway}
			this._vm.disableValidation = true;
			this._vm.saveData(
				function(dsResponse, // metadata about the returned data
							data, // the returned data
							dsRequest) { // the request that was sent
					if (dsResponse.status >= 0) { // redundant success test
						// scatter the first (and only) row returned from the server
						// data parameter is an object on save
						me._saved = true;
						me.scatter(data);
						me.refreshListGrids(true);
						
						if (successCallback) {
							successCallback(data);
						}
					}
					else if (dsResponse.status == -1) {
						isc.warn(data, null, {title: 'Problems'});
					}
					
					return true;
				}, 
				{params: params, willHandleError: true}
			);
			this._vm.disableValidation = false; // reset to default immediately after call (not on callback)
		}
	},

	// method to place values into view controls from the instance
	scatter: function(values) { // values to scatter
		// clear error markings from the form
		this._vm.clearErrors(true);
		this._vm.clearValues();
		
		var link = '';
		if (values.persisted) {
			link = '&nbsp;<a target="_top" href="?m=' + this._mod + '&d=' + this._doc +
					'&i=' + values.bizId + '">Link</a>';
		}
		
		var header = BizUtil.headerTemplate;
		header = header.replace('{title}', values._title).replace('{link}', link);
		this._heading.setContents(header);

		// remove the form title so it is not subsequently posted
		delete values._title;

		// remove the value maps so they are not subsequently posted
		var valueMaps = values._valueMaps;
		delete values._valueMaps; // dont need these any more

		// scatter of the scalar values happens automatically
		// through this._vm.setValues(); which calls remember values also
		this._vm.setValues(values);
		// enable/disable, hide/show controls, invalidate caches etc
		this._processWidgets(this._editPanel, false, values, valueMaps);

		// scatter the list and membership values
		for (var gridBinding in this._grids) {
			var data = values[gridBinding];
			var grids = this._grids[gridBinding];
			for (var gridID in grids) {
				var grid = grids[gridID];
				if (data && isc.isAn.Array(data)) {
					if (grid._comparisonTree && grid._comparisonForm) { // we have a comparison widget
						grid.setData(data);
					}
					else if (grid._candidateList && grid._memberList) { // we have a list membership
						var valueMap = valueMaps[gridBinding];
						var candidates = [];
						for (var key in valueMap) {
							var value = valueMap[key];
							if (! data.containsProperty('bizId', key)) {
								candidates.push({bizId: key, bizKey: value});
							}
						}
						grid.setData(candidates, data);
					}
					else if (grid.grid) { // data grid
						var gridFields = grid.grid.fields;
						
						// for each field defined in the data grid
						for (var j = 0, k = gridFields.length; j < k; j++) {
							var gridField = gridFields[j];
							var type = gridField.type;
	
							// if a date or a time field
							// change date and time strings from the server to javascript dates
							var isDate = type.contains('YYYY');
							var isTime = type.contains('HH');
							if (isDate || isTime) {
								var name = gridField.name;
	
								// for each row of data for the data grid
								for (var i = 0, l = data.length; i < l; i++) {
									var row = data[i];
									var value = row[name];
									if (value) {
										if (isDate) {
											// NB - this handles Logical Dates (2000-01-01) and 
											//      ISO Dates (2001-01-01T00:00:00+00:00)
											row[name] = Date.parseSchemaDate(value);
										}
										else if (isTime) {
											row[name] = Time.parseInput(value);
										}
									}
								}
							}
							else if (gridField.valueMap) {
								// take care of variant domain values (they come up with each response)
								if (valueMaps) {
									var valueMap = valueMaps[gridBinding + '_' + gridField.name];
									if (valueMap) {
										gridField.valueMap = valueMap;
									}
								}
							}
						}
						grid.grid.setData(data);
					}
				}
			}
		}

		var onlyView = BizUtil.getCurrentView() == this;
		
		// enable/disable the actions on the form
		var members = this._actionPanel.getMembers();
		for (var i = 0, l = members.length; i < l; i++) {
			var tool = members[i];

			// don't show tools for create view when edit view is being shown
			// ensure that we hide these and don't enable/disable as we may not 
			// have the conditions required from the server (values)
			if (values.created && // object is created (as defined on the server)
					(tool.forCreate != null) && // this tool is not for both edit and create views
					tool.forCreate) { // only for create view
				this._showHide(tool, this._actionPanel, values, true);
				continue;
			}
			else if (values.notCreated && // object is not created (as defined on the server)
						(tool.forCreate != null) && // this tool is not for both edit and create views
						(! tool.forCreate)) { // nopt for create view
				this._showHide(tool, this._actionPanel, values, true);
				continue;
			}
			
			this._enableDisable(tool, this._actionPanel, values);

			// turn off certain buttons
			if (tool.actionName) {
				if (this._b) { // zoomed view
					this._showHide(tool, 
									this._actionPanel, 
									values,
									(tool.type == 'O') || // OK
										(tool.type == 'S') || // Save
										(tool.type == 'C') || // Cancel
										(tool.type == 'D') || // Delete
										// only allow the remove button if we came from a list
										((tool.type == 'R') && // Remove
											((this._openedFromDataGrid === undefined) ||
												(! this._openedFromDataGrid))));
				}
				else { // top view
					this._showHide(tool, 
									this._actionPanel, 
									values,
									(tool.type == 'Z') || // ZoomOut
									(tool.type == 'R') || // Remove
									// only allow delete button if the instance is persisted
									((! values['persisted']) && (tool.type == 'D')) ||
									// only allow delete button if not the only view
									(onlyView && (tool.type == 'D')) ||
									// only allow OK button if not the only view
									(onlyView && (tool.type == 'O')) ||
									// only allow Cancel button if not the only view
									(onlyView && (tool.type == 'C')));
				}
			}
		}
	},

	// if the listgrid is visible, it refreshes the grid
	refreshListGrids: function(forceRefresh) { // if true force refresh of all grids
		if (forceRefresh) {
			this._refreshedGrids = {};
		}
			
		// now that the values are set, we can reset all list grids - which have parameters
		for (var gridBinding in this._grids) {
			var grids = this._grids[gridBinding];
			for (var gridID in grids) {
				var grid = grids[gridID];
				if (this._refreshedGrids[gridID]) {} else {
					if (grid.isVisible()) { // only refresh component if it is visible
						if (grid._map) { // this is a map
							grid.resume();
							grid.rerender();
							this._refreshedGrids[gridID] = true;
						}
						else if (grid.dataSource) { // this is a list grid
							// Using refresh() instead of setDataSource() as setDataSource()
							// resets all fields and data sources on everything, essentially
							// recreating the listgrid guts.
							if (grid.hasDataSource()) {
								grid.refresh();
							}
							else {
								grid.setDataSource(grid.dataSource);
							}
							this._refreshedGrids[gridID] = true;
						}
					}
				}
			}
		}
	},
	
	// abstract method to extract values from view controls into the instance
	// if there are validation errors this function returns null
	gather: function(validate) { // whether to validate or not
		var result = null;
		
		if (validate) { // validation required
			if (this._vm.validate()) { // validate scalar values handled by values manager mechanism
				// TODO perform extra list validation here?
				// for each list, check the cardinality
				// check that mandatory fields are entered?  Can this be done when modal editing in the list?

				// if (error) {
				// 	isc.warn("Window Heading", "The message");
				// 	return;
				// }

				result = this._gather();
			}
		}
		else { // validation not required
			result = this._gather();
		}
		
		return result;
	},

	_gather: function() {
		// there is no need to gather the lists here as the grid controls 
		// manipulate the lists directly when scatter gives them the lists

		var result = this._vm.getValues(); // collect all scalar values

		// transform list membership values to an array of bizIds
		// transform comparison tree into an object hierarchy
		// reorder data in a data grid
		for (var gridBinding in this._grids) {
			var data = result[gridBinding];
			if (isc.isAn.Array(data) && (data.length > 0)) {
				if (data[0].properties) { // comparison widget
					var comparisons = this._grids[gridBinding];
					for (var comparisonID in comparisons) {
						result[gridBinding] = comparisons[comparisonID].getData();
					}
				}
				else {
					var grids = this._grids[gridBinding];
					for (var gridID in grids) {
						var grid = grids[gridID];
						var gridFields = [];
						var smartClientGrid = grid.grid; // grid property DNE if we are looking at a list membership
						if (smartClientGrid) {
							gridFields = smartClientGrid.fields;
							// Ensure the ordering is correct when DnD ordering is done by the user
							smartClientGrid.reorderData();
							data = data.sortByProperty(grid._ordinal, true);
						}
	
						if ((grid._candidateList && grid._memberList) || // we have a list membership OR
								// we have a data grid with a combo representing an aggregated reference
								((gridFields.length == 1) && 
									(gridFields[0].name == 'bizId') &&
									(gridFields[0].type == 'enum'))) {
							if (grid._ordinal) {
								// Reorder the data when DnD ordering is done by the user
								grid._memberList.reorderData();
								data = data.sortByProperty(grid._ordinal, true);
							}
							var members = [];
							result[gridBinding] = members;
							for (var i = 0, l = data.length; i < l; i++) {
								var item = data[i];
								members.push(item.bizId);
							}
						}
					}
				}
			}
		}
		
		return result;
	},
	
	_evaluateConditionName: function(conditionName, values) {
		var result = false;
		if (conditionName) {
			result = ((conditionName == "true") ? true : ((conditionName == "false") ? false : values[conditionName]));
		}
		
		return result;
	},
	
	_processWidgets: function(container, // a BizContainer
								invisible, // whether the current container is invisible or not
								values, // the VM values with the evaluated conditions
								valueMaps) { // the VM value maps
		for (var i = 0, l = container.contained.length; i < l; i++) {
			var contained = container.contained[i];
			
			this._enableDisable(contained, container, values);
			var containedInvisible = invisible || this._showHide(contained, container, values, false);

			if (isA.Function(contained.addContained)) {
				this._processWidgets(contained, containedInvisible, values, valueMaps);
			}
			if (isA.Function(contained.addBizTab)) { // tab pane
				// hold the current tab pane
				var selectedTab = contained.getSelectedTab();
				var selectedTabPane = selectedTab ? selectedTab.pane : null;
				
				// remove all tabs from the tab pane, ready for show/hide
				if (contained.tabs) {
					contained.removeTabs(contained.tabs);
				}

				// for each bizTab, show/hide and enable disable
				for (var j = 0, m = contained.bizTabs.length; j < m; j++) {
					var bizTab = contained.bizTabs[j];

					var tabInvisible = containedInvisible || this._showHide(bizTab, contained, values, false);
					this._enableDisable(bizTab, contained, values);

					// Determine if this should be the selected tab
					if (this._evaluateConditionName(bizTab.selectedConditionName, values)) {
						selectedTabPane = bizTab.pane;
					}
					
					this._processWidgets(bizTab.pane, tabInvisible, values, valueMaps);
				}
				
				// restore the current tab
				if (selectedTabPane) {
					selectedTab = contained.tabForPane(selectedTabPane);
					if (selectedTab) {
						contained.selectTab(selectedTab);
					}
				}
			}

			if (isA.Function(contained.getItems)) { // form
				// add/remove the form to/from the values manager if it is hidden/visible
				if (containedInvisible) {
					if (this._vm.members && this._vm.members.contains(contained)) { // is currently a member
						this._vm.removeMember(contained);
					}
				}
				else {
					if ((this._vm.members == null) || (! this._vm.members.contains(contained))) { // not already a member
						this._vm.addMember(contained);
					}
				}

				// process each form item
				var items = contained.items;
				for (var j = 0, m = items.length; j < m; j++) {
					var item = items[j];
					this._enableDisable(item, contained, values);
					if (! this._showHide(item, contained, values, false)) { // visible
						if (item.type == 'bizLookupDescription') {
							item.setValueMapFromEditView(values);
							item.invalidateCache();
						}
						else if ((item.type == 'select') || 
								(item.type == 'enum') ||
								(item.type == 'comboBox')) {
							// if this item has an option data source, set the value map
							if (item.optionDataSource) {
								// is a previous values
								if (item.optionDataSource == BizUtil.PREVIOUS_VALUES_DATA_SOURCE) {
									var valueMap = {};
									valueMap[values[item.name]] = values[item.name];
									item.setValueMap(valueMap);
								}
								else {
									item.fetchData();
									// set value map for the selected value, in case it is not 
									// in the option data source ResultSet yet
									var valueMap = {};
									valueMap[values[item.name]] = values[item.name + '_' + item.displayField];
									item.setValueMap(valueMap);
								}
							}
							else { // domain values must be in the bean
								if (valueMaps) {
									var valueMap = valueMaps[item.name];
									if (valueMap) {
										item.setValueMap(valueMap);
									}
								}
							}
						}
					}
				}
			}
			if (isA.Function(contained.rerender)) { // re-render (bound widgets and others that need a refresh)
				contained.rerender();
			}
		}
	},

	_enableDisable: function(widget, // the widget to enable or disable
								parent, // the parent container
								values) { // the values scattered
		if (widget.disablePickConditionName) {
			widget.canPick = ! this._evaluateConditionName(widget.disablePickConditionName, values);
			widget.enableDisablePick();
		}
		if (widget.disableAddConditionName) {
			widget.canAdd = ! this._evaluateConditionName(widget.disableAddConditionName, values);
		}
		if (widget.disableZoomConditionName) {
			widget.canZoom = ! this._evaluateConditionName(widget.disableZoomConditionName, values);
		}
		if (widget.disableEditConditionName) {
			widget.canEdit = ! this._evaluateConditionName(widget.disableEditConditionName, values);
		}
		if (widget.disableRemoveConditionName) {
			widget.canRemove = ! this._evaluateConditionName(widget.disableRemoveConditionName, values);
		}
		if (widget.disableClearConditionName) {
			widget.canClear = ! this._evaluateConditionName(widget.disableClearConditionName, values);
		}

		this._setDisabled(widget, parent, widget.disabledConditionName, values);
	},
	
	_setDisabled: function(widget, // the widget to enable or disable
							parent, // the parent container
							disabledConditionName, // the disabled condition to evaluate
							values) { // the values to evaluate with
		var disabled = this._evaluateConditionName(disabledConditionName, values);

		// make field optional/required if it is disabled/enabled
		// Visibility takes precedence over disability
		if (widget.bizRequired && widget.setRequired && widget.isVisible && widget.isVisible()) {
			widget.setRequired(! disabled);
		}

		if (widget.setDisabled) {
			widget.setDisabled(disabled);
		}
		else if (widget.enable && widget.disable) {
			if (disabled) {
				widget.disable();
			}
			else {
				widget.enable();
			}
		}
		else if (parent && parent.enableTab && parent.disableTab) {
			var tabIndex = parent.getTabNumber(widget);
			if (tabIndex >= 0) { // is shown
				if (disabled) {
					parent.disableTab(widget);
				}
				else {
					parent.enableTab(widget);
				}
			}
		}
	},
	
	// called from the view generator servlet
	setDisabled: function(binding, disabledConditionName) {
		var widget = this._vm.getItem(binding);
		if (widget) {
			var values = this.gather(false);
			this._setDisabled(widget, null, disabledConditionName, values);
		}
	},

	// called from the view generator servlet
	toggleDisabled: function(binding) {
		var widget = this._vm.getItem(binding);
		if (widget) {
			this._setDisabled(widget, null, widget.isDisabled() ? "false" : "true", null);
		}
	},
	
	// returns whether the widget is invisible or not
	_showHide: function(widget, // the widget to enable or disable
							parent, // the parent container
							values, // the values scattered
							forceInvisible) { // force this widget to be invisible - can be undefined
		return this._setInvisible(widget, parent, widget.invisibleConditionName, values, forceInvisible);
	},
	
	// returns whether the widget is invisible or not
	_setInvisible: function(widget, // the widget to enable or disable
								parent, // the parent container
								invisibleConditionName, // the invisible condition to evaluate
								values, // the values to evaluate with
								forceInvisible) { // force this widget to be invisible - can be undefined
		var invisible = forceInvisible || false; // forceInvisible could be undefined
		if (! invisible) {
			invisible = this._evaluateConditionName(invisibleConditionName, values);
		}

		// make field optional/required if it is hidden/displayed
		// Visibility takes precedence over disability
		if (widget.bizRequired && widget.setRequired) {
			if (invisible) {
				widget.setRequired(false);
			}
			else {
				if (widget.isDisabled && (! widget.isDisabled())) {
					widget.setRequired(true);
				}
			}
		}

		if (widget.show && widget.hide) {
			if (invisible) {
				widget.hide();
			}
			else {
				widget.show();
			}
		}
		else if (parent) {
			if (invisible) {
				parent.hideMember(widget);
			}
			else {
				parent.showMember(widget);
			}
		}
		
		return invisible;
	},
	
	// called from the view generator servlet
	setInvisible: function(binding, invisibleConditionName) {
		var widget = this._vm.getItem(binding);
		if (widget) {
			var values = this.gather(false);
			this._setInvisible(widget, null, invisibleConditionName, values);
		}
	}
});

// Action Renderer
// actions
// buttons

isc.ClassFactory.defineClass("BizButton", "IButton");
// actionName: null - the name of the server side action to call
// validate: undefined or true or false - whether to validate client-side before executing custom actions
// type: the type of the action represented as a char
// tooltip: null - the tooltip to display
// displayName: null - The display name
// icon: null - The icon to display
// confirm - The text to display as a confirmation for the action
// hasDisabledIcon: false - Has a disabled icon?
//
// _view: null - The view that this button belongs to
BizButton.addMethods({
	initWidget: function () {
		this.autoFit = ! (arguments[0].width || arguments[0].height);
		this.hasDisabledicon = false;
		if (this.displayName) {
			this.title = this.displayName;
		}
		
		if (this.tooltip) {
			this.canHover = true;
			this.getHoverHTML = function() {
				return this.tooltip;
			};
		}
		
		if (this.icon) {
			this.showDisabledIcon = this.hasDisabledIcon;
		}
		
		this.click = function() {
			if (this.confirm) {
				var me = this;
				isc.ask(this.confirm,
							function(value) {
								if (value) {
									me._click();
								}
							},
							{title: 'Confirm'}
				);
			}
			else {
				this._click();
			}
		},
		
		this._click = function() {
			// New and Edit are list view actions
			if (this.type == "O") { // OK on edit view
				this._view.saveInstance(this.actionName);
			}
			else if (this.type == "S") { // Save on edit view
				this._view.saveInstance(this.actionName);
			}
			else if (this.type == "A") { // Add on child edit view
			}
			else if (this.type == "Z") { // Change on child edit view
				this._view.saveInstance(this.actionName);
			}
			// TODO this is probably not required
			else if (this.type == "C") { // Cancel on edit view and child edit view
				var me = this;
				if (this._view._vm.valuesHaveChanged()) {
					isc.ask("There are unsaved changes.  Do you wish to cancel?",
							function(value) {
								if (value) {
									WindowStack.popoff(me._view._saved); // dont rerender the opener view unless save or an action was taken
								}
							},
							{title:'Discard Unsaved Changes?'}
					);
				}
				else {
					WindowStack.popoff(me._view._saved); // dont rerender the opener view unless save or an action was taken
				}
			}
			else if (this.type == "D") { // Delete on edit view
				var me = this;
				isc.ask("Do you want to delete this data?",
							function(value) {
								if (value) {
									me._view.deleteInstance();
								}
							},
							{title: 'Delete?'}
				);
			}
			else if (this.type == "R") { // Remove on child edit view
				var bizId = this._view.gather(false).bizId;

				// remove anything before and including the last dot (if present)
				// and after the last 'ElementById'
				var gridBinding = this._view._b;
				gridBinding = gridBinding.substring(gridBinding.lastIndexOf('.') + 1, 
														gridBinding.lastIndexOf('ElementById'));
				var opener = WindowStack.getOpener();
				if (opener) {
					var openerListGrids = opener._grids[gridBinding];
					if (openerListGrids) {
						for (var openerListGridID in openerListGrids) {
							var openerListGrid = openerListGrids[openerListGridID];
							openerListGrid.remove(bizId);
						}
					}
				}
				WindowStack.popoff(false); // dont rerender
			}
			else if (this.type == "P") { // Invoke a report
				ReportDialog.popupReport(this._view, this.params);
			}
			else if (this.type == "X") { // BizExport
				var me = this;
				// Popup on the mouse click event - to default popup blockers
				BizUtil.popupFrame('images/blank.gif', 'Export', 800, 630);
				me._view.rerender(function() {
					var instance = me._view.gather(false); // don't validate
					if (instance) {
						// window is already popped up, 
						// so popup wont block even though we're on the arse end of an XHR
						BizUtil.popupFrame('bizexport.xls?_n=' + me.actionName + 
													'&_doc=' + me._view._mod + '.' + me._view._doc + 
													'&_c=' + instance._c +
													'&_ctim=' + new Date().getTime(),
													'Export', 
													800, 
													630);
					}
				});
			}
			else if (this.type == "I") { // BizImport
				var instance = this._view.gather(false); // don't validate
				if (instance) {
					BizUtil.popupFrame('pages/upload/bizimport.jsp?_n=' + this.actionName + 
											'&_doc=' + this._view._mod + '.' + this._view._doc + 
											'&_c=' + instance._c +
											'&_ctim=' + new Date().getTime(),
										'Export', 
										800, 
										630);
				}
			}
			else if (this.type == "U") { // Upload Action
				var instance = this._view.gather(false); // don't validate - rerender() call will validate below
				if (instance) {
					var me = this;
					this._view.rerender(function() {
						var url = 'fileUpload.xhtml?_a=' + me.actionName + 
									'&_c=' + instance._c;
						if (me._view._b) {
							url += '&_b=' + me._view._b;
						}
						WindowStack.popup(null,
											"Upload",
											true,
											[isc.HTMLPane.create({
												contentsType: 'page',
												contents: 'Loading Page...',
												contentsURL: url
											})]);
					});
				}
			}
			else if (this.type == "M") { // Navigate to a binding within a conversation
			}
			else {
				var validate = this.validate;
				if (validate === undefined) {
					validate = true;
				}
				this._view.doAction(this.actionName, validate);
			}
		};

        this.Super("initWidget", arguments);
	}
});

// TODO dialog button

// Container renderer
isc.ClassFactory.defineClass("BizVBox", "VLayout");
// contained: [] - the contained widgets
// bizBorder: false - whether to display a border or not
// bizBorderTitle: null - the title of the border if any
// invisibleConditionName: null - the invisible condition 
BizVBox.addMethods({
	initWidget: function() {
		this.contained = [];
		if (this.bizBorder) {
			if (this.bizBorderTitle) {
				this.isGroup = true;
				this.groupTitle = this.bizBorderTitle;
			}
			else {
				this.border = "1px";
			}
		}
		this.Super("initWidget", arguments);
	},
	
	addContained: function(contained) {
		this.contained.add(contained);
		this.addMember(contained);
	}
});

// HBox
isc.ClassFactory.defineClass("BizHBox", "HLayout");
// contained: [] - the contained widgets
// bizBorder: false - whether to display a border or not
// bizBorderTitle: null - the title of the border if any
// invisibleConditionName: null - the invisble condition 
BizHBox.addMethods({
	initWidget: function() {
		this.contained = [];
		if (this.bizBorder) {
			if (this.bizBorderTitle) {
				this.isGroup = true;
				this.groupTitle = this.bizBorderTitle;
			}
			else {
				this.border = "1px";
			}
		}
		this.Super("initWidget", arguments);
	},
	
	addContained: function(contained) {
		this.contained.add(contained);
		this.addMember(contained);
	}
});

// TabPane/Tab - use the default TabSet/Tab
isc.ClassFactory.defineClass("BizTabPane", "TabSet");
// name,
// width
// height
// tabs - defaults to []
// bizTabs - the internal tab definitions, so we can show and hide tabs - defaults to []
BizTabPane.addMethods({
	initWidget: function() {
		this.tabs = [];
		this.bizTabs = [];
		this.destroyPanes = false; // dont destroy the tab panes when we remove them - show & hide
		this.tabBarThickness = 30; // height
		this.Super("initWidget", arguments);
	},
	
	tabSelected: function(tabNum, tabPane, ID, tab) {
		if (this._view.isVisible()) {
			this._view.delayCall('refreshListGrids', [false], 0);
		}
	},
	
	// use this instead of addTab() as it keeps track of the bizTabs
	addBizTab: function(bizTab) { // tab definition
		this.bizTabs.add(bizTab);
		this.addTab({title: bizTab.title,
						pane: bizTab.pane,
						disabledConditionName: bizTab.disabledConditionName,
						invisibleConditionName: bizTab.invisibleConditionName,
						selectedConditionName: bizTab.selectedConditionName});
	},
	
	showMember: function(tab) { // the bizTab to show
		this.addTab({title: tab.title,
						pane: tab.pane,
						disabledConditionName: tab.disabledConditionName,
						invisibleConditionName: tab.invisibleConditionName,
						selectedConditionName: tab.selectedConditionName});
	},
	
	hideMember: function(tab) { // the bizTab to hide
		if (this.getTabNumber(tab) >= 0) {
			this.removeTab(tab);
		}
	}
});

// Input renderer
// TextArea - built-in
// HTML - TODO make this editor happen
// TextField - built-in
// Lookup - TODO
// LookupDescription - defined in types.js

// Colour - built-in
// Combo - built-in

// Radio - TODO
// ListMembership
isc.ClassFactory.defineClass("BizListMembership", "HLayout");
// Properties
// 
// candidatesHeading: null, // title of candidate list
// membersHeading: null, // title of members list
// _view: null, // the containing view

BizListMembership.addMethods({
	initWidget : function (config) {
		this.membersMargin = 10;
		this.height = "100%"; // need height to vertically centre the arrows
		
		var me = this;
		
		this._candidateList = isc.ListGrid.create({
			width: "100%", 
			height: "100%",
			canDragRecordsOut: true,
			canAcceptDroppedRecords: true,
			dragDataAction: "move",
			recordDrop: function (dropRecords, targetRecord, index, sourceWidget) {
				this.Super('recordDrop', arguments);
				me.changed();
			},
			alternateRecordStyles: true,
			autoFetchData: false,
			leaveScrollbarGap: false,
			showHeaderContextMenu: false,
			fields: [
			    {name: 'bizKey', title: (this.candidatesHeading ? this.candidatesHeading : 'Candidates')}
	        ]
		});

		this._memberList = isc.ListGrid.create({
			width: "100%",
			height: "100%",
			canDragRecordsOut: true,
			canAcceptDroppedRecords: true,
			dragDataAction: "move",
			canReorderRecords: config._ordinal ? true : false,
			recordDrop: function (dropRecords, targetRecord, index, sourceWidget) {
				this.Super('recordDrop', arguments);
				if (sourceWidget == this) { // a reorder drop
					this.reorderData();
				}
				me.changed();
			},
			reorderData: function() {
				if (config._ordinal) { // this grid is orderable
					var data = this.getData();
					for (var i = 0, l = data.length; i < l; i++) {
						data[i][config._ordinal] = i + 1;
					}
				}
			},
			alternateRecordStyles: true,
			autoFetchData: false,
			leaveScrollbarGap: false,
			showHeaderContextMenu: false,
			fields: [
				{name: 'bizKey', title: (this.membersHeading ? this.membersHeading : 'Members')}
	        ]
		});

		this.members = [
			this._candidateList,
			isc.VLayout.create({
				layoutAlign: "center",
				membersMargin: 10,
				height: 100,
				members: [
					isc.IButton.create({
						icon: "icons/memberAssign.png",
						iconWidth: 24,
						iconHeight: 24,
						iconAlign: "center",
						showText: false, 
						width: 32, 
						height: 32, 
						click: function() {
							me._memberList.transferSelectedData(me._candidateList);
							me.changed();
						},
						canHover: true,
						getHoverHTML: function() {return "Add the selected candidates.";}
					}),
					isc.IButton.create({
						icon: "icons/memberUnassign.png",
						iconWidth: 24,
						iconHeight:24,
						iconAlign: "center", 
						showText: false,
						width: 32, 
						height: 32, 
						click: function() {
							me._candidateList.transferSelectedData(me._memberList);
							me.changed();
						},
						canHover: true,
						getHoverHTML: function() {return "Remove the selected members.";}
					})
				]
			}),
			this._memberList
		];
		
		this.Super("initWidget", arguments);
		
		var grids = this._view._grids[this._b];
		if (grids) {} else {
			grids = {};
			this._view._grids[this._b] = grids;
		}
		grids[this.getID()] = this;
	},
	
	setData: function(candidates, members) {
		this._candidateList.setData(candidates);
		this._memberList.setData(members);
	},
	
	// overridden during view generation if required
	changed: function() {}
});

// Comparison Editor
isc.ClassFactory.defineClass("BizComparison", "HLayout");
// Properties
// 
// _view: null, // the containing view

BizComparison.addMethods({
	initWidget : function () {
//		this.width = 100;
//		this.height = '100%';
		this.canDragResize = true;
		this.resizeFrom = ['L', 'R'];

		var me = this;

		this._comparisonTree = isc.TreeGrid.create({
			width: '50%',
			height: '100%',
		    fields: [{name: "bizKey", title: 'Document'},
		             {name: "relationship", title: 'Relationship'}],
		    data: isc.Tree.create({
		        modelType: "parent",
		        idField: "bizId",
		        parentIdField: "parent",
		        data: []
		    }),

		    selectionUpdated: function(record, recordList) {
		    	if (record) {
		        	me.setFormFields(record.properties);
		    	}
		    },

		    // customize appearance
		    showOpenIcons:false,
		    showDropIcons:false,
		    showResizeBar: true
		});

		this._comparisonForm = isc.PropertySheet.create({
			width: '100%',
			height: '100%',
			numCols: 4,
			colWidths: [150, '*', 50, '*'],
			border: '1px solid #A7ABB4',
			fields: []
		});

		this.members = [this._comparisonTree, isc.VLayout.create({overflow: 'auto', members: [this._comparisonForm]})];
		
		this.Super("initWidget", arguments);
		
		var grids = this._view._grids[this._b];
		if (grids) {} else {
			grids = {};
			this._view._grids[this._b] = grids;
		}
		grids[this.getID()] = this;
	},
	
	// the tree data
	setData: function(data) {
		this._comparisonTree.setData(isc.Tree.create({
	        modelType: "parent",
	        idField: "bizId",
	        parentIdField: "parent",
	        data: data}));
		// set form to first record - maybe we should just leave this
		this.setFormFields([]);
	},
	
	// the tree node properties
	setFormFields: function(properties) {
		var me = this;
		var fields = [
		              	{type: 'blurb', align: 'center', colSpan: 1, defaultValue: 'Property', startRow: false, endRow: false, cellStyle:"propSheetTitle"},
						{type: 'blurb', align: 'center', colSpan: 1, defaultValue: 'New', startRow: false, endRow: false, cellStyle:"propSheetTitle"},
						{title: "<<-",
							type: "button",
							// start from field 4 (field 0 - 3 are the header fields)
							// end at fields length - 5 (last 4 fields are spacers and the apply button) {4 + 1 (for last _old_ field)}
							click: "for (var i = 4; i < form.getFields().length - 5; i += 3) {var old = form.getField(i + 2).getValue(); form.getField(i).setValue(old ? old : '')}",
							startRow: false,
							endRow: false,
							align: 'center',
							cellStyle:"propSheetValue",
							titleStyle:null,
							textBoxStyle:null},
						{type: 'blurb', align: 'center', colSpan: 1, defaultValue: 'Old', startRow: false, endRow: false, cellStyle:"propSheetTitle"},
		];
					
		for (var i = 0; i < properties.length; i++) {
			var name = properties[i].name;
			var title = properties[i].title;
			var type = properties[i].type;
			var editorType = properties[i].editorType;
			var length = properties[i].length;
			var valueMap = properties[i].valueMap;
			var required = properties[i].required;
			var allowEmptyValue = properties[i].allowEmptyValue;
			
			var field = {name: name, title: title, type: type, width: '*', defaultValue: properties[i].newValue};
			var oldField = {name: "_old_" + name, type: type, width: '*', showTitle: false, canEdit: false, defaultValue: properties[i].oldValue};
			if (editorType) {
				field.editorType = editorType;
				oldField.editorType = editorType;
			}
			if (length) {
				field.length = length;
			}
			if (valueMap) {
				field.valueMap = valueMap;
				oldField.valueMap = valueMap;
			}
			if (required) {
				field.required = required;
			}
			if (allowEmptyValue) {
				field.allowEmptyValue = allowEmptyValue;
			}
			
			fields.add(field);
			
			fields.add({title: "<-",
	    		 type: "button",
	    		 click: "var old = form.getField('_old_" + name + "').getValue(); form.getField('" + name + "').setValue(old ? old : '')",
	    		 align: 'center',
	    		 startRow: false,
	    		 endRow: false,
	    		 cellStyle:"propSheetValue",
	    		 titleStyle:null,
	    		 textBoxStyle:null
			});
			
			fields.add(oldField);
		}
		
		fields.add({type: 'spacer', colSpan: 4, cellStyle: null, titleStyle: null, textBoxStyle: null});
		fields.add({type: 'spacer', colSpan: 3, cellStyle: null, titleStyle: null, textBoxStyle: null});
		fields.add({
			title: "Apply Changes",
			type: "button",
			startRow: false,
			endRow: false,
			align: 'center',
			cellStyle: null,
			titleStyle: null,
			textBoxStyle:null,
			click: function(form, item) {
				var values = form.getValues();
				var properties = me._comparisonTree.getSelectedRecord().properties;
				for (var i = 0; i < properties.length; i++) {
					var property = properties[i];
					property.newValue = values[property.name];
				}
				isc.showPrompt("Changes Applied");
				Timer.setTimeout("isc.clearPrompt()", 500);
			}
		});
		fields.add({type: 'spacer', colSpan: 4, cellStyle: null, titleStyle: null, textBoxStyle: null});

//alert(isc.JSON.encode(fields, {prettyPrint:false}));
		this._comparisonForm.setFields(fields);
		this._comparisonForm.clearValues();
	},
	
	// return the json data from the comparison editor
	getData: function() {
		var result = this._getData(this._comparisonTree.getData().getRoot().children[0]); // root node is a GAY string
		delete result._b;
		delete result._t;
		return result;
	},
	
	// recursively build the appropriate data structure from the TreeGrid tree node
	_getData: function(treeNode) {
		// create the result
		var result = {
			bizId: treeNode.bizId,
			_b: treeNode._b,
			_t: treeNode._t
		};

		// add all scalar properties to the result
		var properties = treeNode.properties;
		for (var i = 0; i < properties.length; i++) {
			result[properties[i].name] = properties[i].newValue;
		}
		
		// add all child nodes to the result
		var children = treeNode.children;
		if (children) {
			for (var i = 0; i < children.length; i++) {
				var childResult = this._getData(children[i]);
				var binding = childResult._b;
				var referenceType = childResult._t;
				delete childResult._b;
				delete childResult._t;
				
				// create an array if we are adding a second value to the same property
				var existing = result[binding];
				if (existing) {
					existing.add(childResult);
				}
				else {
					if (referenceType == 'A') {
						result[binding] = childResult;
					}
					else {
						result[binding] = [childResult];
					}
				}
			}
		}
		
		return result;
	}
});

// TODO CheckMembership
// CheckBox - built-in

// Tabular Renderer
// DataTable
// DataGrid - defined in grids.js
// PickView - TODO

// Widget Renderer
// Dynamic Image
isc.ClassFactory.defineClass("BizDynamicImage", "VLayout");
// Properties
// name: null, 	// name of image to display
// moduleDotDocument: null, // the module and document name
// imageWidth: null, // starting width of image
// imageHeight: null, // starting height of image
// width: null, // starting width of widget
// height: null, // starting height of widget
// parameters: null, // array of parameters for the image
	
//	_widthSlider: null,
//	_heightSlider: null,
//	_refreshButton: null,
//	_img: null
BizDynamicImage.addMethods({
	initWidget : function () {
		this.showEdges = true;
		this.edgeSize = 2;
		this.overflow = 'hidden';

		this.wZoom = 100;
		this.hZoom = 100;
		
		// TODO add parameters
		if (this.parameters) {
/* TODO find out how to add properties
			for (var i = 0, l = parameters.length; i < l; i++) {
				
			}
*/
		}
		this.members = [];

		var me = this;
		
		this.mouseWheelTimer = null;
		
		this.contextMenu = isc.Menu.create({
		    showShadow: true,
		    shadowDepth: 10,
		    data: [
	           {title:'Size', 
	        	   icon:'icons/mag.png',
	        	   submenu:[
    	            {title: '50%', click: function() {me._zoom(50);}},
    	            {title: '75%', click: function() {me._zoom(75);}},
    	            {title: '100%', click: function() {me._zoom(100);}},
    	            {title: '125%', click: function() {me._zoom(125);}},
    	            {title: '150%', click: function() {me._zoom(150);}},
    	            {title: '175%', click: function() {me._zoom(175);}},
    	            {title: '200%', click: function() {me._zoom(200);}},
    	            {title: '300%', click: function() {me._zoom(300);}},
    	            {title: '400%', click: function() {me._zoom(400);}},
    	            {title: '500%', click: function() {me._zoom(500);}}]},
	            {title: 'Enlarge', 
	            	icon:'icons/magIn.png',
	            	click: function() {
	            		me._zoom(me.wZoom + 10);
            		}
	            },
	            {title: 'Reduce', 
	            	icon:'icons/magOut.png',
	            	click: function() {
	            		me._zoom(me.wZoom - 10);
            		}
	            },
	            {isSeparator: true},
            	{title: 'Open', 
        			icon: 'zoom.gif',
        			click: function() {
        				me._open();
        			}
            	},
            	{isSeparator: true},
    	        {title: 'Refresh',
            		icon: 'refresh.png',
            		click: function() {
            			me.rerender();
        			}
	            }
            ]
		});
		
		this.Super("initWidget", arguments);
	},
	
	// throttle indicates whether to throttle zoom calls to the server to 1/2 second
	_zoom: function(zoomLevel, throttle) {
		if (zoomLevel > 0) {
			this.wZoom = zoomLevel;
			this.hZoom = zoomLevel;
			if (throttle) {
				if (this.mouseWheelTimer) {
					Timer.clear(this.mouseWheelTimer);
				}
				this.mouseWheelTimer = Timer.setTimeout(this.ID + '.rerender()', 250);
			}
			else {
				this.rerender();
			}
		}
	},
	
	_open: function() {
		var image = BizDynamicImage.create({name: this.name,
												moduleDotDocument: this.moduleDotDocument,
												format: this.format,
												_view: this._view});
		WindowStack.popup(null, "Image", true, [image]);
		image.rerender();
	},
	
	resized: function() {
		this.rerender();
	},
	
	rerender: function() {
		// this was a rerender called when there is no data in the form yet - no web context
		// this was probably called from the resized event callback and is spurious
		var c = this._view._vm.getValue('_c');
		if (c) {} else {
			return;
		}
		
		this.mouseWheelTimer = null;
		if (this.members && (this.members.length == 1)) {
			this.removeMember(0);
			if (this._img) {
				this._img.destroy();
			}
		}

		var b = this._view._b;
		var bizId = this._view._vm.getValue('bizId');

		var w = this.imageWidth ? this.imageWidth : (this.getVisibleWidth() - 20); // -20 for padding etc
		var h = this.imageHeight ? this.imageHeight : (this.getVisibleHeight() - 20); // -20 for padding etc
		
		var me = this;

		var src = "dynamic." + this.format + "?_doc=" + this.moduleDotDocument + "&_n=" + this.name;
		if (this.imageWidth) {
			src += "&_w=" + this.imageWidth; 
		}
		if (this.imageHeight) {
			src += "&_h=" + this.imageHeight;
		}
		src += "&_w=" + w + "&_h=" + h + "&_wz=" + this.wZoom + "&_hz=" + this.hZoom;
		if (c) {
			src += "&_c=" + c;
		}
		if (b) {
			src += "&_b=" + b;
		}
		if (bizId) {
			src += "&bizId=" + bizId;
		}
		src += "&_ts=" + new Date().getTime();
		
		this._img = isc.Img.create({
			width: '100%',
			height: '100%',
			overflow: 'hidden',
			imageWidth: Math.round(w * this.wZoom / 100.0),
			imageHeight: Math.round(h * this.hZoom / 100.0),
			imageType: 'center',
			canDrag: true,
			cursor: 'all-scroll',
			dragAppearance: 'none',
			dragStart: function () {
				this.startScrollLeft = this.getScrollLeft();
				this.startScrollTop = this.getScrollTop();
			},
			dragMove: function () {
				this.scrollTo(
				    this.startScrollLeft - isc.Event.lastEvent.x + isc.Event.mouseDownEvent.x,
				    this.startScrollTop - isc.Event.lastEvent.y + isc.Event.mouseDownEvent.y
				);
			},
			mouseWheel: function() {
				var wheelDelta = isc.EventHandler.getWheelDelta();
				me._zoom(Math.round(me.wZoom - (wheelDelta * 10.0)), true); // throttle this event
				return false;
			},
			doubleClick: function() {
				me._open();
			},
			src: src
		});
		if (this.members) {
			this.addMember(this._img);
		}
	}
});

// Image
isc.ClassFactory.defineClass("BizImage", "Img");
// Properties
// invisibleConditionName: null, // condition to evaluate
// file: null, // for specifying a static file to display (when downloadFrom is 'resources')
BizImage.addMethods({
	initWidget: function () {
		this.imageType = "stretch";
		this.src = "resources?_n=" + this.file + "&_doc=" + this.modoc + "&_b=null";

		this.Super("initWidget", arguments);
	}

	// no rerender required as the file is static - leave it to the browser caching
});

// Label
isc.ClassFactory.defineClass("BizLabel", "Label");
// Properties
// value: null, // literal value (forBinding evaluation or value literal)
// width, height: null, // pixel values
// invisibleConditionName: null, // condition to evaluate
// formatted: false
BizLabel.addMethods({
	initWidget : function () {
		this.autoFit = ! (arguments[0].width || arguments[0].height);
		if (this.value) {
			this.setContents(this.value);
		}
		if (this.textAlign) {
			this.setAlign(this.textAlign);
		}
		
        this.Super("initWidget", arguments);
	}
});
// ProgressBar
isc.ClassFactory.defineClass("BizProgressBar", "ProgressBar");
// Properties