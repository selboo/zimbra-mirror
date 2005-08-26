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
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.ParseMimeMessage;
import com.zimbra.cs.service.util.ParsedItemID;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.soap.ZimbraContext;

public class CreateAppointmentException extends CreateAppointment 
{
    private static Log sLog = LogFactory.getLog(CreateAppointmentException.class);
    private static StopWatch sWatch = StopWatch.getInstance("CreateAppointmentException");

    
    protected static class CreateApptExceptionInviteParser implements ParseMimeMessage.InviteParser
    {
        private String mUid;
        private TimeZoneMap mTzMap;
        
        CreateApptExceptionInviteParser(String uid, TimeZoneMap tzMap)
        {
            mUid = uid;
            mTzMap = tzMap;
        }
        
        public ParseMimeMessage.InviteParserResult parseInviteElement(OperationContext octxt, Account account, Element inviteElem) throws ServiceException 
        {
            return CalendarUtils.parseInviteForCreate(account, inviteElem, mTzMap, mUid, true);
        }
    };
    
    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element request, Map context) throws ServiceException 
    {
        long startTime = sWatch.start();
        try {
            ZimbraContext lc = getZimbraContext(context);
            OperationContext octxt = lc.getOperationContext();            
            Account acct = getRequestedAccount(lc);
            Mailbox mbox = getRequestedMailbox(lc);
            
            ParsedItemID pid = ParsedItemID.parse(request.getAttribute("id"));
            int compNum = (int)request.getAttributeLong("comp");
            
            sLog.info("<CreateAppointmentException id="+pid+" comp="+compNum+">");

            synchronized(mbox) {
                Appointment appt = mbox.getAppointmentById(pid.getItemIDInt()); 
                Invite inv = appt.getInvite(pid.getSubIdInt(), compNum);
                
                if (inv.hasRecurId()) {
                    throw ServiceException.FAILURE("Invite id="+pid+" comp="+compNum+" is not the a default invite", null);
                }
                
                if (appt == null)
                    throw ServiceException.FAILURE("Could not find Appointment for id="+pid+" comp="+compNum+">", null);
                else if (!appt.isRecurring())
                    throw ServiceException.FAILURE("Appointment "+appt.getId()+" is not a recurring appointment", null);

                // <M>
                Element msgElem = request.getElement(MailService.E_MSG);
                
                CalSendData dat = handleMsgElement(octxt, msgElem, acct, mbox, new CreateApptExceptionInviteParser(appt.getUid(), inv.getTimeZoneMap()));
                
                Element response = lc.createElement(MailService.CREATE_APPOINTMENT_EXCEPTION_RESPONSE);            
                return sendCalendarMessage(octxt, acct, mbox, dat, response);
            }
            
        } finally {
            sWatch.stop(startTime);
        }
    }
}
