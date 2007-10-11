/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */


/**
* Creates a control that allows the user to select items from a list, and
* places the selected items in another list.
* @constructor
* @class
* This class creates and manages a control that lets the user
* select items from a list. Two lists are maintained, one with items to select 
* from, and one that contains the selected items. Between them are buttons 
* to shuffle items back and forth between the two lists.
* <p>
* There are two types of buttons: one or more transfer buttons move items from
* the source list to the target list, and the remove button moves items from the
* target list to the source list. The client can specify its transfer buttons.
* If no specification is given, there will be a single transfer button called 
* "Add".</p>
* <p>
* The parent must implement search(columnItem, ascending) if column sorting
* is supported. It should also create a subclass of DwtChooser which returns
* the appropriate source and target list views, themselves subclasses of
* DwtChooserListView. Those subclasses must implement _getHeaderList() and
* _createItemHtml(item).</p>
* <p>
* There are two different layout styles, horizontal (with the list views at the
* left and right) and vertical (with the list views at the top and bottom). There
* are two different selection styles, single and multiple, which control how many
* items may appear in the target list view. There are two different transfer modes:
* one where items are copied between lists, and one where they're moved.</p>
*
* TODO: - column sorting
*
* @author Conrad Damon
*
* @param parent			[DwtComposite]		containing widget
* @param className		[string]*			CSS class
* @param slvClassName	[string]*			CSS class for source list view
* @param tlvClassName	[string]*			CSS class for target list view
* @param buttonInfo		[array]*			id/label pairs for transfer buttons
* @param layoutStyle	[constant]*			layout style (vertical or horizontal)
* @param selectStyle	[constant]*			multi-select (default) or single-select
* @param mode			[constant]*			items are moved or copied
* @param noDuplicates	[boolean]*			if true, prevent duplicates in target list
* @param singleHeight	[int]*				height of list view for single select style
* @param listSize		[int]*				list width (if HORIZ) or height (if VERT)
* @param sourceEmptyOk	[boolean]*			if true, don't show "No Results" in source list view
* @param allButtons		[boolean]*			if true, offer "Add All" and "Remove All" buttons
* @param hasTextField	[boolean]*			if true, create a text field for user input
*/
DwtChooser = function(params) {

	if (arguments.length == 0) return;
	DwtComposite.call(this, params.parent, params.className);

	this._slvClassName = params.slvClassName;
	this._tlvClassName = params.tlvClassName;
	this._layoutStyle = params.layoutStyle ? params.layoutStyle : DwtChooser.HORIZ_STYLE;
	this._selectStyle = params.selectStyle ? params.selectStyle : DwtChooser.MULTI_SELECT;
	this._mode = params.listStyle ? params.listStyle : DwtChooser.MODE_MOVE;
	this._noDuplicates = (params.noDuplicates !== false);
	this._singleHeight = params.singleHeight ? params.singleHeight : 45; // 45 = header row + row with icon
	this._listSize = params.listSize;
	this._sourceEmptyOk = params.sourceEmptyOk;
	this._allButtons = params.allButtons;
	this._hasTextField = params.hasTextField;

	this._handleButtonInfo(params.buttonInfo);
	this._mode = params.mode ? params.mode :
						this._hasMultiButtons ? DwtChooser.MODE_COPY : DwtChooser.MODE_MOVE;

	this._createHtml();
	this._initialize();
	
	var parentSz = params.parent.getSize();
	var listWidth = params.listWidth || parentSz.x;
	var listHeight = params.listHeight || parentSz.y;
	if (listWidth && listHeight) {
		this.resize(listWidth, listHeight);
	}

};

DwtChooser.prototype = new DwtComposite;
DwtChooser.prototype.constructor = DwtChooser;

// Consts

// layout style
DwtChooser.HORIZ_STYLE	= 1;
DwtChooser.VERT_STYLE	= 2;

// number of items target list can hold
DwtChooser.SINGLE_SELECT	= 1;
DwtChooser.MULTI_SELECT		= 2;

// what happens to source items during transfer
DwtChooser.MODE_COPY	= 1;
DwtChooser.MODE_MOVE	= 2;

DwtChooser.REMOVE_BTN_ID = "__remove__";
DwtChooser.ADD_ALL_BTN_ID = "__addAll__";
DwtChooser.REMOVE_ALL_BTN_ID = "__removeAll__";

DwtChooser.prototype.toString = 
function() {
	return "DwtChooser";
};

