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

function ZmPopAccountsView(parent, appCtxt, controller) {
    var labels = {
        infoTitle: ZmMsg.popAccounts,
        infoContents: ZmMsg.popAccountsInfo,
        detailsHeader: ZmMsg.popAccountSettings,
        listHeader: ZmMsg.popAccounts
    };
	ZmPrefListView.call(this, parent, appCtxt, controller, labels, "ZmPopAccountView");

	this._title = [ZmMsg.zimbraTitle, ZmMsg.options, ZmPrefView.TAB_NAME[ZmPrefView.POP_ACCOUNTS]].join(": ");
};

ZmPopAccountsView.prototype = new ZmPrefListView;
ZmPopAccountsView.prototype.constructor = ZmPopAccountsView;
 
ZmPopAccountsView.prototype.toString =
function() {
	return "ZmPopAccountsView";
};

// Public methods

ZmPopAccountsView.prototype.getTitle =
function() {
	return this._title;
};

ZmPopAccountsView.prototype.showItem =
function(account) {
    this._account = account;
    if (account) {
        this._accountView.setAccount(account);
        this._accountView.setVisible(true);
        this._accountView.focus();
        this.validate();
    }
    else {
        this._accountView.setVisible(false);
    }
};

ZmPopAccountsView.prototype.addCommand = function(batchCommand) {
    // gather up creates, updates, and deletes
    var creates = [];
    var modifies = [];
    var deletes = [];

    var list = this.getList();
    var items = list.getList().getArray();
    for (var i = 0; i < items.length; i++) {
        var account = items[i];
        var isNew = account._new;
        if (!isNew) {
            var modified = false;
            for (var pname in account) {
                if (pname.match(/^_/)) continue;
                if (account.hasOwnProperty(pname)) {
                    modified = true;
                    break;
                }
            }
            if (!modified) {
                continue;
            }
        }
        var array = isNew ? creates : modifies;
        array.push(account);
    }

    var removedItems = this._controller.getRemovedItems();
    for (var i = 0; i < removedItems.length; i++) {
        var account = removedItems[i];
        deletes.push(account);
    }

    // is there anything to do?
    if (creates.length == 0 && modifies.length == 0 && deletes.length == 0) {
        return;
    }

    // add soap docs to batch request
    var errorCallback = new AjxCallback(this, this._commandException);
    var execFrame;
    for (var i = 0; i < creates.length; i++) {
        var account = creates[i];
        var callback = new AjxCallback(this, this._commandResult, [account]);
        account.create(callback, errorCallback, batchCommand);

        // create identity
        if (account._identity.checked) {
            var email = account._identity.email;
            var display = email.replace(/@.*$/,"");

            var identity = new ZmIdentity(this._appCtxt);
            identity.name = account.name;
            identity.sendFromDisplay = display;
            identity.sendFromAddress = email;
            identity.setReplyTo = true;
            identity.setReplyToDisplay = display;
            identity.setReplyToAddress = email;
            identity.useWhenSentTo = account._identity.linkAddr;
            identity._whenSentoAddresses = [email];
            identity.useWhenInFolder = account._identity.linkFolder;
            identity.whenInFolderIds = [account.folderId];

            identity.createRequest("CreateIdentityRequest", batchCommand);
        }
    }
    for (var i = 0; i < modifies.length; i++) {
        var account = modifies[i];
        var callback = new AjxCallback(this, this._commandResult, [account]);
        account.save(callback, errorCallback, batchCommand);
    }
    for (var i = 0; i < deletes.length; i++) {
        var account = deletes[i];
        var callback = new AjxCallback(this, this._commandResult, [account]);
        account.doDelete(callback, errorCallback, batchCommand);
    }
};

ZmPopAccountsView.prototype.getAccount = function() {
    return this._account;
};

ZmPopAccountsView.prototype.isDirty = function() {
    return this._accountView.isDirty();
};

ZmPopAccountsView.prototype.reset = function() {
	//TODO: Implement this. It gets called by the pop shield-no callback. 
};

// Protected methods

