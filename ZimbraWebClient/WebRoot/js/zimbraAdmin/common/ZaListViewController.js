/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Web Client
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
* @class ZaListViewController base class for all Za***ListControllers (for list views)
* @extends ZaController
* @contructor 
* @param appCtxt
* @param container
* @param app
* @param iKeyName
* @author Greg Solovyev
* @see ZaAccountListController
* @see ZDomainListController
**/
function ZaListViewController(appCtxt, container, app, iKeyName) {
	if (arguments.length == 0) return;
	this._currentPageNum = 1;	
   	this._toolbarOperations = new Array();
   	this._popupOperations = new Array();	
	//this.pages = new Object();
	this._currentSortOrder = "1";
	ZaController.call(this, appCtxt, container, app, iKeyName);
	this.RESULTSPERPAGE = ZaSettings.RESULTSPERPAGE; 
	this.MAXSEARCHRESULTS = ZaSettings.MAXSEARCHRESULTS;
}

ZaListViewController.prototype = new ZaController();
ZaListViewController.prototype.constructor = ZaListViewController;

ZaListViewController.prototype._nextPageListener = 
function (ev) {
	if(this._currentPageNum < this.numPages) {
		this._currentPageNum++;
		this.show();	
	} 
}

ZaListViewController.prototype._prevPageListener = 
function (ev) {
	if(this._currentPageNum > 1) {
		this._currentPageNum--;
		/*if(this.pages[this._currentPageNum]) {
			this.show(this.pages[this._currentPageNum])
		} else {*/
			this.show();
		//}
	} 
}

/**
* @return ZaItemList - the list currently displaid in the list view
**/
ZaListViewController.prototype.getList = 
function() {
	return this._list;
}

ZaListViewController.prototype._updateUI = 
function(list) {
    if (!this._UICreated) {
		this._createUI();
	} 
	if (list) {
		var tmpArr = new Array();
		var cnt = list.getArray().length;
		for(var ix = 0; ix < cnt; ix++) {
			tmpArr.push(list.getArray()[ix]);
		}
		if(cnt < 1) {
			//if the list is empty - go to the previous page
		}
		//add the default column sortable
		this._contentView._bSortAsc = (this._currentSortOrder=="1");
		this._contentView.set(AjxVector.fromArray(tmpArr), this._contentView._defaultColumnSortable);	
	}
	this._removeList = new Array();
	this._changeActionsState();

	if(this.numPages <= this._currentPageNum) {
		this._toolbar.enable([ZaOperation.PAGE_FORWARD], false);
	} else {
		this._toolbar.enable([ZaOperation.PAGE_FORWARD], true);
	}
	if(this._currentPageNum == 1) {
		this._toolbar.enable([ZaOperation.PAGE_BACK], false);
	} else {
		this._toolbar.enable([ZaOperation.PAGE_BACK], true);
	}
}

ZaListViewController.prototype.searchCallback =
function(params, resp) {
	try {
		if(!resp) {
			throw(new AjxException(ZaMsg.ERROR_EMPTY_RESPONSE_ARG, AjxException.UNKNOWN, "ZaListViewController.prototype.searchCallback"));
		}
		if(resp.isException()) {
			throw(resp.getException());
		} else {
			var response = resp.getResponse().Body.SearchDirectoryResponse;
			this._list = new ZaItemList(params.CONS, this._app);	
			this._list.loadFromJS(response);	
			var searchTotal = response.searchTotal;
			var limit = params.limit ? params.limit : this.RESULTSPERPAGE; 
			this.numPages = Math.ceil(searchTotal/params.limit);
			if(params.show)
				this._show(this._list);			
			else
				this._updateUI(this._list);
		}
	} catch (ex) {
		if (ex.code != ZmCsfeException.MAIL_QUERY_PARSE_ERROR) {
			this._handleException(ex, "ZaListViewController.prototype.searchCallback");	
		} else {
			this.popupErrorDialog(ZaMsg.queryParseError, ex);
			if(this._searchField)
				this._searchField.setEnabled(true);	
		}		
	}
}

/**
* @param nextViewCtrlr - the controller of the next view
* Checks if it is safe to leave this view. Displays warning and Information messages if neccesary.
**/
ZaListViewController.prototype.switchToNextView = 
function (nextViewCtrlr, func, params) {
	func.call(nextViewCtrlr, params);
}

/**
* @param ev
* This listener is invoked by any other controller that can change an object in this controller
**/
ZaListViewController.prototype.handleChange = 
function (ev) {
	if(ev && this.objType && ev.type==this.objType) {
		if(ev.getDetails()) {
			this.show();			
		}
	}
}

/**
* @param ev
* This listener is invoked by any other controller that can create an object in the view controlled by this controller
**/
ZaListViewController.prototype.handleCreation = 
function (ev) {
	if(ev && this.objType && ev.type==this.objType) {
		if(ev.getDetails()) {
			this.show();			
		}
	}
}

/**
* @param ev
* This listener is invoked by any other controller that can remove an object form the view controlled by this controller
**/
ZaListViewController.prototype.handleRemoval = 
function (ev) {
	if(ev &&  this.objType && ev.type==this.objType) {
		if(ev.getDetails()) {
			this.show();			
		}
	}
}