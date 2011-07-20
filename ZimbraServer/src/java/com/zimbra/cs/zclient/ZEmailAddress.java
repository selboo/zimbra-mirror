/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.json.JSONException;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

public class ZEmailAddress implements ToZJSONObject {

    public static final String EMAIL_TYPE_BCC = "b";
    public static final String EMAIL_TYPE_CC = "c";
    public static final String EMAIL_TYPE_FROM = "f";
    public static final String EMAIL_TYPE_SENDER = "s";
    public static final String EMAIL_TYPE_TO = "t";
    public static final String EMAIL_TYPE_REPLY_TO = "r";


    private String address;
    private String display;
    private String personal;
    private String type;

    public ZEmailAddress(String address, String display, String personal, String type) {
        this.address = address;
        this.display = display;
        this.personal = personal;
        this.type = type;
    }

    public ZEmailAddress(Element e) {
        address = e.getAttribute(MailConstants.A_ADDRESS, null);
        display = e.getAttribute(MailConstants.A_DISPLAY, null);
        personal = e.getAttribute(MailConstants.A_PERSONAL, null);
        type = e.getAttribute(MailConstants.A_TYPE, "");
    }

    /**
     * (f)rom, t(o), c(c), (s)ender, (r)eply-to, b(cc). Type is only sent when an individual message message is returned. In the
     * list of conversations, all the email addresseses returned for a conversation are a subset
     * of the participants. In the list of messages in a converstation, the email addressses are
     * the senders.
     */
    public String getType() {
        return type;
    }

    /**
     * the comment/name part of an address
     */
    public String getPersonal() {
        return personal;
    }

    /**
     * the user@domain part of an address
     */
    public String getAddress() {
        return address;
    }

    /**
     * if we have personal, first word in "word1 word2" format, or last word in "word1, word2" format.
     * if no personal, take string before "@" in email-address.
     */
    public String getDisplay() {
        return display;
    }

    private String quoteAddress(String addr) {
        if (addr == null)
            return "";
        else if (addr.startsWith("<"))
            return addr;
        else
            return "<" + addr +">";
    }

    public String getFullAddressQuoted() {
        if (personal == null) {
            return quoteAddress(address);
        } else {
            String p = personal;
            if (p.indexOf("\"") != -1)
                p = p.replaceAll("\"", "\\\"");
            return "\"" + p + "\" "+ quoteAddress(address);
        }
    }

    public String getFullAddress() {
        try {
            if (personal == null)
                return address;
            else
                return new JavaMailInternetAddress(address, personal).toUnicodeString();
        } catch (UnsupportedEncodingException e) {
            if (personal == null)
                return address;
            else {
                String p = personal;
                if (p.indexOf("\"") != -1)
                    p = p.replaceAll("\"", "\\\"");
                return p + " "+getAddress();
            }
        }
    }

    public boolean  isBcc()        { return ZEmailAddress.EMAIL_TYPE_BCC.equals(getType()); }
    public boolean  isCc()         { return ZEmailAddress.EMAIL_TYPE_CC.equals(getType()); }
    public boolean  isFrom()       { return ZEmailAddress.EMAIL_TYPE_FROM.equals(getType()); }
    public boolean  isSender()     { return ZEmailAddress.EMAIL_TYPE_SENDER.equals(getType()); }
    public boolean  isTo()         { return ZEmailAddress.EMAIL_TYPE_TO.equals(getType()); }
    public boolean  isReplyTo()    { return ZEmailAddress.EMAIL_TYPE_REPLY_TO.equals(getType()); }

    @Override
    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("address", address);
        jo.put("display", display);
        jo.put("personal", personal);
        jo.put("type", type);
        jo.put("fullAddressQuoted", getFullAddressQuoted());
        return jo;
    }

    @Override
    public String toString() {
        return String.format("[ZEmailAddress %s]", getFullAddressQuoted());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    /**
    *
    * @param type type of addresses to create in the returned list.
    * @see com.zimbra.cs.zclient.ZEmailAddress EMAIL_TYPE_TO, etc.
    * @return list of ZEMailAddress obejcts.
    * @throws ServiceException
    */
    public static List<ZEmailAddress> parseAddresses(String line, String type) throws ServiceException {
        try {
            line = line.replace(";", ",");
            InternetAddress[] inetAddrs = JavaMailInternetAddress.parseHeader(line, false);
            List<ZEmailAddress> result = new ArrayList<ZEmailAddress>(inetAddrs.length);
            for (InternetAddress ia : inetAddrs) {
                result.add(new ZEmailAddress(ia.getAddress().replaceAll("\"",""), null, ia.getPersonal(), type));
            }
            return result;
        } catch (AddressException e) {
            throw ServiceException.INVALID_REQUEST("Couldn't parse address", e);
        }
    }
}