ZmPopAccountsView.prototype._commandResult = function(account, result) {
    var prefsApp = this._appCtxt.getApp(ZmZimbraMail.PREFERENCES_APP);
    var collection = prefsApp.getDataSourceCollection();

    var data = result._data;
    if (data.CreateDataSourceResponse) {
        var pop3 = data.CreateDataSourceResponse.pop3[0];
        account.id = pop3.id;
        account.password = "";
        delete account._new;
        delete account._identity;
        collection.add(account);
    }
    if (data.ModifyDataSourceResponse) {
        collection.modify(account);
    }
    if (data.DeleteDataSourceResponse) {
        /*** HACK: response is incorrect ***
        var pop3 = data.DeleteDataSourceResponse.pop3[0];
        var account = collection.getById(pop3.id);
        /***/
        collection.remove(account);
    }
};
ZmPopAccountsView.prototype._commandException = function(ex) {
//    debugger;
};

ZmPopAccountsView.prototype._validateSelectedItem = function(errors) {
    if (this._account) {
        this._accountView._validateSelectedItem(errors);
    }
};

ZmPopAccountsView.prototype._createDetails = function(parentElement) {
    // REVISIT: When we have more server support, will separate
    //       settings into two pages: basic and advanced.
    this._accountView = new ZmPopAccountBasicPage(this, this._appCtxt, "basic");
    this._accountView.setVisible(false);

    var el = this._accountView.getHtmlElement();
    parentElement.appendChild(el);
};

ZmPopAccountsView.prototype._createItemHtml = function(item) {
    var span = document.createElement("SPAN");
    span.innerHTML = [
        "<INPUT type='checkbox' ",(item.enabled?"checked ":""),">"
    ].join("");
    var input = span.firstChild;
    Dwt.associateElementWithObject(input, {view:this, item:item}, "popItem");
    Dwt.setHandler(input, DwtEvent.ONMOUSEDOWN, ZmPopAccountsView._eventHandled);
    Dwt.setHandler(input, DwtEvent.ONMOUSEUP, ZmPopAccountsView._eventHandled);
    Dwt.setHandler(input, DwtEvent.ONCLICK, ZmPopAccountsView._handleItemCheckbox);

    var div = ZmPrefListView.prototype._createItemHtml.call(this, item);
    var table = div.firstChild;
    var row = table.rows[0];
    var cell = row.insertCell(0);
    cell.appendChild(input);
    return div;
};

ZmPopAccountsView._eventHandled = function(event) {
    DwtUiEvent.setBehaviour(event, true, true);
};
ZmPopAccountsView._handleItemCheckbox = function(event) {
    var target = DwtUiEvent.getTarget(event);
    var object = Dwt.getObjectFromElement(target, "popItem");
    var view = object.view;
    var item = object.item;
    item.enabled = target.checked;
    view._accountView._dirtyListener(null);
    view.validate();
};

//
// Class: ZmPopAccountBasicPage
//

function ZmPopAccountBasicPage(parent, appCtxt, pageId, className, posStyle) {
	DwtTabViewPage.call(this, parent, className, posStyle);
	this._appCtxt = appCtxt;
	this._pageId = pageId;
    this._createHtml();
}
ZmPopAccountBasicPage.prototype = new DwtTabViewPage;
ZmPopAccountBasicPage.prototype.constructor = ZmPopAccountBasicPage;

// Constants

ZmPopAccountBasicPage.DEFAULT_FOLDER_ID = ZmOrganizer.ID_INBOX; 

// Data

ZmPopAccountBasicPage.prototype._account;

ZmPopAccountBasicPage.prototype._nameField;
ZmPopAccountBasicPage.prototype._serverField;
ZmPopAccountBasicPage.prototype._usernameField;
ZmPopAccountBasicPage.prototype._password1Field;
ZmPopAccountBasicPage.prototype._showPasswordEl;
ZmPopAccountBasicPage.prototype._folderButton;
ZmPopAccountBasicPage.prototype._portField;

ZmPopAccountBasicPage.prototype._createIdentityEl;
ZmPopAccountBasicPage.prototype._identityProps;
ZmPopAccountBasicPage.prototype._identityNameEl;
ZmPopAccountBasicPage.prototype._emailField;
ZmPopAccountBasicPage.prototype._linkAddrEl;
ZmPopAccountBasicPage.prototype._linkFolderEl;

ZmPopAccountBasicPage.prototype._isDirty;
ZmPopAccountBasicPage.prototype._isLinkEmailDirty;

// HTML Handlers

