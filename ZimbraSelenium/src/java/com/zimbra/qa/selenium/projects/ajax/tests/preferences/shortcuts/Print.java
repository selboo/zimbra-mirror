/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.selenium.projects.ajax.tests.preferences.shortcuts;

import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.ui.Action;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.SeparateWindow;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.TreePreferences.TreeItem;


public class Print extends AjaxCommonTest {

	public Print() {
		
		super.startingPage = app.zPagePreferences;
		super.startingAccountPreferences = null;
	}


	@Test(
			description = "Print the shortcuts preference page",
			groups = { "functional" }
			)
	public void Print_01() throws HarnessException {

		
		// Navigate to preferences -> notifications
		app.zTreePreferences.zTreeItem(Action.A_LEFTCLICK, TreeItem.Shortcuts);

		
		// Verify the page is showing
		String locator = "css=div[id$='_SHORTCUT_PRINT'] div.ZButton td[id$='_title']";
		if ( !app.zPagePreferences.sIsElementPresent(locator) ) {
			throw new HarnessException("Print button does not exist");
		}
		
		SeparateWindow window = null;
		
		try {
				
			// Click Print, which opens a separate window
			window = new SeparateWindow(app);
			window.zInitializeWindowNames();
			app.zTreePreferences.zClickAt(locator, "");
			app.zTreePreferences.zWaitForBusyOverlay();

			
			// Make sure the window is there			
			window.zWaitForActive();
			ZAssert.assertTrue(window.zIsActive(), "Verify the print window is active");

		} finally {
			
			// Close the print window, if applicable
			
			if ( window != null ) {
				window.zCloseWindow();
				window = null;
			}
		}


	}
}