/**
* Populates the given list view with the given list. Defaults to source view.
*
* @param items			[object]		list of items (AjxVector, array, or single object), or hash of lists
* @param view			[constant]*		view to set (source or target)
* @param clearOtherView	[boolean]*		if true, clear out other view
*/
DwtChooser.prototype.setItems =
function(items, view, clearOtherView) {
	view = view ? view : DwtChooserListView.SOURCE;
	this._reset(view);
	this.addItems(items, view, true);
	this._selectFirst(view);
	if (clearOtherView) {
		this._reset((view == DwtChooserListView.SOURCE) ? DwtChooserListView.TARGET : DwtChooserListView.SOURCE);
	}
};	
	
/**
* Returns a copy of the items in the given list. If that's the target list and 
* there are multiple transfer buttons, then a hash with a vector for each one
* is returned. Otherwise, a single vector is returned. Defaults to target view.
*
* @param view	[constant]*		view to get items from (source or target)
*/
DwtChooser.prototype.getItems =
function(view) {
	view = view ? view : DwtChooserListView.TARGET;
	if (view == DwtChooserListView.SOURCE) {
		return this.sourceListView.getList().clone();
	} else {	
		if (this._hasMultiButtons) {
			var data = {};
			for (var i in this._data) {
				data[i] = this._data[i].clone();
			}
			return data;
		} else {
			return this._data[this._buttonInfo[0].id].clone();
		}
	}
};

/**
* Adds items to the given list view.
*
* @param items		[object]		list of items (AjxVector, array, or single object)
* @param view		[constant]		view to add to (source or target)
* @param skipNotify	[boolean]*		if true, don't notify listeners
* @param id			[string]*		button ID
*/
DwtChooser.prototype.addItems =
function(items, view, skipNotify, id) {
	view = view ? view : DwtChooserListView.SOURCE;
	var list = (items instanceof AjxVector) ? items.getArray() : (items instanceof Array) ? items : [items];
	if (view == DwtChooserListView.SOURCE) {
		for (var i = 0; i < list.length; i++) {
			this._addToSource(list[i], null, skipNotify);
		}
	} else {
		var data;
		if (this._selectStyle == DwtChooser.SINGLE_SELECT) {
			this.targetListView._resetList();
			list = (list.length > 0) ? [list[0]] : list;
		}
		for (var i = 0; i < list.length; i++) {
			this._addToTarget(list[i], id, skipNotify);
//			if (this._selectStyle == DwtChooser.SINGLE_SELECT) {
//				return;
//			}
		}
	}
	if (view == DwtChooserListView.SOURCE) {
		var list = this.sourceListView.getList();
		this._sourceSize = list ? list.size() : 0;
	}
};

/**
* Removes items from the given list view.
*
* @param list		[object]		list of items (AjxVector, array, or single object)
* @param view		[constant]		view to remove from (source or target)
* @param skipNotify	[boolean]*		if true, don't notify listeners
*/
DwtChooser.prototype.removeItems =
function(list, view, skipNotify) {
	list = (list instanceof AjxVector) ? list.getArray() : (list instanceof Array) ? list : [list];
	for (var i = 0; i < list.length; i++) {
		(view == DwtChooserListView.SOURCE) ? this._removeFromSource(list[i], skipNotify) : this._removeFromTarget(list[i], skipNotify);
	}
};

/*
* Moves or copies items from the source list to the target list, paying attention
* to current mode.
*
* @param list		[object]	list of items (AjxVector, array, or single object)
* @param id			[string]*	ID of the transfer button that was used
* @param skipNotify	[boolean]*	if true, don't notify listeners
*/
DwtChooser.prototype.transfer =
function(list, id, skipNotify) {
	id = id ? id : this._activeButtonId;
	this._setActiveButton(id);
	if (this._mode == DwtChooser.MODE_MOVE) {
		if (this._selectStyle == DwtChooser.SINGLE_SELECT) {
			var tlist = this.targetListView.getList();
			if (tlist && tlist.size()) {
				this.remove(tlist, true);
			}
		}
		this.removeItems(list, DwtChooserListView.SOURCE, true);
	}
	this.addItems(list, DwtChooserListView.TARGET, skipNotify);
	this.sourceListView.deselectAll();
};

/**
* Removes items from target list, paying attention to current mode. Also handles button state.
*
* @param list		[object]	list of items (AjxVector, array, or single object)
* @param skipNotify	[boolean]*	if true, don't notify listeners
*/
DwtChooser.prototype.remove =
function(list, skipNotify) {
	list = (list instanceof AjxVector) ? list.getArray() : (list instanceof Array) ? list : [list];
	if (this._mode == DwtChooser.MODE_MOVE) {
		for (var i = 0; i < list.length; i++) {
			var index = this._getInsertionIndex(this.sourceListView, list[i]);
			this.sourceListView.addItem(list[i], index, true);
		}
		this._sourceSize = list ? list.length : 0;
	}
	this.removeItems(list, DwtChooserListView.TARGET);
};