ZmPopAccountBasicPage._handleCreateIdentity = function(event) {
    var target = DwtUiEvent.getTarget(event);
    var page = Dwt.getObjectFromElement(target);
    page._account._identity.checked = target.checked;
    page._setCreateIdentity(target.checked);
    page.parent.validate();
};
ZmPopAccountBasicPage._handleLinkAddr = function(event) {
    ZmPopAccountBasicPage._handleLinkAddrOrFolder("linkAddr", event);
};
ZmPopAccountBasicPage._handleLinkFolder = function(event) {
    ZmPopAccountBasicPage._handleLinkAddrOrFolder("linkFolder", event);
};
ZmPopAccountBasicPage._handleLinkAddrOrFolder = function(pname, event) {
    var target = DwtUiEvent.getTarget(event);
    var page = Dwt.getObjectFromElement(target);
    page._account._identity[pname] = target.checked;
};
ZmPopAccountBasicPage._handleShowPassword = function(event) {
    var target = DwtUiEvent.getTarget(event);
    var page = Dwt.getObjectFromElement(target);
    page._password1Field.getInputElement().type = target.checked ? "text" : "password";
};

// Public methods

ZmPopAccountBasicPage.prototype.setAccount = function(account) {
    // save account object
    this._account = account;
    var isNew = Boolean(account._new);
    if (isNew && !account._identity) {
        account._identity = {
            checked: false, linkAddr: true, linkFolder: true 
        };
    }

    // initialize input fields
    this._nameField.setValue(account.name);
    this._serverField.setValue(account.mailServer);
    this._usernameField.setValue(account.userName);
    this._password1Field.setRequired(isNew);
    this._password1Field.setValue(account.password);
    this._password1Field.getInputElement().type = "password";
    this._showPasswordEl.checked = false;

    var folderId = account.folderId || ZmPopAccountBasicPage.DEFAULT_FOLDER_ID;
    var tree = this._appCtxt.getTree(ZmOrganizer.FOLDER);
    var folder = tree.getById(folderId);

    this._folderButton.setText(folder.name);
    this._portField.setValue(account.port || ZmAccount.PORT_DEFAULT);

    // initialize other form state
    var createIdentityDiv = this._createIdentityEl.parentNode;
    createIdentityDiv.style.display = isNew ? "block" : "none";

    this._isDirty = false;
    this._isLinkEmailDirty = account._identity && account._identity._emailDirty;

    this._createIdentityEl.checked = account._identity && account._identity.checked;
    this._updateLinkName();
    this._updateLinkEmail();
    this._linkAddrEl.checked = account._identity && account._identity.linkAddr;
    this._linkFolderEl.checked = account._identity && account._identity.linkFolder;
    this._setCreateIdentity(this._createIdentityEl.checked);
};

ZmPopAccountBasicPage.prototype.getAccount = function() {
    return this._account;
};

ZmPopAccountsView.prototype.isDirty = function() {
    return this._isDirty;
};

ZmPopAccountBasicPage.prototype._validateSelectedItem = function(errors) {
    // tally errors (only if enabled)
    var account = this._account;
    if (account.enabled) {
        var isNew = account._new;
        var requiredFields = account.name && account.mailServer && account.userName;
        var requiredNewFields = !isNew || (account.password && account._identity.email);
        if (!requiredFields || !requiredNewFields) {
            errors.push(ZmMsg.errorMissingRequired);
        }
        if (isNew && account._identity.email && !this._emailField.isValid()) {
            errors.push(ZmMsg.errorInvalidEmail);
        }
    }
    else if (!account.name) {
        errors.push(ZmMsg.errorMissingRequired);
    }
    if (account.name && !this._nameField.isValid()) {
        errors.push(ZmMsg.errorDuplicateName);
    }

    // toggle test button
    var connectionFields = account.mailServer && account.userName && account.password;
    this._testButton.setEnabled(connectionFields);
};

ZmPopAccountBasicPage.prototype.focus = function() {
    this._nameField.focus();
};

// Protected methods

