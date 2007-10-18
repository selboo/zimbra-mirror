/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Sep 29, 2004
 */
package com.zimbra.cs.service.mail;

import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ExceptionToString;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailSender.SafeSendFailedException;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.BlobDataSource;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ContentServlet;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.service.UploadDataSource;
import com.zimbra.cs.service.formatter.VCard;
import com.zimbra.cs.service.mail.ToXML.EmailType;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.JMSession;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.SendFailedException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.MailConstants;


/**
 * @author tim
 */
public class ParseMimeMessage {

    private static Log mLog = LogFactory.getLog(ParseMimeMessage.class);

    private static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;

    public static MimeMessage importMsgSoap(Element msgElem) throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailConstants.E_MSG));

        Element contentElement = msgElem.getElement(MailConstants.E_CONTENT);

        byte[] content = contentElement.getText().getBytes();
        long maxSize = Provisioning.getInstance().getLocalServer().getLongAttr(Provisioning.A_zimbraFileUploadMaxSize, DEFAULT_MAX_SIZE);
        if (content.length > maxSize)
            throw ServiceException.INVALID_REQUEST("inline message too large", null);

        ByteArrayInputStream messageStream = new ByteArrayInputStream(content);
        try {
            return new Mime.FixedMimeMessage(JMSession.getSession(), messageStream);
        } catch (MessagingException me) {
            mLog.warn(ExceptionToString.ToString(me));
            throw ServiceException.FAILURE("MessagingExecption", me);
        }
    }

    /**
     * @author tim
     * 
     * Callback routine for parsing the <inv> element and building a iCal4j Calendar from it
     * 
     *  We use a callback b/c there are differences in the parsing depending on the operation: 
     *  Replying to an invite is different than Creating or Modifying one, etc etc...
     *
     */
    static abstract class InviteParser {
        abstract protected InviteParserResult parseInviteElement(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element invElement) throws ServiceException;

        public final InviteParserResult parse(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element invElement) throws ServiceException {
            mResult = parseInviteElement(zsc, octxt, account, invElement);
            return mResult;
        }

        private InviteParserResult mResult;
        public InviteParserResult getResult() { return mResult; }
    }

    static class InviteParserResult {
        public ZCalendar.ZVCalendar mCal;
        public String mUid;
        public String mSummary;
        public Invite mInvite;
    }

    // by default, no invite allowed
    static InviteParser NO_INV_ALLOWED_PARSER = new InviteParser() {
        public InviteParserResult parseInviteElement(ZimbraSoapContext zsc, OperationContext octxt, Account account, Element inviteElem)
        throws ServiceException {
            throw ServiceException.INVALID_REQUEST("No <inv> element allowed for this request", null);
        }
    };


    /**
     * Wrapper class for data parsed out of the mime message
     */
    public static class MimeMessageData {
        public List<InternetAddress> newContacts = new ArrayList<InternetAddress>();
        public List<Upload> uploads = null;    // NULL unless there are attachments
        public String iCalUUID = null;         // NULL unless there is an iCal part
    }

    public static MimeMessage parseMimeMsgSoap(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox,
                                               Element msgElem, MimeBodyPart[] additionalParts, MimeMessageData out)
    throws ServiceException {
        return parseMimeMsgSoap(zsc, octxt, mbox, msgElem, additionalParts, NO_INV_ALLOWED_PARSER, out);
    }

    // Recursively find and return the content of the first text/plain part.
    public static String getTextPlainContent(Element elem) {
        if (elem == null) return null;
        if (MailConstants.E_MSG.equals(elem.getName())) {
            elem = elem.getOptionalElement(MailConstants.E_MIMEPART);
            if (elem == null) return null;
        }
        String type = elem.getAttribute(MailConstants.A_CONTENT_TYPE, Mime.CT_DEFAULT).trim().toLowerCase();
        if (type.equals(Mime.CT_DEFAULT)) {
            return elem.getAttribute(MailConstants.E_CONTENT, null);
        } else if (type.startsWith(Mime.CT_MULTIPART_PREFIX)) {
            for (Element childElem : elem.listElements(MailConstants.E_MIMEPART)) {
                String text = getTextPlainContent(childElem);
                if (text != null)
                    return text;
            }
        }
        return null;
    }


    /** Class encapsulating common data passed among methods. */
    private static class ParseMessageContext {
        MimeMessageData out;
        ZimbraSoapContext zsc;
        OperationContext octxt;
        Mailbox mbox;
        boolean use2231;
        String defaultCharset;
        long size;
        long maxSize;
        
        ParseMessageContext() {
            try {
                Config config = Provisioning.getInstance().getConfig();
                maxSize = config.getIntAttr(Provisioning.A_zimbraMtaMaxMessageSize, -1);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.warn("Unable to determine max message size.  Disabling limit check.", e);
            }
            if (maxSize < 0) {
                maxSize = Long.MAX_VALUE;
            }
        }
        
        void incrementSize(long numBytes) throws MailServiceException {
            size += numBytes;
            if (size > maxSize) {
                throw MailServiceException.MESSAGE_TOO_BIG(maxSize);
            }
        }
    }

    /**
     * Given an <m> element from SOAP, return us a parsed MimeMessage, 
     * and also fill in the MimeMessageData structure with information we parsed out of it (e.g. contained 
     * Invite, msgids, etc etc)
     * @param zsc TODO
     * @param octxt TODO
     * @param mbox
     * @param msgElem the <m> element
     * @param additionalParts - MimeBodyParts that we want to have added to the MimeMessage (ie things the server is adding onto the message)
     * @param inviteParser Callback which handles <inv> embedded invite components
     * @param out Holds info about things we parsed out of the message that the caller might want to know about
     * @return
     * @throws ServiceException
     */
    public static MimeMessage parseMimeMsgSoap(ZimbraSoapContext zsc, OperationContext octxt, Mailbox mbox, Element msgElem,
                                               MimeBodyPart[] additionalParts, InviteParser inviteParser, MimeMessageData out)
    throws ServiceException {
        /* msgElem == "<m>" E_MSG */
        assert(msgElem.getName().equals(MailConstants.E_MSG));

        Account target = DocumentHandler.getRequestedAccount(zsc);
        ParseMessageContext ctxt = new ParseMessageContext();
        ctxt.out = out;
        ctxt.zsc = zsc;
        ctxt.octxt = octxt;
        ctxt.mbox = mbox;
        ctxt.use2231 = target.getBooleanAttr(Provisioning.A_zimbraPrefUseRfc2231, false);
        ctxt.defaultCharset = target.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, Mime.P_CHARSET_UTF8);
        if (ctxt.defaultCharset.equals(""))
            ctxt.defaultCharset = Mime.P_CHARSET_UTF8;

        try {
            MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession());
            MimeMultipart mmp = null;

            Element partElem   = msgElem.getOptionalElement(MailConstants.E_MIMEPART);
            Element attachElem = msgElem.getOptionalElement(MailConstants.E_ATTACH);
            Element inviteElem = msgElem.getOptionalElement(MailConstants.E_INVITE);

            boolean hasContent  = (partElem != null || inviteElem != null || additionalParts != null);
            boolean isMultipart = (attachElem != null); // || inviteElem != null || additionalParts!=null);
            if (isMultipart) {
                mmp = new MimeMultipart("mixed");  // may need to change to "digest" later
                mm.setContent(mmp);
            }

            // Grab the <inv> part now so we can stick it in a multipart/alternative if necessary
            MimeBodyPart[] alternatives = null;

            if (inviteElem != null) {
                int additionalLen = 0;
                if (additionalParts != null)
                    additionalLen += additionalParts.length;
                alternatives = new MimeBodyPart[additionalLen+1];
                int curAltPart = 0;

                // goes into the "content" subpart
                InviteParserResult result = inviteParser.parse(zsc, octxt, mbox.getAccount(), inviteElem);
                
                if (partElem != null && result.mCal != null) {
                    // If textual content is provided and there's an invite,
                    // set the text as DESCRIPTION of the iCalendar.  This helps
                    // clients that ignore alternative text content and only
                    // displays the DESCRIPTION specified in the iCalendar part.
                    // (e.g. MS Entourage for Mac)
                    String desc = getTextPlainContent(partElem);
                    if (desc != null && desc.length() > 0) {
                        result.mCal.addDescription(desc);
                        if (result.mInvite != null)
                            result.mInvite.setFragment(Fragment.getFragment(desc, true));
                    }
                }
                MimeBodyPart mbp = CalendarMailSender.makeICalIntoMimePart(result.mUid, result.mCal);
                alternatives[curAltPart++] = mbp;

                if (additionalParts != null) {
                    for (int i = 0; i < additionalParts.length; i++)
                        alternatives[curAltPart++] = additionalParts[i];
                }
            } else {
                alternatives = additionalParts;
            }

            // handle the content from the client, if any
            if (hasContent)
                setContent(mm, mmp, partElem != null ? partElem : inviteElem, alternatives, ctxt);

            // attachments go into the toplevel "mixed" part
            if (isMultipart && attachElem != null)
                handleAttachments(attachElem, mmp, ctxt, null);

            // <m> attributes: id, f[lags], s[ize], d[ate], cid(conv-id), l(parent folder)
            // <m> child elements: <e> (email), <s> (subject), <f> (fragment), <mp>, <attach>
            MessageAddresses maddrs = new MessageAddresses(out.newContacts);
            for (Element elem : msgElem.listElements()) {
                String eName = elem.getName();
                if (eName.equals(MailConstants.E_ATTACH)) {
                    // ignore it...
                } else if (eName.equals(MailConstants.E_MIMEPART)) { /* <mp> */
                    // processMessagePart(mm, elem);
                } else if (eName.equals(MailConstants.E_EMAIL)) { /* <e> */
                    maddrs.add(elem, ctxt.defaultCharset);
                } else if (eName.equals(MailConstants.E_IN_REPLY_TO)) { /* <irt> */
                    // mm.setHeader("In-Reply-To", elem.getText());
                } else if (eName.equals(MailConstants.E_SUBJECT)) { /* <su> */
                    // mm.setSubject(elem.getText(), "utf-8");
                } else if (eName.equals(MailConstants.E_FRAG)) { /* <f> */
                    mLog.debug("Ignoring message fragment data");
                } else if (eName.equals(MailConstants.E_INVITE)) { /* <inv> */
                    // Already processed above.  Ignore it.
                } else if (eName.equals(MailConstants.E_CAL_TZ)) { /* <tz> */
                    // Ignore as a special case.
                } else {
                    mLog.warn("unsupported child element '" + elem.getName() + "' under parent " + msgElem.getName());
                }
            }

            // deal with things that can be either <m> attributes or subelements
            String subject = msgElem.getAttribute(MailConstants.E_SUBJECT, "");
            mm.setSubject(subject, StringUtil.checkCharset(subject, ctxt.defaultCharset));

            String irt = msgElem.getAttribute(MailConstants.E_IN_REPLY_TO, null);
            if (irt != null)
                mm.setHeader("In-Reply-To", irt);

            // can have no addresses specified if it's a draft...
            if (!maddrs.isEmpty())
                addAddressHeaders(mm, maddrs, ctxt.defaultCharset);

            if (!hasContent && !isMultipart)
                mm.setText("", Mime.P_CHARSET_DEFAULT);

            String flagStr = msgElem.getAttribute(MailConstants.A_FLAGS, "");
            if (flagStr.indexOf(mbox.mUrgentFlag.getAbbreviation()) != -1) {
                mm.addHeader("X-Priority", "1");
                mm.addHeader("Importance", "high");
            } else if (flagStr.indexOf(mbox.mBulkFlag.getAbbreviation()) != -1) {
                mm.addHeader("X-Priority", "5");
                mm.addHeader("Importance", "low");
            }

            // JavaMail tip: don't forget to call this, it is REALLY confusing.  
            mm.saveChanges();

            if (mLog.isDebugEnabled())
                dumpMessage(mm);

            return mm;
        } catch (UnsupportedEncodingException encEx) {
            String excepStr = ExceptionToString.ToString(encEx);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("UnsupportedEncodingExecption", encEx);
        } catch (SendFailedException sfe) {
            SafeSendFailedException ssfe = new SafeSendFailedException(sfe);
            String excepStr = ExceptionToString.ToString(ssfe);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("SendFailure", ssfe);
        } catch (MessagingException me) {
            String excepStr = ExceptionToString.ToString(me);
            mLog.warn(excepStr);
            throw ServiceException.FAILURE("MessagingExecption", me);
        } catch (IOException e) {
            e.printStackTrace();
            throw ServiceException.FAILURE("IOExecption", e);
        }
    }

    private static void handleAttachments(Element attachElem, MimeMultipart mmp, ParseMessageContext ctxt, String contentID)
    throws ServiceException, MessagingException, IOException {
        if (contentID != null)
            contentID = '<' + contentID + '>';

        String attachIds = attachElem.getAttribute(MailConstants.A_ATTACHMENT_ID, null);
        if (attachIds != null) {
            List<Upload> uploads = attachUploads(mmp, attachIds, contentID, ctxt);
            if (ctxt.out.uploads == null)
                ctxt.out.uploads = uploads;
            else
                ctxt.out.uploads.addAll(uploads);
        }

        for (Element elem : attachElem.listElements()) {
            String eName = elem.getName();
            if (eName.equals(MailConstants.E_MIMEPART)) {
                ItemId iid = new ItemId(elem.getAttribute(MailConstants.A_MESSAGE_ID), (String) null);
                String part = elem.getAttribute(MailConstants.A_PART);
                if (!iid.hasSubpart()) {
                    attachPart(mmp, ctxt.mbox.getMessageById(ctxt.octxt, iid.getId()), part, contentID, ctxt);
                } else {
                    CalendarItem calItem = ctxt.mbox.getCalendarItemById(ctxt.octxt, iid.getId());
                    MimeMessage calMm = calItem.getSubpartMessage(iid.getSubpartId());
                    MimePart calMp = Mime.getMimePart(calMm, part);
                    if (calMp == null)
                        throw MailServiceException.NO_SUCH_PART(part);
                    attachPart(mmp, calMp, contentID, ctxt);
                }
            } else if (eName.equals(MailConstants.E_MSG)) {
                int messageId = (int) elem.getAttributeLong(MailConstants.A_ID);
                attachMessage(mmp, ctxt.mbox.getMessageById(ctxt.octxt, messageId), contentID, ctxt);
            } else if (eName.equals(MailConstants.E_CONTACT)) {
                int contactId = (int) elem.getAttributeLong(MailConstants.A_ID);
                attachContact(mmp, ctxt.mbox.getContactById(ctxt.octxt, contactId), contentID, ctxt);
            }
        }
    }

    /**
     * The <mp>'s from the client and the MimeBodyParts in alternatives[] all want to be "content"
     * of this MimeMessage.  The alternatives[] all need to be "alternative" to whatever the client sends
     * us....but we want to be careful so that we do NOT create a nested multipart/alternative structure
     * within another one (that would be very tacky)....so this is a bit complicated.
     * 
     * @param mm
     * @param mmp
     * @param elem
     * @param alternatives
     * @param defaultCharset TODO
     * @throws MessagingException
     * @throws IOException 
     * @throws ServiceException 
     */
    private static void setContent(MimeMessage mm, MimeMultipart mmp, Element elem, MimeBodyPart[] alternatives, ParseMessageContext ctxt)
    throws MessagingException, ServiceException, IOException {
        String type = elem.getAttribute(MailConstants.A_CONTENT_TYPE, Mime.CT_DEFAULT).trim();
        ContentType ctype = new ContentType(type, ctxt.use2231);

        // is the client passing us a multipart?
        if (ctype.getPrimaryType().equals("multipart")) {
            // handle multipart content separately...
            setMultipartContent(ctype.getSubType(), mm, mmp, elem, alternatives, ctxt);
            return;
        }

        Element inline = elem.getOptionalElement(MailConstants.E_ATTACH);
        if (inline != null) {
            handleAttachments(inline, mmp, ctxt, elem.getAttribute(MailConstants.A_CONTENT_ID, null));
            return;
        }

        // a single part from the client...we might still have to create a multipart/alternative if
        // there are alternatives[] passed-in, but still this is fairly straightforward...

        if (alternatives != null) {
            // create a multipart/alternative to hold all the alternatives
            MimeMultipart mmpNew = new MimeMultipart("alternative");
            if (mmp == null) {
                mm.setContent(mmpNew);
            } else {
                MimeBodyPart mbpWrapper = new MimeBodyPart();
                mbpWrapper.setContent(mmpNew);
                mmp.addBodyPart(mbpWrapper);
            }
            mmp = mmpNew;
        }

        // once we get here, mmp is either NULL, a multipart/mixed from the toplevel, 
        // or a multipart/alternative created just above....either way we are safe to stick
        // the client's nice and simple body right here
        String data = elem.getAttribute(MailConstants.E_CONTENT, "");
        ctxt.incrementSize(data.getBytes().length);

        // if the user has specified an alternative charset, make sure it exists and can encode the content
        String charset = StringUtil.checkCharset(data, ctxt.defaultCharset);
        ctype.setParameter(Mime.P_CHARSET, charset);

        if (mmp != null) {
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setText(data, charset);
            mbp.setHeader("Content-Type", ctype.toString());
            mmp.addBodyPart(mbp);
        } else {
            mm.setText(data, charset);
            mm.setHeader("Content-Type", ctype.toString());
        }

        if (alternatives != null) {
            for (int i = 0; i < alternatives.length; i++) {
                ctxt.incrementSize(alternatives[i].getSize());
                mmp.addBodyPart(alternatives[i]);
            }
        }
    }

    private static void setMultipartContent(String subType, MimeMessage mm, MimeMultipart mmp, Element elem, MimeBodyPart[] alternatives, ParseMessageContext ctxt)
    throws MessagingException, ServiceException, IOException {
        // do we need to add a multipart/alternative for the alternatives?
        if (alternatives == null || subType.equals("alternative")) {
            // no need to add an extra multipart/alternative!

            // create the MimeMultipart and attach it to the existing structure:
            MimeMultipart mmpNew = new MimeMultipart(subType);
            if (mmp == null) {
                // there were no multiparts at all, we need to create one 
                mm.setContent(mmpNew);
            } else {
                // there was already a multipart/mixed at the top of the mm
                MimeBodyPart mbpWrapper = new MimeBodyPart();
                mbpWrapper.setContent(mmpNew);
                mmp.addBodyPart(mbpWrapper);
            }

            // add each part in turn (recursively) below
            for (Element subpart : elem.listElements())
                setContent(mm, mmpNew, subpart, null, ctxt);

            // finally, add the alternatives if there are any...
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length; i++) {
                    ctxt.incrementSize(alternatives[i].getSize());
                    mmpNew.addBodyPart(alternatives[i]);
                }
            }
        } else {
            // create a multipart/alternative to hold all the client's struct + the alternatives
            MimeMultipart mmpNew = new MimeMultipart("alternative");
            if (mmp == null) {
                mm.setContent(mmpNew);
            } else {
                MimeBodyPart mbpWrapper = new MimeBodyPart();
                mbpWrapper.setContent(mmpNew);
                mmp.addBodyPart(mbpWrapper);
            }

            // add the entire client's multipart/whatever here inside our multipart/alternative
            setContent(mm, mmpNew, elem, null, ctxt);

            // add all the alternatives
            if (alternatives != null) {
                for (int i = 0; i < alternatives.length; i++) {
                    ctxt.incrementSize(alternatives[i].getSize());
                    mmpNew.addBodyPart(alternatives[i]);
                }
            }
        }
    }

    private static List<Upload> attachUploads(MimeMultipart mmp, String attachIds, String contentID, ParseMessageContext ctxt)
    throws ServiceException, MessagingException {
        List<Upload> uploads = new ArrayList<Upload>();
        String[] uploadIds = attachIds.split(FileUploadServlet.UPLOAD_DELIMITER);

        for (int i = 0; i < uploadIds.length; i++) {
            Upload up = FileUploadServlet.fetchUpload(ctxt.zsc.getAuthtokenAccountId(), uploadIds[i], ctxt.zsc.getRawAuthToken());
            if (up == null)
                throw MailServiceException.NO_SUCH_UPLOAD(uploadIds[i]);
            
            // Make sure we haven't exceeded the max size
            ctxt.incrementSize((long) (up.getSize() * 1.33));
            uploads.add(up);

            // scan upload for viruses
            StringBuffer info = new StringBuffer();
            UploadScanner.Result result = UploadScanner.accept(up, info);
            if (result == UploadScanner.REJECT)
                throw MailServiceException.UPLOAD_REJECTED(up.getName(), info.toString());
            if (result == UploadScanner.ERROR)
                throw MailServiceException.SCAN_ERROR(up.getName());
            String filename = up.getName();

            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setDataHandler(new DataHandler(new UploadDataSource(up)));

            String ctype = up.getContentType() == null ? Mime.CT_APPLICATION_OCTET_STREAM : up.getContentType();
            mbp.setHeader("Content-Type", new ContentType(ctype, ctxt.use2231).setParameter("name", filename).toString());
            mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, ctxt.use2231).setParameter("filename", filename).toString());
            mbp.setContentID(contentID);

            mmp.addBodyPart(mbp);
        }
        return uploads;
    }

    private static void attachMessage(MimeMultipart mmp, com.zimbra.cs.mailbox.Message message, String contentID, ParseMessageContext ctxt)
    throws MessagingException, ServiceException {
        ctxt.incrementSize(message.getSize());
        MailboxBlob blob = message.getBlob();

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new BlobDataSource(blob)));
        mbp.setHeader("Content-Type", blob.getMimeType());
        mbp.setHeader("Content-Disposition", Part.ATTACHMENT);
        mbp.setContentID(contentID);
        mmp.addBodyPart(mbp);
    }

    private static void attachContact(MimeMultipart mmp, com.zimbra.cs.mailbox.Contact contact, String contentID, ParseMessageContext ctxt)
    throws MessagingException, MailServiceException {
        ctxt.incrementSize(contact.getSize());
        VCard vcf = VCard.formatContact(contact);
        String filename = vcf.fn + ".vcf";
        String charset = StringUtil.checkCharset(vcf.formatted, ctxt.defaultCharset);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setText(vcf.formatted, charset);
        mbp.setHeader("Content-Type", new ContentType("text/x-vcard", ctxt.use2231).setParameter("name", filename).setParameter("charset", charset).toString());
        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, ctxt.use2231).setParameter("filename", filename).toString());
        mbp.setContentID(contentID);
        mmp.addBodyPart(mbp);
    }

    // subclass of MimePartDataSource that cleans up Content-Type headers before returning them so JavaMail doesn't barf
    private static class FixedMimePartDataSource implements DataSource {
        private final MimePart mMimePart;

        private FixedMimePartDataSource(MimePart mimePart) {
            mMimePart = mimePart;
        }

        public String getName() {
            return Mime.getFilename(mMimePart);
        }
        public String getContentType() {
            try {
                return new ContentType(mMimePart.getContentType()).toString();
            } catch (MessagingException e) {
                return Mime.CT_APPLICATION_OCTET_STREAM;
            }
        }
        public InputStream getInputStream() throws IOException {
            try {
                return mMimePart.getInputStream();
            } catch (MessagingException e) {
                IOException ioe = new IOException(e.getMessage());
                ioe.initCause(e);
                throw ioe;
            }
        }
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
    }

    private static void attachPart(MimeMultipart mmp, com.zimbra.cs.mailbox.Message message, String part, String contentID, ParseMessageContext ctxt)
    throws IOException, MessagingException, ServiceException {
        MimePart mp = ContentServlet.getMimePart(message, part);
        if (mp == null)
            throw MailServiceException.NO_SUCH_PART(part);
        ctxt.incrementSize((long) (mp.getSize() * 1.33));
        String filename = Mime.getFilename(mp);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new FixedMimePartDataSource(mp)));

        String ctype = mp.getContentType();
        if (ctype != null)
            mbp.setHeader("Content-Type", new ContentType(ctype, ctxt.use2231).setParameter("name", filename).toString());

        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, ctxt.use2231).setParameter("filename", filename).toString());

        String desc = mp.getDescription();
        if (desc != null)
            mbp.setHeader("Content-Description", desc);

        mbp.setContentID(contentID);

        mmp.addBodyPart(mbp);
    }

    private static void attachPart(MimeMultipart mmp, MimePart mp, String contentID, ParseMessageContext ctxt)
    throws MessagingException, MailServiceException {
        ctxt.incrementSize((long) (mp.getSize() * 1.33));
        String filename = Mime.getFilename(mp);

        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setDataHandler(new DataHandler(new FixedMimePartDataSource(mp)));

        String ctype = mp.getContentType();
        if (ctype != null)
            mbp.setHeader("Content-Type", new ContentType(ctype, ctxt.use2231).setParameter("name", filename).toString());

        mbp.setHeader("Content-Disposition", new ContentDisposition(Part.ATTACHMENT, ctxt.use2231).setParameter("filename", filename).toString());

        String desc = mp.getDescription();
        if (desc != null)
            mbp.setHeader("Content-Description", desc);

        mbp.setContentID(contentID);

        mmp.addBodyPart(mbp);
    }


    private static final class MessageAddresses {
        private final HashMap<String, Object> addrs = new HashMap<String, Object>();
        private final List<InternetAddress> newContacts;

        MessageAddresses(List<InternetAddress> contacts) {
            newContacts = contacts;
        }

        @SuppressWarnings("unchecked")
        public void add(Element elem, String defaultCharset) throws ServiceException, UnsupportedEncodingException {
            String emailAddress = IDNUtil.toAscii(elem.getAttribute(MailConstants.A_ADDRESS));
            String personalName = elem.getAttribute(MailConstants.A_PERSONAL, null);
            String addressType = elem.getAttribute(MailConstants.A_ADDRESS_TYPE);

            InternetAddress addr = new InternetAddress(emailAddress, personalName, StringUtil.checkCharset(personalName, defaultCharset));
            if (elem.getAttributeBool(MailConstants.A_ADD_TO_AB, false))
                newContacts.add(addr);

            Object content = addrs.get(addressType);
            if (content == null || addressType.equals(EmailType.FROM.toString()) || addressType.equals(EmailType.SENDER.toString())) {
                addrs.put(addressType, addr);
            } else if (content instanceof List) {
                ((List<InternetAddress>) content).add(addr);
            } else {
                List<InternetAddress> list = new ArrayList<InternetAddress>();
                list.add((InternetAddress) content);
                list.add(addr);
                addrs.put(addressType, list);
            }
        }

        public InternetAddress[] get(String addressType) {
            Object content = addrs.get(addressType);
            if (content == null) {
                return null;
            } else if (content instanceof InternetAddress) {
                return new InternetAddress[] { (InternetAddress) content };
            } else {
                ArrayList list = (ArrayList) content;
                InternetAddress[] result = new InternetAddress[list.size()];
                for (int i = 0; i < list.size(); i++)
                    result[i] = (InternetAddress) list.get(i);
                return result;
            }
        }

        public boolean isEmpty() {
            return addrs.isEmpty();
        }
    }

    private static void addAddressHeaders(MimeMessage mm, MessageAddresses maddrs, String defaultCharset)
    throws MessagingException {
        InternetAddress[] addrs = maddrs.get(EmailType.TO.toString());
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.TO, addrs);
            mLog.debug("\t\tTO: " + Arrays.toString(addrs));
        }

        addrs = maddrs.get(EmailType.CC.toString());
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.CC, addrs);
            mLog.debug("\t\tCC: " + Arrays.toString(addrs));
        }

        addrs = maddrs.get(EmailType.BCC.toString());
        if (addrs != null) {
            mm.addRecipients(Message.RecipientType.BCC, addrs);
            mLog.debug("\t\tBCC: " + Arrays.toString(addrs));
        }

        addrs = maddrs.get(EmailType.FROM.toString());
        if (addrs != null && addrs.length == 1) {
            mm.setFrom(addrs[0]);
            mLog.debug("\t\tFrom: " + addrs[0]);
        }

        addrs = maddrs.get(EmailType.SENDER.toString());
        if (addrs != null && addrs.length == 1) {
            mm.setSender(addrs[0]);
            mLog.debug("\t\tSender: " + addrs[0]);
        }

        addrs = maddrs.get(EmailType.REPLY_TO.toString());
        if (addrs != null && addrs.length > 0) {
            mm.setReplyTo(addrs);
            mLog.debug("\t\tReply-To: " + addrs[0]);
        }
    }

    private static void dumpMessage(MimeMessage mm) {
        /* 
         * Dump the outgoing message to stdout for now...
         */
        mLog.debug("--------------------------------------");
        try {
            Enumeration hdrsEnum = mm.getAllHeaders();
            if (hdrsEnum != null)
                while (hdrsEnum.hasMoreElements()) {
                    Header hdr = (Header) hdrsEnum.nextElement();
                    if (!hdr.getName().equals("") && !hdr.getValue().equals("\n"))
                        mLog.debug(hdr.getName()+" = \""+hdr.getValue()+"\"");
                }
            mLog.debug("--------------------------------------");
            javax.mail.Address[] recips = mm.getAllRecipients();
            if (recips != null)
                for (int i = 0; i < recips.length; i++)
                    mLog.debug("Recipient: "+recips[i].toString());

            mLog.debug("--------------------------------------\nMessage size is: "+mm.getSize());

//          System.out.println("--------------------------------------");
//          System.out.print("Message Dump:");
//          mm.writeTo(System.out);
        } catch (Exception e) { e.printStackTrace(); };
        mLog.debug("--------------------------------------\n");
    }

    public static void main(String[] args) throws ServiceException, IOException, MessagingException, com.zimbra.cs.account.AuthTokenException {
        Element m = new Element.JSONElement(MailConstants.E_MSG);
        m.addAttribute(MailConstants.E_SUBJECT, "dinner appt");
        m.addUniqueElement(MailConstants.E_MIMEPART).addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain").addAttribute(MailConstants.E_CONTENT, "foo bar");
        m.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS_TYPE, EmailType.TO.toString()).addAttribute(MailConstants.A_ADDRESS, "test@localhost");
        System.out.println(m.prettyPrint());

        Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.name, "user1");
        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put(com.zimbra.soap.SoapServlet.ZIMBRA_AUTH_TOKEN, new com.zimbra.cs.account.AuthToken(acct).getEncoded());
        ZimbraSoapContext zsc = new ZimbraSoapContext(null, context, com.zimbra.common.soap.SoapProtocol.SoapJS);
        OperationContext octxt = new OperationContext(acct);

        MimeMessage mm = parseMimeMsgSoap(zsc, octxt, null, m, null, new MimeMessageData());
        mm.writeTo(System.out);
    }
}