/**
* Sets the select style to the given style (single or multiple). Performs a resize
* in order to adjust the layout, and changes the label on the transfer button if it's
* the default one.
*
* @param style		[constant]		single or multiple select
* @param noResize	[boolean]*		if true, don't perform resize
*/
DwtChooser.prototype.setSelectStyle =
function(style, noResize) {
	if (style == this._selectStyle) return;
	
	this._selectStyle = style;
	if (this._defLabel) {
		var button = this._button[this._buttonInfo[0].id];
		button.setText((style == DwtChooser.SINGLE_SELECT) ? AjxMsg.select : AjxMsg.add);
	}
	if (!noResize) {
		var curSz = this.getSize();
		this.resize(curSz.x, curSz.y);
	}
	
	// "Add All" and "Remove All" buttons only shown if MULTI_SELECT
	if (this._allButtons) {
		this._addAllButton.setVisible(style == DwtChooser.MULTI_SELECT);
		this._removeAllButton.setVisible(style == DwtChooser.MULTI_SELECT);
		this._enableButtons();
	}

	// if we're going from multi to single, preserve only the first target item
	if (style == DwtChooser.SINGLE_SELECT) {
		var list = this.targetListView.getList();
		var a = list ? list.clone().getArray() : null;
		if (a && a.length) {
			this._reset(DwtChooserListView.TARGET);
			this.addItems(a[0], DwtChooserListView.TARGET, true);
			this.targetListView.deselectAll();
			if (a.length > 1 && this._mode == DwtChooser.MODE_MOVE) {
				this.addItems(a.slice(1), DwtChooserListView.SOURCE, true);
			}
			this._enableButtons();
		}
	}
	
	this.sourceListView.setMultiSelect(style == DwtChooser.MULTI_SELECT);
	this.targetListView.setMultiSelect(style == DwtChooser.MULTI_SELECT);
};

/**
* Resets one or both list views, and the buttons. Defaults to resetting both list views.
*
* @param view	[constant]		source or target list view
*/
DwtChooser.prototype.reset =
function(view) {
	this._reset(view);
	this._setActiveButton(this._buttonInfo[0].id); // make first button active by default
	this._enableButtons();
	if (this._hasTextField) {
		this._textField.setValue("");
	}
};

/*
* Resets one or both list views. Defaults to resetting both list views.
*
* @param view	[constant]		source or target list view
*/
DwtChooser.prototype._reset =
function(view) {
	// clear out source list view and related data
	if (!view || view == DwtChooserListView.SOURCE) {
		this.sourceListView._resetList();
	}

	// clear out target list view and related data
	if (!view || view == DwtChooserListView.TARGET) {
		this.targetListView._resetList();
		for (var i in this._data) {
			this._data[i].removeAll();
		}
	}
};

/**
* Adds a change listener.
*
* @param listener	[AjxListener]	a listener
*/
DwtChooser.prototype.addStateChangeListener = 
function(listener) {
	this.targetListView.addStateChangeListener(listener);
};

/**
* Removes the given change listener.
*
* @param listener	[AjxListener]	a listener
*/
DwtChooser.prototype.removeStateChangeListener = 
function(listener) {
	this.targetListView.removeStateChangeListener(listener);
};

/**
* Returns the DIV that contains the source list view.
*/
DwtChooser.prototype.getSourceListView = 
function() {
	return document.getElementById(this._sourceListViewDivId);
};

/**
* Returns the DIV that contains the buttons.
*/
DwtChooser.prototype.getButtons = 
function() {
	return document.getElementById(this._buttonsDivId);
};

/**
* Returns the DIV that contains the target list view.
*/
DwtChooser.prototype.getTargetListView = 
function() {
	return document.getElementById(this._targetListViewDivId);
};

DwtChooser.prototype.getTextField =
function() {
	return this._textField;
};