ZmPopAccountBasicPage.prototype._updateLinkName = function() {
    var name = this._nameField.getValue();
    this._identityNameEl.innerHTML = AjxStringUtil.htmlEncode(name);
};
ZmPopAccountBasicPage.prototype._updateLinkEmail = function() {
    if (this._isLinkEmailDirty) return;

    var userName = this._usernameField.getValue();
    var mailServer = this._serverField.getValue();
    var value = userName && mailServer ? [userName,mailServer].join("@") : "";
    this._emailField.setValue(value);
};
ZmPopAccountBasicPage.prototype._setCreateIdentity = function(state) {
    this._identityProps.setVisible(state);
};

ZmPopAccountBasicPage.prototype._validateName = function(value) {
    var name = value && value.replace(/^\s+$/);
    if (!name || name == '') {
        return null;
    }
    var view = this.parent;
    var list = view.getList();
    var items = list.getList().getArray();
    for (var i = 0; i < items.length; i++) {
        var account = items[i];
        if (account === this._account) continue;
        if (account.name == name) {
            return null;
        }
    }
    return value;
};
ZmPopAccountBasicPage.prototype._validateEmail = function(value) {
    if (/^.+@.+$/.test(value)) {
        return value;
    }
    return null;
};

ZmPopAccountBasicPage.prototype._createHtml = function() {
    // create controls
    var props = new DwtPropertySheet(this);

    this._nameField = new DwtInputField({
        parent:props, size:30, required:true,
        validationStyle:DwtInputField.CONTINUAL_VALIDATION,
        validatorCtxtObj:this, validator: this._validateName
    });
    this._serverField = new DwtInputField({
        parent:props, size:30, required:true,
        validationStyle:DwtInputField.CONTINUAL_VALIDATION
    });
    this._portField = new DwtInputField({
        parent:props, type:DwtInputField.INTEGER, size:5,
        validationStyle:DwtInputField.CONTINUAL_VALIDATION
    });
    this._usernameField = new DwtInputField({
        parent:props, size:20, required:true,
        validationStyle:DwtInputField.CONTINUAL_VALIDATION
    });

    this._password1Field = new DwtInputField({
        parent:props, type:DwtInputField.PASSWORD, size:20, required:true,
        validationStyle:DwtInputField.CONTINUAL_VALIDATION
    });
    this._testButton = new DwtButton(props);
    var testId = [this._htmlElId,"test"].join("_");
    var testHtml = [
        "<table border='0' style='border-collapse:collapse'>",
            "<tr><td id='",testId,"'></td></tr>",
        "</table>"
    ].join("");
    this._testButton.setText(ZmMsg.popAccountTest);

    this._folderButton = new DwtButton(props);
    var folderId = [this._htmlElId,"folder"].join("_");
    var folderHtml = [
        "<table border='0' style='border-collapse:collapse'>",
            "<tr><td id='",folderId,"'></td></tr>",
        "</table>"
    ].join("");

    var serverId = [this._htmlElId,"server"].join("_");
    var portId = [this._htmlElId,"port"].join("_");
    var serverHtml = [
        "<table border='0' cellPadding='0' cellSpacing='0' style='border-collapse:collapse'>",
            "<tr>",
                "<td id='",serverId,"'></td>",
                "<td style='padding-left:0.5em;padding-right:0.25em'>","Port:","</td>",
                "<td id='",portId,"'></td>",
                "<td style='padding-left:0.5em'>",ZmMsg.defLabel," 110</td>",
            "</tr>",
        "</table>"
    ].join("");

    var passwordId = [this._htmlElId,"password"].join("_");
    var showPasswordId = [this._htmlElId,"showPassword"].join("_");
    var passwordHtml = [
        "<table border='0' style='border-collapse:collapse'>",
            "<tr>",
                "<td id='",passwordId,"'></td>",
                "<td style='padding-left:0.5em'>",
                    "<input type='checkbox' id='",showPasswordId,"'> ",
                    ZmMsg.showPassword,
                "</td>",
            "</tr>",
        "</table>"
    ].join("");

    var createIdentityId = [this._htmlElId,"createIdentity"].join("_");
    var createIdentityHtml = [
        "<input type='checkbox' id='",createIdentityId,"'> ",
        ZmMsg.popAccountCreateNewIdentity
    ].join("");

    var createIdentityEl = document.createElement("DIV");
    createIdentityEl.className = "ZmPopAccountsViewLink";
    createIdentityEl.innerHTML = createIdentityHtml;
    this.getHtmlElement().appendChild(createIdentityEl);

    var identityProps = new DwtPropertySheet(this, "ZmPopAccountsViewLinkOpts");
    this._identityProps = identityProps;
    createIdentityEl.appendChild(this._identityProps.getHtmlElement());

    var identityNameEl = document.createElement("SPAN");

    this._emailField = new DwtInputField({
        parent:identityProps, size:30, required:true,
        validationStyle:DwtInputField.CONTINUAL_VALIDATION,
        validatorCtxtObj: this, validator: this._validateEmail
    });

    var linkAddrId = [this._htmlElId,"linkAddr"].join("_");
    var linkAddrHtml = [
        "<input type='checkbox' checked id='",linkAddrId,"'> ",
        ZmMsg.popAccountLinkForAddr
    ].join("");
    var linkFolderId = [this._htmlElId,"linkFolder"].join("_");
    var linkFolderHtml = [
        "<input type='checkbox' checked id='",linkFolderId,"'> ",
        ZmMsg.popAccountLinkForFolder
    ].join("");

    // add properties
    props.addProperty(ZmMsg.popAccountNameLabel, this._nameField);
    props.addProperty("", "<hr>");
    props.addProperty(ZmMsg.popAccountServerLabel, serverHtml);
    props.addProperty(ZmMsg.usernameLabel, this._usernameField);
    props.addProperty(ZmMsg.passwordLabel, passwordHtml);
    props.addProperty("", testHtml);
    props.addProperty("", "<hr>");
    props.addProperty(ZmMsg.popAccountFolderLabel, folderHtml);

    identityProps.addProperty(ZmMsg.identityNameLabel, identityNameEl);
    identityProps.addProperty(ZmMsg.emailAddrLabel, this._emailField);
    identityProps.addProperty(ZmMsg.popAccountLinkLabel, linkAddrHtml);
    identityProps.addProperty("", linkFolderHtml);

    // attach components
    var serverEl = document.getElementById(serverId);
    serverEl.appendChild(this._serverField.getHtmlElement());
    var portEl = document.getElementById(portId);
    portEl.appendChild(this._portField.getHtmlElement());
    var passwordEl = document.getElementById(passwordId);
    passwordEl.appendChild(this._password1Field.getHtmlElement());
    var folderEl = document.getElementById(folderId);
    folderEl.appendChild(this._folderButton.getHtmlElement());
    var testEl = document.getElementById(testId);
    testEl.appendChild(this._testButton.getHtmlElement());

    // save refs to elements
    this._createIdentityEl = document.getElementById(createIdentityId);
    this._identityNameEl = identityNameEl;
    this._linkAddrEl = document.getElementById(linkAddrId);
    this._linkFolderEl = document.getElementById(linkFolderId);
    this._showPasswordEl = document.getElementById(showPasswordId);

    // register handlers and associate elements
    Dwt.setHandler(this._createIdentityEl, DwtEvent.ONCLICK, ZmPopAccountBasicPage._handleCreateIdentity);
    Dwt.setHandler(this._linkAddrEl, DwtEvent.ONCLICK, ZmPopAccountBasicPage._handleLinkAddr);
    Dwt.setHandler(this._linkFolderEl, DwtEvent.ONCLICK, ZmPopAccountBasicPage._handleLinkFolder);
    Dwt.setHandler(this._showPasswordEl, DwtEvent.ONCLICK, ZmPopAccountBasicPage._handleShowPassword);
    Dwt.associateElementWithObject(this._createIdentityEl, this);
    Dwt.associateElementWithObject(this._linkAddrEl, this);
    Dwt.associateElementWithObject(this._linkFolderEl, this);
    Dwt.associateElementWithObject(this._showPasswordEl, this);

    // add listeners
    var nameListener = new AjxListener(this, this._nameListener);
    var serverListener = new AjxListener(this, this._serverOrUserNameListener, [this._serverField, "mailServer"]);
    var portListener = new AjxListener(this, this._portListener);
    var userNameListener = new AjxListener(this, this._serverOrUserNameListener, [this._usernameField, "userName"]);
    var passwordListener = new AjxListener(this, this._passwordListener);
    var folderListener = new AjxListener(this, this._folderListener);
    var emailListener = new AjxListener(this, this._emailListener);
    var testListener = new AjxListener(this, this._testListener);

    this._nameField.addListener(DwtUiEvent.ONKEYUP, nameListener);
    this._serverField.addListener(DwtUiEvent.ONKEYUP, serverListener);
    this._portField.addListener(DwtUiEvent.ONKEYUP, portListener);
    this._usernameField.addListener(DwtUiEvent.ONKEYUP, userNameListener);
    this._password1Field.addListener(DwtUiEvent.ONKEYUP, passwordListener);
    this._folderButton.addSelectionListener(folderListener);
    this._emailField.addListener(DwtUiEvent.ONKEYUP, emailListener);
    this._testButton.addSelectionListener(testListener);
};

