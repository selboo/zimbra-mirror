/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;

/**
 * Abstraction of DavResource that maps to MailItem in the mailbox.
 * Supports the dead properties that can be saved for DavResource.
 * 
 * @author jylee
 *
 */
public abstract class MailItemResource extends DavResource {
	protected int  mFolderId;
	protected int  mId;
	protected byte mType;
	protected byte mColor;
	protected String mEtag;
	protected String mSubject;
	protected String mPath;
	protected long mModifiedDate;
	protected String mOwnerId;
	protected Map<QName,Element> mDeadProps;
	
	private static final String CONFIG_KEY = "caldav";
	private static final int PROP_LENGTH_LIMIT = 1024;
    private static final String TEXT_PLAIN = "text/plain";
	
    private static final String BLUE   = "#0252D4FF";
    private static final String CYAN   = "#008284FF";
    private static final String GREEN  = "#2CA10BFF";
    private static final String PURPLE = "#492BA1FF";
    private static final String RED    = "#E51717FF";
    private static final String YELLOW = "#848200FF";
    private static final String PINK   = "#B027AEFF";
    private static final String GRAY   = "#848284FF";
    private static final String ORANGE = "#F57802FF";
    private static final String DEFAULT_COLOR = ORANGE;
    
    protected static final String[] COLOR_MAP = {
    	DEFAULT_COLOR, BLUE, CYAN, GREEN, PURPLE, RED, YELLOW, PINK, GRAY, ORANGE
    };
    
    protected static final ArrayList<String> COLOR_LIST = new ArrayList<String>();
    
    static {
    	Collections.addAll(COLOR_LIST, COLOR_MAP);
    }
    
	public MailItemResource(DavContext ctxt, MailItem item) throws ServiceException {
		this(ctxt, getItemPath(ctxt, item), item);
	}
	
	public MailItemResource(DavContext ctxt, String path, MailItem item) throws ServiceException {
		super(path, ctxt.getUser());
		mFolderId = item.getFolderId();
		mId = item.getId();
		mType = item.getType();
		mEtag = MailItemResource.getEtag(item);
		mSubject = item.getSubject();
		mPath = item.getPath();
		mModifiedDate = item.getChangeDate();
		mOwnerId = item.getAccount().getId();
		try {
			mDeadProps = getDeadProps(ctxt, item);
		} catch (Exception e) {
			ZimbraLog.dav.warn("can't get dead properties for MailItem id="+mId, e);
		}
		setProperty(DavElements.P_GETETAG, mEtag);
		if (mModifiedDate > 0)
            setLastModifiedDate(mModifiedDate);
		mColor = item.getColor();
		if (mColor >= COLOR_MAP.length)
			mColor = 0;
		setProperty(DavElements.E_DISPLAYNAME, item.getName());
		addProperty(Acl.getPrincipalUrl(this));
	}
	
	public MailItemResource(String path, String acct) {
		super(path, acct);
	}
	
	private static String getItemPath(DavContext ctxt, MailItem item) throws ServiceException {
		String path = ctxt.getCollectionPath();
		if (path != null)
			path += item.getName();
		else
			path = ctxt.getPath();
		if ((item.getType() == MailItem.TYPE_FOLDER || item.getType() == MailItem.TYPE_MOUNTPOINT) && !path.endsWith("/"))
			return path + "/";
		return path;
	}

	public boolean hasEtag() {
		return true;
	}
	
	@Override public String getEtag() {
		return mEtag;
	}
	