/*
* Creates the HTML framework, with placeholders for elements which are created
* later.
*/
DwtChooser.prototype._createHtml = 
function() {

	this._sourceListViewDivId	= Dwt.getNextId();
	this._targetListViewDivId	= Dwt.getNextId();
	this._buttonsDivId			= Dwt.getNextId();
	this._removeButtonDivId		= Dwt.getNextId();
	if (this._allButtons) {
		this._addAllButtonDivId		= Dwt.getNextId();
		this._removeAllButtonDivId	= Dwt.getNextId();
	}
	if (this._hasTextField) {
		this._textFieldTdId = Dwt.getNextId();
	}

	var html = [];
	var idx = 0;
	
	if (this._layoutStyle == DwtChooser.HORIZ_STYLE) {
		// start new table for list views
		html[idx++] = "<table cellspacing=0 cellpadding=0 border=0>";
		html[idx++] = "<tr>";

		// source list
		html[idx++] = "<td id='";
		html[idx++] = this._sourceListViewDivId;
		html[idx++] = "'></td>";

		// transfer buttons
		html[idx++] = "<td valign='middle' id='";
		html[idx++] = this._buttonsDivId;
		html[idx++] = "'>";
		if (this._allButtons) {
			html[idx++] = "<div id='";
			html[idx++] = this._addAllButtonDivId;
			html[idx++] = "'></div><br>";
		}
		for (var i = 0; i < this._buttonInfo.length; i++) {
			var id = this._buttonInfo[i].id;
			html[idx++] = "<div id='";
			html[idx++] = this._buttonDivId[id];
			html[idx++] = "'></div><br>";
		}
		// remove button
		html[idx++] = "<br><div id='";
		html[idx++] = this._removeButtonDivId;
		html[idx++] = "'></div>";
		if (this._allButtons) {
			html[idx++] = "<br><div id='";
			html[idx++] = this._removeAllButtonDivId;
			html[idx++] = "'></div><br>";
		}
		html[idx++] = "</td>";

		// target list
		html[idx++] = "<td id='";
		html[idx++] = this._targetListViewDivId;
		html[idx++] = "'></td>";

		html[idx++] = "</tr>";
		
		if (this._hasTextField) {
			html[idx++] = "<tr><td>";
			html[idx++] = "<table border=0 width=100%><tr><td style='white-space:nowrap; width:1%'>";
			html[idx++] = AjxMsg.add;
			html[idx++] = ":</td><td id='";
			html[idx++] = this._textFieldTdId;
			html[idx++] = "'></td></tr></table>";
			html[idx++] = "</td><td>&nbsp;</td><td>&nbsp;</td></tr>";
		}

		html[idx++] = "</table>";
	} else {
		// source list
		html[idx++] = "<div id='";
		html[idx++] = this._sourceListViewDivId;
		html[idx++] = "'></div>";

		// transfer buttons
		html[idx++] = "<div align='center' id='";
		html[idx++] = this._buttonsDivId;
		html[idx++] = "'>";
		html[idx++] = "<table><tr>";
		if (this._allButtons) {
			html[idx++] = "<td id='";
			html[idx++] = this._addAllButtonDivId;
			html[idx++] = "'></td>";
		}
		for (var i = 0; i < this._buttonInfo.length; i++) {
			var id = this._buttonInfo[i].id;
			html[idx++] = "<td id='";
			html[idx++] = this._buttonDivId[id];
			html[idx++] = "'></td>";
		}
		// remove button
		html[idx++] = "<td id='";
		html[idx++] = this._removeButtonDivId;
		html[idx++] = "'></td>";
		if (this._allButtons) {
			html[idx++] = "<td id='";
			html[idx++] = this._removeAllButtonDivId;
			html[idx++] = "'></td>";
		}
		html[idx++] = "</tr></table></div>";

		// target list
		html[idx++] = "<div id='";
		html[idx++] = this._targetListViewDivId;
		html[idx++] = "'></div>";
	}

	this.getHtmlElement().innerHTML = html.join("");
};

/*
* Takes button info and sets up various bits of internal data for later use.
*/
DwtChooser.prototype._handleButtonInfo =
function(buttonInfo) {

	if (!buttonInfo) {
		this._defLabel = (this._selectStyle == DwtChooser.SINGLE_SELECT) ? AjxMsg.select : AjxMsg.add;
		buttonInfo = [ { label: this._defLabel } ];
	}
	this._buttonInfo = buttonInfo;

	// create IDs for button elements and their containers
	this._buttonDivId = {};
	this._buttonId = {};
	if (this._buttonInfo.length == 1) {
		if (!this._buttonInfo[0].id) {
			this._buttonInfo[0].id = Dwt.getNextId();
		}
		this._activeButtonId = this._buttonInfo[0].id;
	}
	for (var i = 0; i < this._buttonInfo.length; i++) {
		var id = this._buttonInfo[i].id;
		this._buttonDivId[id] = Dwt.getNextId();
		this._buttonId[id] = Dwt.getNextId();
	}
	this._hasMultiButtons = (this._buttonInfo.length > 1);
};

