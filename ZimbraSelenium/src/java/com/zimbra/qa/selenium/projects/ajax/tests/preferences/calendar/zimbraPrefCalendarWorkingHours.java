/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
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
package com.zimbra.qa.selenium.projects.ajax.tests.preferences.calendar;

import java.util.Calendar;
import java.util.HashMap;
import org.testng.annotations.Test;

import com.zimbra.qa.selenium.framework.core.Bugs;
import com.zimbra.qa.selenium.framework.items.AppointmentItem;
import com.zimbra.qa.selenium.framework.ui.Action;
import com.zimbra.qa.selenium.framework.ui.Button;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.SleepUtil;
import com.zimbra.qa.selenium.framework.util.ZAssert;
import com.zimbra.qa.selenium.framework.util.ZDate;
import com.zimbra.qa.selenium.framework.util.ZTimeZone;
import com.zimbra.qa.selenium.framework.util.ZimbraAdminAccount;
import com.zimbra.qa.selenium.framework.util.ZimbraSeleniumProperties;
import com.zimbra.qa.selenium.projects.ajax.core.AjaxCommonTest;
import com.zimbra.qa.selenium.projects.ajax.ui.DialogWarning;
import com.zimbra.qa.selenium.projects.ajax.ui.calendar.DialogConfirmDeleteAppointment;
import com.zimbra.qa.selenium.projects.ajax.ui.calendar.FormApptNew;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.PagePreferences.Locators;
import com.zimbra.qa.selenium.projects.ajax.ui.preferences.TreePreferences.TreeItem;

@SuppressWarnings("unused")
public class zimbraPrefCalendarWorkingHours extends AjaxCommonTest {

	public zimbraPrefCalendarWorkingHours() {
		logger.info("New " + zimbraPrefCalendarWorkingHours.class.getCanonicalName());
		
		super.startingPage = app.zPagePreferences;
	}

	// Need to skip this test till bug 77465 get fixed otherwise automation may stuck at browser navigate away dialog 
	@Bugs(ids = "77465")
	@Test(
			description = "Set calendar custom working hours and verify accordingly", 
			groups = { "bug" })
	public void zimbraPrefCalendarWorkingHours() throws HarnessException {
		
		// Modify the test account
		ZimbraAdminAccount.GlobalAdmin().soapSend(
				"<ModifyAccountRequest xmlns='urn:zimbraAdmin'>"
			+		"<id>"+ app.zGetActiveAccount().ZimbraId +"</id>"
			+		"<a n='zimbraPrefWarnOnExit'>TRUE</a>"
			+	"</ModifyAccountRequest>");

		// Logout and login to pick up the changes
		app.zPageLogin.zNavigateTo();
		this.startingPage.zNavigateTo();

		// Navigate to preferences -> calendar
		app.zTreePreferences.zTreeItem(Action.A_LEFTCLICK, TreeItem.Calendar);
		SleepUtil.sleepMedium();

		// Select custom work hours for e.g. Tuesday to Friday
		app.zPagePreferences.zSelectRadioButton(Button.R_CUSTOM_WORK_HOURS);
		app.zPagePreferences.zPressButton(Button.B_CUSTOMIZE);
		app.zPagePreferences.zCheckboxSet(Button.C_MONDAY_WORK_HOUR, false);
		app.zPagePreferences.zPressButton(Button.B_OK);
		
		// Save preferences
		app.zPagePreferences.zToolbarPressButton(Button.B_SAVE);
		//DialogWarning dlgWarning = (DialogWarning) new DialogWarning(null, app, null).zClickButton(Button.B_NO);
		app.zPagePreferences.zPressButton(Button.B_NO);
 
		// Verify the preference value
		app.zGetActiveAccount().soapSend(
						"<GetPrefsRequest xmlns='urn:zimbraAccount'>"
				+			"<pref name='zimbraPrefCalendarWorkingHours'/>"
				+		"</GetPrefsRequest>");
		
		String value = app.zGetActiveAccount().soapSelectValue("//acct:pref[@name='zimbraPrefCalendarWorkingHours']", null);
		ZAssert.assertEquals(value, "1:N:0800:1700,2:N:0800:1700,3:Y:0800:1700,4:Y:0800:1700,5:Y:0800:1700,6:Y:0800:1700,7:N:0800:1700", "Verify zimbraPrefCalendarWorkingHours value (Sunday, Monday & Saturday as non-working days)'");
		
		// if logout stucks then assume that browser dialog appeared
		app.zPageMain.zLogout();
	}
}