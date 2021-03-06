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
package com.zimbra.qa.selenium.projects.ajax.tests.calendar.meetings.organizer.singleday;

import java.awt.event.KeyEvent;
import java.util.Calendar;
import org.testng.annotations.*;
import com.zimbra.common.soap.Element;
import com.zimbra.qa.selenium.framework.core.Bugs;
import com.zimbra.qa.selenium.framework.items.*;
import com.zimbra.qa.selenium.framework.ui.*;
import com.zimbra.qa.selenium.framework.util.*;
import com.zimbra.qa.selenium.projects.ajax.core.CalendarWorkWeekTest;
import com.zimbra.qa.selenium.projects.ajax.ui.*;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew;
import com.zimbra.qa.selenium.projects.ajax.ui.mail.FormMailNew.Field;

public class CancelMeeting extends CalendarWorkWeekTest {	
	
	public CancelMeeting() {
		logger.info("New "+ CancelMeeting.class.getCanonicalName());
		super.startingPage = app.zPageCalendar;
	}
	
	@Bugs(ids = "69132")
	@Test(description = "Cancel meeting using Delete toolbar button",
			groups = { "smoke" })
			
	public void CancelMeeting_01() throws HarnessException {
		

		//-- Data setup

		
		// Creating object for meeting data
		String tz, apptSubject, apptBody, apptAttendee1;
		tz = ZTimeZone.TimeZoneEST.getID();
		apptSubject = "app" + ZimbraSeleniumProperties.getUniqueString();
		apptBody = "body" + ZimbraSeleniumProperties.getUniqueString();
		apptAttendee1 = ZimbraAccount.AccountA().EmailAddress;
		
		// Absolute dates in UTC zone
		Calendar now = this.calendarWeekDayUTC;
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 14, 0, 0);
		
		app.zGetActiveAccount().soapSend(
                "<CreateAppointmentRequest xmlns='urn:zimbraMail'>" +
                     "<m>"+
                     "<inv method='REQUEST' type='event' status='CONF' draft='0' class='PUB' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"+
                     "<s d='"+ startUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<e d='"+ endUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<or a='"+ app.zGetActiveAccount().EmailAddress +"'/>" +
                     "<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee1 + "' d='2'/>" + 
                     "</inv>" +
                     "<e a='"+ apptAttendee1 +"' t='t'/>" +
                     "<mp content-type='text/plain'>" +
                     "<content>"+ apptBody +"</content>" +
                     "</mp>" +
                     "<su>"+ apptSubject +"</su>" +
                     "</m>" +
               "</CreateAppointmentRequest>");
		
		
		//-- GUI actions
		
		
        // Refresh the view
        app.zPageCalendar.zToolbarPressButton(Button.B_REFRESH);
        
        // Select the appointment
        app.zPageCalendar.zListItem(Action.A_LEFTCLICK, apptSubject);
        
        // Press Delete toolbar button
        DialogWarning dialog = (DialogWarning)app.zPageCalendar.zToolbarPressButton(Button.B_DELETE);
        
        // Wait for the "Send Cancellation" dialog
        // Click Send Cancellation
		dialog.zClickButton(Button.B_SEND_CANCELLATION);
		
		
		
		//-- Verification
		
		
		// Verify meeting disappears from the view
		ZAssert.assertEquals(app.zPageCalendar.zIsAppointmentExists(apptSubject), false, "Verify meeting is deleted from organizer's calendar");
		