/*
* Creates and places elements into the DOM.
*/
DwtChooser.prototype._initialize =
function() {

	// create and add transfer buttons
	var buttonListener = new AjxListener(this, this._transferButtonListener);
	this._button = {};
	this._buttonIndex = {};
	this._data = {};
	for (var i = 0; i < this._buttonInfo.length; i++) {
		var id = this._buttonInfo[i].id;
		this._button[id] = this._setupButton(id, this._buttonId[id], this._buttonDivId[id], this._buttonInfo[i].label);
		this._button[id].addSelectionListener(buttonListener);
		this._buttonIndex[id] = i;
		this._data[id] = new AjxVector();
	}

	// create and add source list view
	this.sourceListView = this._createSourceListView();
	this._addListView(this.sourceListView, this._sourceListViewDivId);
	this.sourceListView.addSelectionListener(new AjxListener(this, this._sourceListener));

	// create and add target list view
	this.targetListView = this._createTargetListView();
	this._addListView(this.targetListView, this._targetListViewDivId);
	this.targetListView.addSelectionListener(new AjxListener(this, this._targetListener));

	// create and add the remove button
	this._removeButtonId = Dwt.getNextId();
	this._removeButton = this._setupButton(DwtChooser.REMOVE_BTN_ID, this._removeButtonId, this._removeButtonDivId, AjxMsg.remove);
	this._removeButton.addSelectionListener(new AjxListener(this, this._removeButtonListener));

	if (this._allButtons) {
		// create and add "Add All" and "Remove All" buttons
		this._addAllButtonId = Dwt.getNextId();
		this._addAllButton = this._setupButton(DwtChooser.ADD_ALL_BTN_ID, this._addAllButtonId, this._addAllButtonDivId, AjxMsg.addAll);
		this._addAllButton.addSelectionListener(new AjxListener(this, this._addAllButtonListener));
		this._removeAllButtonId = Dwt.getNextId();
		this._removeAllButton = this._setupButton(DwtChooser.REMOVE_ALL_BTN_ID, this._removeAllButtonId, this._removeAllButtonDivId, AjxMsg.removeAll);
		this._removeAllButton.addSelectionListener(new AjxListener(this, this._removeAllButtonListener));
		if (this._selectStyle == DwtChooser.SINGLE_SELECT) {
			this._addAllButton.setVisible(false);
			this._removeAllButton.setVisible(false);
		}
	}

	if (this._hasTextField) {
		var params = {parent: this, type: DwtInputField.STRING};
		this._textField = new DwtInputField(params);
		this._textField.reparentHtmlElement(this._textFieldTdId);
		this._textField.getInputElement()._chooserId = AjxCore.assignId(this);
		this._textField.setHandler(DwtEvent.ONKEYUP, DwtChooser._onKeyUp);
		Dwt.setSize(this._textField.getInputElement(), "100%", Dwt.DEFAULT);
	}

	if (this._selectStyle == DwtChooser.SINGLE_SELECT) {
		this.sourceListView.setMultiSelect(false);
		this.targetListView.setMultiSelect(false);
	}
};

/*
* Returns a source list view object.
*/
DwtChooser.prototype._createSourceListView =
function() {
	return new DwtChooserListView(this, DwtChooserListView.SOURCE, this._slvClassName);
};

/*
* Returns a target list view object.
*/
DwtChooser.prototype._createTargetListView =
function() {
	return new DwtChooserListView(this, DwtChooserListView.TARGET, this._tlvClassName);
};

/*
* Adds a list view into the DOM and sets its size to fit in its container.
*
* @param listView		[DwtChooserListView]	the list view
* @param listViewDivId	[string]				ID of container DIV
*/
DwtChooser.prototype._addListView =
function(listView, listViewDivId) {
	var listDiv = document.getElementById(listViewDivId);
 	listDiv.appendChild(listView.getHtmlElement());
	listView.setUI(null, true); // renders headers and empty list
	listView._initialized = true;
};

