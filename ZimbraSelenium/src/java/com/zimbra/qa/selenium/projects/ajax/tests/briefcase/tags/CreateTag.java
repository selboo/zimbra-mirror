package com.zimbra.qa.selenium.projects.ajax.tests.briefcase.tags;

import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.items.*;
import com.zimbra.qa.selenium.framework.items.FolderItem.SystemFolder;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.DialogTag;

public class CreateTag extends AjaxCommonTest {

	public CreateTag() {
		logger.info("New " + CreateTag.class.getCanonicalName());

		// All tests start at the Briefcase page
		super.startingPage = app.zPageBriefcase;
		super.startingAccountPreferences = null;

	}

	@Test(description = "Create a new tag by clicking 'new tag' on folder tree", groups = { "functional" })
	public void CreateTag_01() throws HarnessException {
		ZimbraAccount account = app.zGetActiveAccount();

		// Set the new tag name
		String name = "tag" + ZimbraSeleniumProperties.getUniqueString();

		DialogTag dialog = (DialogTag) app.zTreeBriefcase
				.zPressButton(Button.B_TREE_NEWTAG);
		ZAssert.assertNotNull(dialog, "Verify the new dialog opened");

		// Fill out the input field
		dialog.zSetTagName(name);
		dialog.zClickButton(Button.B_OK);

		// Make sure the tag was created on the server
		TagItem tag = TagItem.importFromSOAP(account, name);
		ZAssert.assertNotNull(tag, "Verify the new folder was created");

		ZAssert.assertEquals(tag.getName(), name,
				"Verify the server and client tag names match");
	}

	@Test(description = "Create a new tag using keyboard shortcuts", groups = { "functional" })
	public void CreateTag_02() throws HarnessException {
		ZimbraAccount account = app.zGetActiveAccount();

		FolderItem briefcaseFolder = FolderItem.importFromSOAP(account,
				SystemFolder.Briefcase);

		Shortcut shortcut = Shortcut.S_NEWTAG;

		// Set the new tag name
		String name = "tag" + ZimbraSeleniumProperties.getUniqueString();

		// refresh briefcase page tags section before creating a new tag
		app.zTreeBriefcase
				.zTreeItem(Action.A_LEFTCLICK, briefcaseFolder, false);

		DialogTag dialog = (DialogTag) app.zPageBriefcase
				.zKeyboardShortcut(shortcut);
		ZAssert.assertNotNull(dialog, "Verify the new dialog opened");

		// Fill out the input field
		dialog.zSetTagName(name);
		dialog.zClickButton(Button.B_OK);
		
		// Make sure the tag was created on the server
		TagItem tag = TagItem.importFromSOAP(account, name);
		ZAssert.assertNotNull(tag, "Verify the new folder was created");
		
		// Click on the tag and make sure it appears in the tree
		app.zTreeBriefcase.zTreeItem(Action.A_LEFTCLICK, tag);
		
		ZAssert.assertEquals(tag.getName(), name,
				"Verify the server and client tag names match");
	}

	@Test(description = "Create a new tag using context menu on a tag", groups = { "functional" })
	public void CreateTag_03() throws HarnessException {
		ZimbraAccount account = app.zGetActiveAccount();

		FolderItem briefcaseFolder = FolderItem.importFromSOAP(account,
				SystemFolder.Briefcase);

		// Set the new tag name
		String name1 = "tag" + ZimbraSeleniumProperties.getUniqueString();
		String name2 = "tag" + ZimbraSeleniumProperties.getUniqueString();

		// Create a tag to right click on
		account.soapSend("<CreateTagRequest xmlns='urn:zimbraMail'>"
				+ "<tag name='" + name1 + "' color='1' />"
				+ "</CreateTagRequest>");

		// Get the tag
		TagItem tag1 = TagItem.importFromSOAP(account, name1);
		
		// refresh briefcase page tags section before creating a new tag
		app.zTreeBriefcase
				.zTreeItem(Action.A_LEFTCLICK, briefcaseFolder, false);

		// Create a new tag using the context menu + New Tag
		DialogTag dialog = (DialogTag) app.zTreeBriefcase.zTreeItem(
				Action.A_RIGHTCLICK, Button.B_TREE_NEWTAG, tag1);
		ZAssert.assertNotNull(dialog, "Verify the new dialog opened");

		// Fill out the input field
		dialog.zSetTagName(name2);
		dialog.zClickButton(Button.B_OK);

		// Make sure the tag was created on the server
		TagItem tag2 = TagItem.importFromSOAP(account, name2);
		ZAssert.assertNotNull(tag2, "Verify the new tag was created");

		ZAssert.assertEquals(tag2.getName(), name2,
				"Verify the server and client tag names match");
	}

	@Test(description = "Create a new tag using briefcase app New -> New Tag", groups = { "functional" })
	public void CreateTag_04() throws HarnessException {
		ZimbraAccount account = app.zGetActiveAccount();

		// Set the new tag name
		String name = "tag" + ZimbraSeleniumProperties.getUniqueString();

		// Create a new tag in the Briefcase using the New pull down menu + Tag
		DialogTag dialog = (DialogTag) app.zPageBriefcase
				.zToolbarPressPulldown(Button.B_NEW, Button.O_NEW_TAG);
		ZAssert.assertNotNull(dialog, "Verify the new dialog opened");

		// Fill out the input field
		dialog.zSetTagName(name);
		dialog.zClickButton(Button.B_OK);

		// Make sure the tag was created on the server
		TagItem tag = TagItem.importFromSOAP(account, name);
		ZAssert.assertNotNull(tag, "Verify the new tag was created");

		ZAssert.assertEquals(tag.getName(), name,
				"Verify the server and client tag names match");
	}
}