	protected Mailbox getMailbox(DavContext ctxt) throws ServiceException, DavException {
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.id, mOwnerId);
		if (account == null)
			throw new DavException("no such account "+mOwnerId, HttpServletResponse.SC_NOT_FOUND, null);
		return MailboxManager.getInstance().getMailboxByAccount(account);
	}
	
	/* Deletes this resource. */
	public void delete(DavContext ctxt) throws DavException {
		if (mId == 0) 
			throw new DavException("cannot delete resource", HttpServletResponse.SC_FORBIDDEN, null);
		try {
			Mailbox mbox = getMailbox(ctxt);
			mbox.move(ctxt.getOperationContext(), mId, MailItem.TYPE_UNKNOWN, Mailbox.ID_FOLDER_TRASH);
		} catch (ServiceException se) {
			int resCode = se instanceof MailServiceException.NoSuchItemException ?
					HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
			throw new DavException("cannot delete item", resCode, se);
		}
	}

	/* Moves this resource to another Collection. */
	public void move(DavContext ctxt, Collection dest) throws DavException {
		if (mFolderId == dest.getId())
			return;
		try {
			Mailbox mbox = getMailbox(ctxt);
			mbox.move(ctxt.getOperationContext(), mId, MailItem.TYPE_UNKNOWN, dest.getId());
		} catch (ServiceException se) {
			int resCode = se instanceof MailServiceException.NoSuchItemException ?
					HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
			throw new DavException("cannot move item", resCode, se);
		}
	}

	/* Copies this resource to another Collection. */
	public DavResource copy(DavContext ctxt, Collection dest) throws DavException {
		try {
			Mailbox mbox = getMailbox(ctxt);
			MailItem item = mbox.copy(ctxt.getOperationContext(), mId, MailItem.TYPE_UNKNOWN, dest.getId());
			return UrlNamespace.getResourceFromMailItem(ctxt, item);
		} catch (ServiceException se) {
			int resCode = se instanceof MailServiceException.NoSuchItemException ?
					HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
			throw new DavException("cannot copy item", resCode, se);
		}
	}

	/* Renames this resource. */
	public void rename(DavContext ctxt, String newName, DavResource destCollection) throws DavException {
		if (!(destCollection instanceof MailItemResource))
			return;
		MailItemResource dest = (MailItemResource) destCollection;
		try {
			Mailbox mbox = getMailbox(ctxt);
			if (isCollection()) {
				mbox.rename(ctxt.getOperationContext(), mId, MailItem.TYPE_FOLDER, newName, dest.mId);
			} else {
                mbox.rename(ctxt.getOperationContext(), mId, mType, newName, dest.mId);
			}
		} catch (ServiceException se) {
			int resCode = se instanceof MailServiceException.NoSuchItemException ?
					HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
			throw new DavException("cannot rename item", resCode, se);
		}
	}
	
	public int getId() {
		return mId;
	}
	
	private Map<QName,Element> getDeadProps(DavContext ctxt, MailItem item) throws DocumentException, IOException, DavException, ServiceException {
		HashMap<QName,Element> props = new HashMap<QName,Element>();
		Mailbox mbox = item.getMailbox();
		Metadata data = mbox.getConfig(ctxt.getOperationContext(), CONFIG_KEY);
		if (data == null)
			return props;
		String configVal = data.get(Integer.toString(item.getId()), null);
		if (configVal == null)
			return props;
		if (configVal.length() == 0)
			return props;
		ByteArrayInputStream in = new ByteArrayInputStream(configVal.getBytes("UTF-8"));
		org.dom4j.Document doc = com.zimbra.common.soap.Element.getSAXReader().read(in);
		Element e = doc.getRootElement();
		if (e == null)
			return props;
		for (Object obj : e.elements())
			if (obj instanceof Element) {
				Element elem = (Element) obj;
				elem.detach();
				props.put(elem.getQName(), elem);
			}
		return props;
	}

	/* Modifies the set of dead properties saved for this resource. 
	 * Properties in the parameter 'set' are added to the existing properties.
	 * Properties in 'remove' are removed.
	 */
	@Override
    public void patchProperties(DavContext ctxt, java.util.Collection<Element> set, java.util.Collection<QName> remove) throws DavException, IOException {
		for (QName n : remove)
				mDeadProps.remove(n);
		for (Element e : set) {
			QName name = e.getQName();
			if (name.equals(DavElements.E_DISPLAYNAME) &&
					(mType == MailItem.TYPE_FOLDER || mType == MailItem.TYPE_MOUNTPOINT)) {
				// rename folder
				try {
					String val = e.getText();
					String uri = getUri();
					Mailbox mbox = getMailbox(ctxt);
					mbox.rename(ctxt.getOperationContext(), mId, mType, val, mFolderId);
					setProperty(DavElements.P_DISPLAYNAME, val);
					UrlNamespace.addToRenamedResource(uri, this);
					UrlNamespace.addToRenamedResource(uri.substring(0, uri.length()-1), this);
				} catch (ServiceException se) {
					ctxt.getResponseProp().addPropError(DavElements.E_DISPLAYNAME, new DavException(se.getMessage(), DavProtocol.STATUS_FAILED_DEPENDENCY));
				}
				mDeadProps.remove(name);
				continue;
			} else if (name.equals(DavElements.E_CALENDAR_COLOR) &&
					(mType == MailItem.TYPE_FOLDER || mType == MailItem.TYPE_MOUNTPOINT)) {
				// change color
				String colorStr = e.getText();
				byte col = (byte) COLOR_LIST.indexOf(colorStr);
				if (col < 0) col = 0;
				try {
					Mailbox mbox = getMailbox(ctxt);
					mbox.setColor(ctxt.getOperationContext(), mId, mType, col);
				} catch (ServiceException se) {
					ctxt.getResponseProp().addPropError(DavElements.E_CALENDAR_COLOR, new DavException(se.getMessage(), DavProtocol.STATUS_FAILED_DEPENDENCY));
				}
				mDeadProps.remove(name);
				continue;
			}
			mDeadProps.put(name, e);
		}
		String configVal = "";
		if (mDeadProps.size() > 0) {
			org.dom4j.Document doc = org.dom4j.DocumentHelper.createDocument();
			Element top = doc.addElement(CONFIG_KEY);
			for (Map.Entry<QName,Element> entry : mDeadProps.entrySet())
				top.add(entry.getValue().detach());

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputFormat format = OutputFormat.createCompactFormat();
			XMLWriter writer = new XMLWriter(out, format);
			writer.write(doc);
			configVal = new String(out.toByteArray(), "UTF-8");
			
			if (configVal.length() > PROP_LENGTH_LIMIT)
				for (Map.Entry<QName,Element> entry : mDeadProps.entrySet())
					ctxt.getResponseProp().addPropError(entry.getKey(), new DavException("prop length exceeded", DavProtocol.STATUS_INSUFFICIENT_STORAGE));
		}
		try {
			Mailbox mbox = getMailbox(ctxt);
			synchronized (mbox) {
				Metadata data = mbox.getConfig(ctxt.getOperationContext(), CONFIG_KEY);
				if (data == null)
					data = new Metadata();
				data.put(Integer.toString(mId), configVal);
				mbox.setConfig(ctxt.getOperationContext(), CONFIG_KEY, data);
			}
		} catch (ServiceException se) {
			throw new DavException("unable to patch properties", HttpServletResponse.SC_FORBIDDEN, se);
		}
	}
	
	public ResourceProperty getProperty(QName prop) {
		ResourceProperty rp = null;
		if (mDeadProps != null) {
			Element e = mDeadProps.get(prop);
			if (e != null)
				rp = new ResourceProperty(e);
		}
		if (rp == null)
			rp = super.getProperty(prop);
		return rp;
	}

	protected boolean isWebRequest(DavContext ctxt) {
		String userAgent = ctxt.getRequest().getHeader(DavProtocol.HEADER_USER_AGENT);
		if (userAgent != null && 
				(userAgent.indexOf("MSIE") >= 0 ||
				 userAgent.indexOf("Mozilla") >= 0)) {
			return true;
		}
		return false;
	}

    @Override public boolean hasContent(DavContext ctxt) {
		if (isWebRequest(ctxt))
			return true;
		return super.hasContent(ctxt);
	}

    @Override public InputStream getContent(DavContext ctxt) throws IOException, DavException {
		if (isWebRequest(ctxt))
			return getTextContent(ctxt);
		return getRawContent(ctxt);
	}

    @Override public String getContentType(DavContext ctxt) {
        if (isWebRequest(ctxt))
            return TEXT_PLAIN;
        return super.getContentType(ctxt);
    }

    protected InputStream getRawContent(DavContext ctxt) throws IOException, DavException {
    	return null;
    }
    
	protected InputStream getTextContent(DavContext ctxt) throws IOException {
		StringBuilder buf = new StringBuilder();
		buf.append("Request\n\n");
		buf.append("\tAuthenticated user:\t").append(ctxt.getAuthAccount().getName()).append("\n");
		buf.append("\tCurrent date:\t\t").append(new Date(System.currentTimeMillis())).append("\n");
		buf.append("\nResource\n\n");
		buf.append("\tName:\t\t\t").append(mSubject).append("\n");
		buf.append("\tPath:\t\t\t").append(mPath).append("\n");
		buf.append("\tDate:\t\t\t").append(new Date(mModifiedDate)).append("\n");
		buf.append("\tOwner account Id:\t").append(mOwnerId).append("\n");
		try {
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(Provisioning.AccountBy.id, mOwnerId);
			buf.append("\tOwner account name:\t").append(account.getName()).append("\n");
		} catch (ServiceException se) {
		}
		buf.append("\nProperties\n\n");
		Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_PROP);
		for (ResourceProperty rp : mProps.values())
			rp.toElement(ctxt, e, false);
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setTrimText(false);
		format.setOmitEncoding(false);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLWriter writer = new XMLWriter(baos, format);
		writer.write(e);
		buf.append(new String(baos.toByteArray()));
		return new ByteArrayInputStream(buf.toString().getBytes("UTF-8"));
	}
	
	public static String getEtag(MailItem item) {
		return getEtag(Long.toString(item.getModifiedSequence()), Long.toString(item.getSavedSequence()));
	}
	public static String getEtag(String modMetadata, String modContent) {
		return "\""+modMetadata+"-"+modContent+"\"";
	}
	
	public void setAce(DavContext ctxt, List<Ace> aceList) throws ServiceException, DavException {
		ACL acl = new ACL();
		for (Ace ace : aceList)
			acl.grantAccess(ace.getZimbraId(), ace.getGranteeType(), ace.getRights(), null);

		Mailbox mbox = getMailbox(ctxt);
		mbox.setPermissions(ctxt.getOperationContext(), getId(), acl);
	}
	
	public List<Ace> getAce(DavContext ctxt) throws ServiceException, DavException {
		ArrayList<Ace> aces = new ArrayList<Ace>();
		Mailbox mbox = getMailbox(ctxt);
		MailItem item = mbox.getItemById(ctxt.getOperationContext(), mId, MailItem.TYPE_UNKNOWN);
		Folder f = null;
		if (item.getType() == MailItem.TYPE_FOLDER)
			f = (Folder)item;
		else
			f = mbox.getFolderById(ctxt.getOperationContext(), item.getParentId());
		for (ACL.Grant g : f.getEffectiveACL().getGrants()) {
			if (!g.hasGrantee())
				continue;
			aces.add(new Ace(g.getGranteeId(), g.getGrantedRights(), g.getGranteeType()));
		}
		return aces;
	}
}