/**
* Sizes the list views based on the given available width and height.
*
* @param width	[int]	width in pixels
* @param height	[int]	height in pixels
*/
DwtChooser.prototype.resize =
function(width, height) {
	if (!width || !height) return;
	if (width == Dwt.DEFAULT && height == Dwt.DEFAULT) return;

	var buttonsDiv = document.getElementById(this._buttonsDivId);
	var btnSz = Dwt.getSize(buttonsDiv);
	var w, sh, th;
	if (this._layoutStyle == DwtChooser.HORIZ_STYLE) {
		w = this._listSize ? this._listSize : (width == Dwt.DEFAULT) ? width : Math.floor(((width - btnSz.x) / 2) - 12);
		sh = th = height;
	} else {
		w = width;
		if (this._selectStyle == DwtChooser.SINGLE_SELECT) {
			sh = this._listSize ? this._listSize : (height == Dwt.DEFAULT) ? height : height - btnSz.y - this._singleHeight - 30;
			th = (height == Dwt.DEFAULT) ? height : height - btnSz.y - sh - 30;
		} else {
			sh = th = this._listSize ? this._listSize : (height == Dwt.DEFAULT) ? height : Math.floor(((height - btnSz.y) / 2) - 12);
		}
	}
	this.sourceListView.setSize(w, sh);
	this.targetListView.setSize(w, th);
};

/*
* Creates a transfer or remove button.
*
* @param id					[string]	button ID
* @param buttonId			[string]	ID of button element
* @param buttonDivId		[string]	ID of DIV that contains button
* @param label				[string]	button text
*/
DwtChooser.prototype._setupButton =
function(id, buttonId, buttonDivId, label) {
	var button = new DwtButton(this);
	button.setText(label);
	button.id = buttonId;
	button.setHtmlElementId(buttonId);
	button._buttonId = id;

	var buttonDiv = document.getElementById(buttonDivId);
	buttonDiv.appendChild(button.getHtmlElement());

	return button;
};

// Listeners

/*
* Single-click selects an item, double-click adds selected items to target list.
*
* @param ev		[DwtEvent]		click event
*/
DwtChooser.prototype._sourceListener =
function(ev) {
	if (ev.detail == DwtListView.ITEM_DBL_CLICKED) {
		// double-click performs transfer
		this.transfer(this.sourceListView.getSelection(), this._activeButtonId);
		this.sourceListView.deselectAll();
	} else if (this._activeButtonId == DwtChooser.REMOVE_BTN_ID) {
		// single-click activates appropriate transfer button if needed
		var id = this._lastActiveTransferButtonId ? this._lastActiveTransferButtonId : this._buttonInfo[0].id;
		this._setActiveButton(id);
	}
	this.targetListView.deselectAll();
	this._enableButtons();
};

/*
* Single-click selects an item, double-click removes it from the target list.
*
* @param ev		[DwtEvent]		click event
*/
DwtChooser.prototype._targetListener =
function(ev) {
	if (ev.detail == DwtListView.ITEM_DBL_CLICKED) {
		this.remove(this.targetListView.getSelection());
	} else {
		this._setActiveButton(DwtChooser.REMOVE_BTN_ID);
		this.sourceListView.deselectAll();
		this._enableButtons();
	}
};

/*
* Clicking a transfer button moves selected items to the target list.
*
* @param ev		[DwtEvent]		click event
*/
DwtChooser.prototype._transferButtonListener =
function(ev) {
	var button = DwtUiEvent.getDwtObjFromEvent(ev);
	var id = button._buttonId;
	var sel = this.sourceListView.getSelection();
	if (sel && sel.length) {
		this.transfer(sel, id);
		var list = this.sourceListView.getList();
		if (list && list.size()) {
			this._selectFirst(DwtChooserListView.SOURCE);
		} else {
			this._enableButtons();
		}
	} else {
		var email = this._getEmailFromText();
		if (email) {
			this.transfer([email], id);
		} else {
			this._setActiveButton(id);
		}
	}
};

/*
* Clicking the remove button removes selected items from the target list.
*
* @param ev		[DwtEvent]		click event
*/
DwtChooser.prototype._removeButtonListener =
function(ev) {
	this.remove(this.targetListView.getSelection());
	var list = this.targetListView.getList();
	if (list && list.size()) {
		this._selectFirst(DwtChooserListView.TARGET);
	} else {
		this._enableButtons();
	}
};

/*
* Populates the target list with all items.
*
* @param ev		[DwtEvent]		click event
*/
DwtChooser.prototype._addAllButtonListener =
function(ev) {
	this.transfer(this.sourceListView.getList().clone());
	this._selectFirst(DwtChooserListView.TARGET);
};

/*
* Clears the target list.
*
* @param ev		[DwtEvent]		click event
*/
DwtChooser.prototype._removeAllButtonListener =
function(ev) {
	this.remove(this.targetListView.getList().clone());
	this._selectFirst(DwtChooserListView.SOURCE);
};



// Miscellaneous methods

