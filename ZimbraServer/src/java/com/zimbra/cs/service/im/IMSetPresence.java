/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.im;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.SoapFaultException;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.cs.mailbox.im.IMPersona;
import com.zimbra.cs.mailbox.im.IMPresence;
import com.zimbra.cs.mailbox.im.IMRouter;

public class IMSetPresence extends DocumentHandler {

    @Override
    public Element handle(Element request, Map context) throws ServiceException, SoapFaultException 
    {
        ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = super.getRequestedMailbox(lc);

        Element response = lc.createElement(IMService.IM_SET_PRESENCE_RESPONSE);
        
        Element e = request.getElement("presence");
        
        String showStr = e.getAttribute("show", IMPresence.Show.ONLINE.toString());
        String statusStr = null;
        Element status = e.getOptionalElement("status");
        if (status != null) {
            statusStr = status.getText();
        }
        
        IMPresence presence = new IMPresence(IMPresence.Show.valueOf(showStr.toUpperCase()), (byte)1, statusStr);
        
        Mailbox.OperationContext oc = lc.getOperationContext();
        synchronized(mbox) {
            IMPersona persona = IMRouter.getInstance().findPersona(oc, mbox, true);
            persona.setMyPresence(oc, presence);
        }
        
        return response;
    }
}
