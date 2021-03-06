/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.TagInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_TAG_RESPONSE)
public class CreateTagResponse {

    /**
     * @zm-api-field-description Information about the newly created tag
     */
    @XmlElement(name=MailConstants.E_TAG, required=false)
    private TagInfo tag;

    public CreateTagResponse() {
    }

    public void setTag(TagInfo tag) { this.tag = tag; }
    public TagInfo getTag() { return tag; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("tag", tag)
            .toString();
    }
}
