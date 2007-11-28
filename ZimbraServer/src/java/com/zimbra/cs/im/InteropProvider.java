/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
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
package com.zimbra.cs.im;

import java.util.List;

import org.xmpp.packet.JID;

import com.zimbra.common.service.ServiceException;

/**
 * 
 */
public interface InteropProvider {
    List<String> getAvailableServices();
    GatewayRegistrationStatus getRegistrationStatus(String service, JID jid) throws ServiceException; 
    
    void connectUser(String serviceName, JID jid, String username, String password) throws ServiceException;
    void disconnectUser(String serviceName, JID jid) throws ServiceException;
    void reconnectUser(String serviceName, JID jid) throws ServiceException;
}
