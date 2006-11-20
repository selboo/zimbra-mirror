package com.zimbra.cs.taglib.tag;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.taglib.bean.ZMessageBean;
import com.zimbra.cs.taglib.bean.ZMessageComposeBean;
import com.zimbra.cs.taglib.bean.ZMessageComposeBean.Action;
import com.zimbra.cs.zclient.ZMailbox;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;

public class ComputeMessageComposeTag extends ZimbraSimpleTag {

    public static final String ACTION_NEW = "new";
    public static final String ACTION_REPLY = "reply";
    public static final String ACTION_REPLY_ALL = "replyAll";
    public static final String ACTION_FORWARD = "forward";        

    private String mVar;
    private ZMessageBean mMessage;
    private String mAction;

    public void setVar(String var) { this.mVar = var; }

    public void setMessage(ZMessageBean message) { mMessage = message; }

    public void setAction(String action) { mAction = action; }

    public void doTag() throws JspException, IOException {
        try {
            JspContext jctxt = getJspContext();
            ZMailbox mailbox = getMailbox();

            ZMessageComposeBean compose;
            if (ACTION_REPLY.equals(mAction))
                compose = new ZMessageComposeBean(Action.REPLY, mMessage, mailbox.getIdentities());
            else if (ACTION_REPLY_ALL.equals(mAction))
                compose = new ZMessageComposeBean(Action.REPLY_ALL, mMessage, mailbox.getIdentities());
            else if (ACTION_FORWARD.equals(mAction))
                compose = new ZMessageComposeBean(Action.FORWARD, mMessage, mailbox.getIdentities());
            else
                compose = new ZMessageComposeBean(Action.NEW, null, mailbox.getIdentities());

            jctxt.setAttribute(mVar, compose, PageContext.PAGE_SCOPE);
            
        } catch (ServiceException e) {
            throw new JspTagException(e.getMessage(), e);
        }
    }



}