/*
* Enable/disable buttons as appropriate.
*/
DwtChooser.prototype._enableButtons =
function(sForce, tForce) {
	var sourceList = this.sourceListView.getList();
	var targetList = this.targetListView.getList();
	var sourceEnabled = (sForce || (this.sourceListView.getSelectionCount() > 0));
	for (var i = 0; i < this._buttonInfo.length; i++) {
		var id = this._buttonInfo[i].id;
		this._button[id].setEnabled(sourceEnabled);
	}
	var targetEnabled = (tForce || (this.targetListView.getSelectionCount() > 0));
	this._removeButton.setEnabled(targetEnabled);

	if (this._allButtons && (this._selectStyle == DwtChooser.MULTI_SELECT)) {
		var sourceSize = sourceList ? sourceList.size() : 0;
		var targetSize = targetList ? targetList.size() : 0;
		this._addAllButton.setEnabled(sourceSize > 0);
		this._removeAllButton.setEnabled(targetSize > 0);
	}
};

/*
* Selects the first item in the given list view.
*
* @param view	[constant]		source or target
*/
DwtChooser.prototype._selectFirst =
function(view, index) {
	var listView = (view == DwtChooserListView.SOURCE) ? this.sourceListView : this.targetListView;
	var list = listView.getList();
	if (list && list.size() > 0) {
		listView.setSelection(list.get(0));
	}
};

/*
* Makes a button "active" (the default for double-clicks). Done by
* manipulating the style class. The active/non-active class is set as the
* "_origClassName" so that activation/triggering still work. This only
* applies if there are multiple transfer buttons.
*
* @param id		[string]	ID of button to make active
*/
DwtChooser.prototype._setActiveButton =
function(id) {
	if (!this._hasMultiButtons) {
		return;
	}
	if (id != this._activeButtonId) {
		var buttonId = (this._activeButtonId == DwtChooser.REMOVE_BTN_ID) ? this._removeButtonId : this._buttonId[this._activeButtonId];
		if (buttonId) {
			var oldButton = Dwt.getObjectFromElement(document.getElementById(buttonId));
			if (oldButton) {
				oldButton.setDisplayState(DwtControl.NORMAL);
			}
		}
		buttonId = (id == DwtChooser.REMOVE_BTN_ID) ? this._removeButtonId : this._buttonId[id];
		var button = Dwt.getObjectFromElement(document.getElementById(buttonId));
		if (button) {
			button.setDisplayState(DwtControl.DEFAULT);
		}
		this._activeButtonId = id;
		if (id != DwtChooser.REMOVE_BTN_ID) {
			this._lastActiveTransferButtonId = id;
		}
	}
};

/*
* Returns true if the list contains the item. Default implementation is identity.
*
* @param item	[object]			item
* @param list	[AjxVector]			list to check against
*/
DwtChooser.prototype._isDuplicate =
function(item, list) {
	return list.contains(item);
};

/*
* Adds an item to the end of the source list.
*
* @param item		[object]	item to add
* @param skipNotify	[boolean]*	if true, don't notify listeners
*/
DwtChooser.prototype._addToSource =
function(item, index, skipNotify) {
	if (!item) return;
	if (!item._chooserIndex) {
		var list = this.sourceListView.getList();
		item._chooserIndex = list ? list.size() + 1 : 1;
	}
	this.sourceListView.addItem(item, index, skipNotify);
};

/*
* Adds an item to the target list. If there are multiple transfer buttons, it keeps
* the items grouped depending on which button was used to move them.
*
* @param item		[object]	item to add
* @param id			[string]	ID of the transfer button that was used
* @param skipNotify	[boolean]*	if true, don't notify listeners
*/
DwtChooser.prototype._addToTarget =
function(item, id, skipNotify) {
	if (!item) return;
	id = id ? id : this._activeButtonId;
	if (this._noDuplicates && this._data[id] && this._isDuplicate(item, this._data[id])) {
		return;
	}

	// item is being added to target list with multiple transfer buttons,
	// so we need to clone it on second and subsequent transfers
	var list = this.targetListView.getList();
	if (list && list.contains(item) && item.clone) {
		var newItem = item.clone();
		newItem.id = Dwt.getNextId();
		item = newItem;
	}

	var idx = null;
	if (this._hasMultiButtons) {
		// get a list of all the items in order
		var list = [];
		for (var i = 0; i < this._buttonInfo.length; i++) {
			list = list.concat(this._data[this._buttonInfo[i].id].getArray());
		}
		// find the first item with a higher button index
		var buttonIdx = this._buttonIndex[id];
		for (idx = 0; idx < list.length; idx++) {
			var testButtonIdx = this._buttonIndex[list[idx]._buttonId];
			if (testButtonIdx > buttonIdx) {
				break;
			}
		}
	}

	item._buttonId = id;
	this._data[id].add(item);
	if (!item.id) item.id = Dwt.getNextId();
	this.targetListView.addItem(item, idx, skipNotify);
};