ZmPopAccountBasicPage.prototype._fieldListener =
function(field, pname, evt) {
    this._account[pname] = field.getValue();
};

ZmPopAccountBasicPage.prototype._nameListener = function(evt) {
    this._fieldListener(this._nameField, "name", evt);
    var name = this._nameField.getValue();
    this._identityNameEl.innerHTML = AjxStringUtil.htmlEncode(name);
    this.parent.getList().redrawItem(this._account);
    this._dirtyListener(evt);
};

ZmPopAccountBasicPage.prototype._serverOrUserNameListener =
function(field, pname, evt) {
    this._fieldListener(field, pname, evt);
    if (this._account._new && !this._isLinkEmailDirty) {
        var uname = this._usernameField.getValue();
        var server = this._serverField.getValue();

        var email = uname && server ? [uname,server].join("@") : ""; 
        this._account._identity.email = email;
        this._emailField.setValue(email);
    }
    this._dirtyListener(evt);
};

ZmPopAccountBasicPage.prototype._portListener =
function(evt) {
    this._fieldListener(this._portField, "port", evt);
    this._dirtyListener(evt);
};

ZmPopAccountBasicPage.prototype._passwordListener =
function(evt) {
    this._fieldListener(this._password1Field, "password", evt);
    this._dirtyListener(evt);
};

