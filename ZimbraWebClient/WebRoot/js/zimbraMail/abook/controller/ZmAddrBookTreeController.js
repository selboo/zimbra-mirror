/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

/**
* Creates an address book tree controller.
* @constructor
* @class
* This class is a controller for the tree view used by the address book 
* application. This class uses the support provided by ZmOperation. 
*
* @author Parag Shah
* @param appCtxt	[ZmAppCtxt]		main (singleton) app context
* @param type		[constant]		type of organizer we are displaying/controlling
* @param dropTgt	[DwtDropTgt]	drop target for this type
*/
function ZmAddrBookTreeController(appCtxt, type, dropTgt) {
	if (arguments.length === 0) return;

	type = type || ZmOrganizer.ADDRBOOK;
	dropTgt = dropTgt || (new DwtDropTarget("ZmContact"));

	ZmFolderTreeController.call(this, appCtxt, type, dropTgt);

	this._listeners[ZmOperation.NEW_ADDRBOOK] = new AjxListener(this, this._newListener);
	this._listeners[ZmOperation.SHARE_ADDRBOOK] = new AjxListener(this, this._shareAddrBookListener);
	this._listeners[ZmOperation.MOUNT_ADDRBOOK] = new AjxListener(this, this._mountAddrBookListener);
}

ZmAddrBookTreeController.prototype = new ZmFolderTreeController;
ZmAddrBookTreeController.prototype.constructor = ZmAddrBookTreeController;


// Public methods

ZmAddrBookTreeController.prototype.toString =
function() {
	return "ZmAddrBookTreeController";
};

ZmAddrBookTreeController.prototype.show =
function(params) {
	params.include = {};
	params.include[ZmFolder.ID_TRASH] = true;
	ZmTreeController.prototype.show.call(this, params);
};

// Enables/disables operations based on the given organizer ID
ZmAddrBookTreeController.prototype.resetOperations =
function(parent, type, id) {
	var deleteText = ZmMsg.del;

	if (id == ZmFolder.ID_TRASH) {
		parent.enableAll(false);
		parent.enable(ZmOperation.DELETE, true);
		deleteText = ZmMsg.emptyTrash;
	} else {
		parent.enableAll(true);
		var addrBook = this._dataTree.getById(id);
		if (addrBook) {
			if (addrBook.isSystem()) {
				parent.enable([ZmOperation.DELETE, ZmOperation.RENAME_FOLDER], false);
			} else if (addrBook.link) {
				parent.enable([ZmOperation.SHARE_ADDRBOOK], false);
			}
		}
	}

	var op = parent.getOp(ZmOperation.DELETE);
	if (op) {
		op.setText(deleteText);
	}
};


// Protected methods

// Returns a list of desired header action menu operations
ZmAddrBookTreeController.prototype._getHeaderActionMenuOps =
function() {
	var ops = [ ZmOperation.NEW_ADDRBOOK ];
	if (this._appCtxt.get(ZmSetting.SHARING_ENABLED)) {
		ops.push(ZmOperation.MOUNT_ADDRBOOK);
	}
	return ops;
};

// Returns a list of desired action menu operations
ZmAddrBookTreeController.prototype._getActionMenuOps =
function() {
	var ops = [];
	if (this._appCtxt.get(ZmSetting.SHARING_ENABLED))
		ops.push(ZmOperation.SHARE_ADDRBOOK);
	ops.push(ZmOperation.DELETE, ZmOperation.RENAME_FOLDER, ZmOperation.EDIT_PROPS);
	return ops;
};

/*
* Returns a title for moving a folder.
*/
ZmAddrBookTreeController.prototype._getMoveDialogTitle =
function() {
	return AjxMessageFormat.format(ZmMsg.moveAddrBook, this._pendingActionData.name);
};

// Returns the dialog for organizer creation
ZmAddrBookTreeController.prototype._getNewDialog =
function() {
	return this._appCtxt.getNewAddrBookDialog();
};

ZmAddrBookTreeController.prototype._getDropTarget =
function(appCtxt) {
	return (new DwtDropTarget(["ZmContact"]));
};


// Listeners

ZmAddrBookTreeController.prototype._shareAddrBookListener = 
function(ev) {
	this._pendingActionData = this._getActionedOrganizer(ev);
	this._appCtxt.getSharePropsDialog().popup(ZmSharePropsDialog.NEW, this._pendingActionData);
};

ZmAddrBookTreeController.prototype._mountAddrBookListener =
function(ev) {
	this._appCtxt.getMountFolderDialog().popup(ZmOrganizer.ADDRBOOK);
};

/*
* Called when a left click occurs (by the tree view listener). The folder that
* was clicked may be a search, since those can appear in the folder tree. The
* appropriate search will be performed.
*
* @param folder		ZmOrganizer		folder or search that was clicked
*/
ZmAddrBookTreeController.prototype._itemClicked =
function(folder) {
	// always reset the search type to be Contacts
	var sc = this._appCtxt.getSearchController();
	sc.setDefaultSearchType(ZmItem.CONTACT, true);

	var capp = this._appCtxt.getApp(ZmApp.CONTACTS);

	// force a search if user clicked Trash folder or share
	if (folder.id == ZmFolder.ID_TRASH || folder.link) {
		var types = sc.getTypes(ZmItem.CONTACT);
		sc.search({query:folder.createQuery(), types:types, fetch:true, sortBy:ZmSearch.NAME_ASC});
	} else {
		capp.showFolder(folder);
	}

	if (folder.id != ZmFolder.ID_TRASH) {
		var clc = AjxDispatcher.run("GetContactListController");
		clc.getParentView().getAlphabetBar().reset();
	}
};