/*
* Removes an item from the source list.
*
* @param item		[object]	item to remove
* @param skipNotify	[boolean]*	if true, don't notify listeners
*/
DwtChooser.prototype._removeFromSource =
function(item, skipNotify) {
	if (!item) return;
	var list = this.sourceListView.getList();
	if (!list) return;
	if (!list.contains(item)) return;

	this.sourceListView.removeItem(item, skipNotify);
};

/*
* Removes an item from the target list.
*
* @param item		[object]	item to remove
* @param skipNotify	[boolean]*	if true, don't notify listeners
*/
DwtChooser.prototype._removeFromTarget =
function(item, skipNotify) {
	if (!item) return;
	var list = this.targetListView.getList();
	if (!list) return;
	if (!list.contains(item)) return;

	this._data[item._buttonId].remove(item);
	this.targetListView.removeItem(item, skipNotify);
};

DwtChooser.prototype._getInsertionIndex =
function(view, item) {
	var list = view.getList();
	if (!list) return null;
	var a = list.getArray();
	for (var i = 0; i < a.length; i++) {
		if (item._chooserIndex && a[i]._chooserIndex && (a[i]._chooserIndex >= item._chooserIndex)) {
			return i;
		}
	}
	return null;
};

DwtChooser.prototype._getEmailFromText =
function() {
	var text = this._textField.getValue();
	var email = AjxEmailAddress.parse(text);
	if (email) {
		email.id = Dwt.getNextId();
		return email;
	}
};

DwtChooser._onKeyUp =
function(ev) {
	var el = DwtUiEvent.getTarget(ev);
	var chooser = AjxCore.objectWithId(el._chooserId);
	var key = DwtKeyEvent.getCharCode(ev);
	if (key == 3 || key == 13) {
		var email = chooser._getEmailFromText();
		if (email) {
			chooser.transfer([email], chooser._activeButtonId);
			el.value = "";
		}
	}
	chooser._enableButtons(el.value.length);
};

/**
* Creates a chooser list view.
* @constructor
* @class
* This base class represents a list view which contains items that can be transferred from it
* (source) or to it (target). Subclasses should implement  _getHeaderList(),
* _sortColumn(), and _createItemHtml().
*
* @param parent			[DwtComposite]	containing widget
* @param type			[constant]		source or target
* @param className		[string]*		CSS class
*/
DwtChooserListView = function(parent, type, className) {
	
	if (arguments.length == 0) return;
	className = className ? className : "DwtChooserListView";
	DwtListView.call(this, parent, className, null, this._getHeaderList(parent));

	this.type = type;
	this._chooserParent = parent.parent;
};

DwtChooserListView.SOURCE = 1;
DwtChooserListView.TARGET = 2;

DwtChooserListView.prototype = new DwtListView;
DwtChooserListView.prototype.constructor = DwtChooserListView;

DwtChooserListView.prototype._getHeaderList = function() {};

DwtChooserListView.prototype.toString = 
function() {
	return "DwtChooserListView";
};

/*
* Override to handle empty results set. Always omit the "No Results" message if
* this is a target list view, or if we've been told to ignore it in the source view.
*/
DwtChooserListView.prototype.setUI =
function(defaultColumnSort, noResultsOk) {
	noResultsOk = noResultsOk ? noResultsOk : ((this.type == DwtChooserListView.TARGET) ||
												this.parent._sourceEmptyOk);
	DwtListView.prototype.setUI.call(this, defaultColumnSort, noResultsOk);
};

/*
* DwtListView override to ignore right-clicks in list view.
*
* @param clickedEl		[element]	element that was clicked
* @param ev				[DwtEvent]	click event
*/
DwtChooserListView.prototype._itemClicked = 
function(clickedEl, ev) {
	// Ignore right-clicks, we don't support action menus
	if (!ev.shiftKey && !ev.ctrlKey && ev.button == DwtMouseEvent.RIGHT) {
		return;
	} else {
		DwtListView.prototype._itemClicked.call(this, clickedEl, ev);
	}
};

/*
* Called when a column header has been clicked.
*
* @param columnItem		[string]	ID for column that was clicked
* @param ascending		[boolean]	if true, sort in ascending order
*/
DwtChooserListView.prototype._sortColumn = 
function(columnItem, ascending) {
	this._chooserParent.search(columnItem, ascending);
};