ZmPopAccountBasicPage.prototype._emailListener = function(evt) {
    this._account._identity.email = this._emailField.getValue();
    this._isLinkEmailDirty = true;
    this._dirtyListener(evt);
};

ZmPopAccountBasicPage.prototype._folderListener = function(evt) {
    var dialog = this._appCtxt.getChooseFolderDialog();
    dialog.reset();
    dialog.registerCallback(DwtDialog.OK_BUTTON, this._folderOkListener, this, [dialog]);
    dialog.popup([ZmOrganizer.FOLDER], null, true, ZmMsg.popAccountFolderSelect);
};
ZmPopAccountBasicPage.prototype._folderOkListener = function(dialog, folder) {
    dialog.popdown();
    this._folderButton.setText(folder.name);
    this._account.folderId = folder.id;
    this._dirtyListener(null);
};

ZmPopAccountBasicPage.prototype._dirtyListener = function(evt) {
    // REVISIT: Do something with this information?
    this._isDirty = true;
};

ZmPopAccountBasicPage.prototype._testListener = function(evt) {
    var account = this._account;
    var callback = new AjxCallback(this, this._testResponse, [account]);
    account.testConnection(callback);
};
ZmPopAccountBasicPage.prototype._testResponse = function(account, resp) {
    var response = resp && resp.TestDataSourceResponse;
    var pop3 = response && response.pop3;
    var success = pop3.success;

    var message = success ? ZmMsg.popAccountSuccess : ZmMsg.popAccountFailure;
    var details = !success ? pop3.error : null;
    var style = success ? DwtMessageDialog.INFO_STYLE : DwtMessageDialog.WARNING_STYLE;
    var title = ZmMsg.popAccountTest;

    var dialog = this._appCtxt.getMsgDialog();
    dialog.setMessage(message, details, style, title);
    dialog.popup();
};

/***
//
// Class: ZmPopAccountAdvancedPage
//

function ZmPopAccountAdvancedPage(parent, appCtxt, pageId, className, posStyle) {
	DwtTabViewPage.call(this, parent, className, posStyle);
	this._appCtxt = appCtxt;
	this._pageId = pageId;
}
ZmPopAccountAdvancedPage.prototype = new DwtTabViewPage;
ZmPopAccountAdvancedPage.prototype.constructor = ZmPopAccountAdvancedPage;
/***/