		// Verify meeting is deleted from attendee's calendar
		AppointmentItem canceledAppt = AppointmentItem.importFromSOAP(ZimbraAccount.AccountA(), "subject:("+ apptSubject +")");
		ZAssert.assertNull(canceledAppt, "Verify meeting is deleted from attendee's calendar");
		
	}
	
	@DataProvider(name = "DataProviderShortcutKeys")
	public Object[][] DataProviderShortcutKeys() {
		return new Object[][] {
				new Object[] { "VK_DELETE", KeyEvent.VK_DELETE },
				new Object[] { "VK_BACK_SPACE", KeyEvent.VK_BACK_SPACE },
		};
	}

	@Bugs(ids = "69132")
	@Test(description = "Cancel meeting using keyboard shortcuts Del & Backspace",
			groups = { "functional" },
			dataProvider = "DataProviderShortcutKeys")
	public void CancelMeeting_02(String name, int keyEvent) throws HarnessException {
		
		//-- Data Setup
		
		
		// Creating object for meeting data
		String tz, apptSubject, apptBody, apptAttendee1;
		tz = ZTimeZone.TimeZoneEST.getID();
		apptSubject = ZimbraSeleniumProperties.getUniqueString();
		apptBody = ZimbraSeleniumProperties.getUniqueString();
		apptAttendee1 = ZimbraAccount.AccountA().EmailAddress;
		
		// Absolute dates in UTC zone
		Calendar now = this.calendarWeekDayUTC;
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 14, 0, 0);
		
		app.zGetActiveAccount().soapSend(
                "<CreateAppointmentRequest xmlns='urn:zimbraMail'>" +
                     "<m>"+
                     "<inv method='REQUEST' type='event' status='CONF' draft='0' class='PUB' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"+
                     "<s d='"+ startUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<e d='"+ endUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<or a='"+ app.zGetActiveAccount().EmailAddress +"'/>" +
                     "<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee1 + "' d='2'/>" + 
                     "</inv>" +
                     "<e a='"+ apptAttendee1 +"' t='t'/>" +
                     "<mp content-type='text/plain'>" +
                     "<content>"+ apptBody +"</content>" +
                     "</mp>" +
                     "<su>"+ apptSubject +"</su>" +
                     "</m>" +
               "</CreateAppointmentRequest>");
		
		
		//-- GUI actions
		
		
        // Refresh the view
        app.zPageCalendar.zToolbarPressButton(Button.B_REFRESH);
        
        // Select the appointment
        app.zPageCalendar.zListItem(Action.A_LEFTCLICK, apptSubject);
        
        // Cancel meeting using keyboard Del and Backspace key
        DialogWarning dialog = (DialogWarning)app.zPageCalendar.zKeyboardKeyEvent(keyEvent);
        
        // Wait for the "Send Cancellation" dialog
        // Click Send Cancellation
		dialog.zClickButton(Button.B_SEND_CANCELLATION);

		
		//-- Verification
		
		// Verify the meeting disappears from the organizer's calendar
		app.zGetActiveAccount().soapSend(
				"<SearchRequest xmlns='urn:zimbraMail' types='appointment' calExpandInstStart='"+ startUTC.addDays(-7).toMillis() +"' calExpandInstEnd='"+ startUTC.addDays(7).toMillis() +"'>"
			+	"<query>subject:("+ apptSubject +")</query>"
			+	"</SearchRequest>");
		Element[] nodes = app.zGetActiveAccount().soapSelectNodes("//mail:appt");
		ZAssert.assertEquals(nodes.length, 0, "Verify meeting is deleted from organizer's calendar");
		
		// Verify meeting is deleted from attendee's calendar
		AppointmentItem canceledAppt = AppointmentItem.importFromSOAP(ZimbraAccount.AccountA(), "subject:("+ apptSubject +")");
		ZAssert.assertNull(canceledAppt, "Verify meeting is deleted from attendee's calendar");
		
	}
	
	@Bugs(ids = "69132")
	@Test(description = "Don't cancel the meeting (press Cancel button from cancellation dialog)",
			groups = { "functional" })
	public void CancelMeeting_03() throws HarnessException {
		
		//-- Data Setup
		// Creating object for meeting data
		String tz, apptSubject, apptBody, apptAttendee1;
		tz = ZTimeZone.TimeZoneEST.getID();
		apptSubject = "appt" + ZimbraSeleniumProperties.getUniqueString();
		apptBody = ZimbraSeleniumProperties.getUniqueString();
		apptAttendee1 = ZimbraAccount.AccountA().EmailAddress;
		
		// Absolute dates in UTC zone
		Calendar now = this.calendarWeekDayUTC;
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 14, 0, 0);
		
		app.zGetActiveAccount().soapSend(
                "<CreateAppointmentRequest xmlns='urn:zimbraMail'>" +
                     "<m>"+
                     "<inv method='REQUEST' type='event' status='CONF' draft='0' class='PUB' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"+
                     "<s d='"+ startUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<e d='"+ endUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<or a='"+ app.zGetActiveAccount().EmailAddress +"'/>" +
                     "<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee1 + "' d='2'/>" + 
                     "</inv>" +
                     "<e a='"+ apptAttendee1 +"' t='t'/>" +
                     "<mp content-type='text/plain'>" +
                     "<content>"+ apptBody +"</content>" +
                     "</mp>" +
                     "<su>"+ apptSubject +"</su>" +
                     "</m>" +
               "</CreateAppointmentRequest>");
     
        		
		//-- GUI actions
		
		
        // Refresh the view
        app.zPageCalendar.zToolbarPressButton(Button.B_REFRESH);
        
        // Select the appointment
        app.zPageCalendar.zListItem(Action.A_LEFTCLICK, apptSubject);

        // Press Delete Toolbar button
        DialogWarning dialog = (DialogWarning)app.zPageCalendar.zToolbarPressButton(Button.B_DELETE);

        // Wait for the "Send Cancellation" dialog
        // Click Send Cancellation
		dialog.zClickButton(Button.B_CANCEL);

		
		//-- Verification
		ZAssert.assertTrue(app.zPageCalendar.zIsAppointmentExists(apptSubject), "Verify meeting is not deleted from organizer's calendar");
		
		// Verify meeting is not deleted from attendee's calendar
		AppointmentItem canceledAppt = AppointmentItem.importFromSOAP(ZimbraAccount.AccountA(), "subject:("+ apptSubject +")");
		ZAssert.assertNotNull(canceledAppt, "Verify meeting is NOT deleted from attendee's calendar");
		
	}
	
	
	@Bugs(ids = "69132")
	@Test(description = "Cancel appointment without modifying cancellation message content",
			groups = { "functional" })
	public void CancelMeeting_04() throws HarnessException {
		
		
		//-- Data Setup
		
		
		// Creating object for meeting data
		String tz, apptSubject, apptBody, apptAttendee1;
		tz = ZTimeZone.TimeZoneEST.getID();
		apptSubject = ZimbraSeleniumProperties.getUniqueString();
		apptBody = ZimbraSeleniumProperties.getUniqueString();
		apptAttendee1 = ZimbraAccount.AccountA().EmailAddress;
		
		// Absolute dates in UTC zone
		Calendar now = this.calendarWeekDayUTC;
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 14, 0, 0);
		
		app.zGetActiveAccount().soapSend(
                "<CreateAppointmentRequest xmlns='urn:zimbraMail'>" +
                     "<m>"+
                     "<inv method='REQUEST' type='event' status='CONF' draft='0' class='PUB' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"+
                     "<s d='"+ startUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<e d='"+ endUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<or a='"+ app.zGetActiveAccount().EmailAddress +"'/>" +
                     "<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee1 + "' d='2'/>" + 
                     "</inv>" +
                     "<e a='"+ apptAttendee1 +"' t='t'/>" +
                     "<mp content-type='text/plain'>" +
                     "<content>"+ apptBody +"</content>" +
                     "</mp>" +
                     "<su>"+ apptSubject +"</su>" +
                     "</m>" +
               "</CreateAppointmentRequest>");
		
		
		//-- GUI actions
		
		
        // Refresh the view
        app.zPageCalendar.zToolbarPressButton(Button.B_REFRESH);
        
        // Select the appointment
        app.zPageCalendar.zListItem(Action.A_LEFTCLICK, apptSubject);

        // Press Delete Toolbar button
        DialogWarning dialog = (DialogWarning)app.zPageCalendar.zToolbarPressButton(Button.B_DELETE);

        // Wait for the "Send Cancellation" dialog
        // Click Edit Cancellation
        // When the form opens, simply click "SEND" (don't edit content)
        FormMailNew mailComposeForm = (FormMailNew)dialog.zClickButton(Button.B_EDIT_CANCELLATION);
        
        // For some reason, the form takes some time to render
        // Sometimes the SEND button is not activated.
        SleepUtil.sleepMedium();
        
		mailComposeForm.zToolbarPressButton(Button.B_SEND);
		
		
		//-- Verification
		
		// Verify the appointment does not appear in the organizers calendar
		SleepUtil.sleepSmall(); //test fails without sleep
		ZAssert.assertEquals(app.zPageCalendar.zIsAppointmentExists(apptSubject), false, "Verify meeting is deleted from organizer's calendar");
		
		// Verify meeting is deleted from attendee's calendar && receive meeting cancellation message
		SleepUtil.sleepLong(); //importSOAP gives wrong response without sleep
		MailItem canceledApptMail = MailItem.importFromSOAP(ZimbraAccount.AccountA(), "subject:(" + (char)34 + "Cancelled " + apptSubject + (char)34 + ")");
		ZAssert.assertNotNull(canceledApptMail, "Verify meeting cancellation message received to attendee");
		
		AppointmentItem canceledAppt = AppointmentItem.importFromSOAP(ZimbraAccount.AccountA(), "subject:("+ apptSubject +")");
		ZAssert.assertNull(canceledAppt, "Verify meeting is deleted from attendee's calendar");
		
	}
	
	@Bugs(ids = "69132,77548")
	@Test(description = "Modify meeting cancellation message while cancelling appointment",
			groups = { "functional" })
	public void CancelMeeting_05() throws HarnessException {
		
		
		//-- Data Setup
		
		
		// Creating object for meeting data
		String tz, apptSubject, apptBody, apptAttendee1, modifyApptBody;
		tz = ZTimeZone.TimeZoneEST.getID();
		apptSubject = "appt" + ZimbraSeleniumProperties.getUniqueString();
		apptBody = ZimbraSeleniumProperties.getUniqueString();
		apptAttendee1 = ZimbraAccount.AccountA().EmailAddress;
		modifyApptBody = "Modified" + ZimbraSeleniumProperties.getUniqueString();
		
		// Absolute dates in UTC zone
		Calendar now = this.calendarWeekDayUTC;
		ZDate startUTC = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		ZDate endUTC   = new ZDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH), 14, 0, 0);
		
		app.zGetActiveAccount().soapSend(
                "<CreateAppointmentRequest xmlns='urn:zimbraMail'>" +
                     "<m>"+
                     "<inv method='REQUEST' type='event' status='CONF' draft='0' class='PUB' fb='B' transp='O' allDay='0' name='"+ apptSubject +"'>"+
                     "<s d='"+ startUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<e d='"+ endUTC.toTimeZone(tz).toYYYYMMDDTHHMMSS() +"' tz='"+ tz +"'/>" +
                     "<or a='"+ app.zGetActiveAccount().EmailAddress +"'/>" +
                     "<at role='REQ' ptst='NE' rsvp='1' a='" + apptAttendee1 + "' d='2'/>" + 
                     "</inv>" +
                     "<e a='"+ apptAttendee1 +"' t='t'/>" +
                     "<mp content-type='text/plain'>" +
                     "<content>"+ apptBody +"</content>" +
                     "</mp>" +
                     "<su>"+ apptSubject +"</su>" +
                     "</m>" +
               "</CreateAppointmentRequest>");
	
		
		//-- GUI actions
		
		
        // Refresh the view
        app.zPageCalendar.zToolbarPressButton(Button.B_REFRESH);
        
        // Select the appointment
        app.zPageCalendar.zListItem(Action.A_LEFTCLICK, apptSubject);

        // Press Delete Toolbar button
        DialogWarning dialog = (DialogWarning)app.zPageCalendar.zToolbarPressButton(Button.B_DELETE);

        // Wait for the "Send Cancellation" dialog
        // Click Edit Cancellation
        // When the form opens, simply click "SEND" (don't edit content)
        FormMailNew mailComposeForm = (FormMailNew)dialog.zClickButton(Button.B_EDIT_CANCELLATION);
        
        // For some reason, the form takes some time to render
        // Sometimes the SEND button is not activated.
        SleepUtil.sleepMedium();
        
		mailComposeForm.zFillField(Field.Body, modifyApptBody);		
		mailComposeForm.zToolbarPressButton(Button.B_SEND);
		
		
		//-- Verification
		
		// Verify the meeting no longer appears in the organizer's calendar
		ZAssert.assertEquals(app.zPageCalendar.zIsAppointmentExists(apptSubject), false, "Verify meeting is deleted from organizer's calendar");
		

		// Verify the meeting no longer appears in the attendee's calendar
		MailItem canceledApptMail = MailItem.importFromSOAP(ZimbraAccount.AccountA(), "in:inbox subject:(Cancelled) subject:("+ apptSubject +")"); // TODO: I18N
		
		// Verify meeting cancellation message with exact body content
		ZAssert.assertNotNull(canceledApptMail, "Verify meeting cancellation message received to attendee");
		ZAssert.assertStringContains(canceledApptMail.dBodyText, modifyApptBody, "Verify the body field value is correct");
		
		// Verify meeting is deleted from attendee's calendar
		AppointmentItem canceledAppt = AppointmentItem.importFromSOAP(ZimbraAccount.AccountA(), "subject:("+ apptSubject +")");
		ZAssert.assertNull(canceledAppt, "Verify meeting is deleted from attendee's calendar");
	}
}
