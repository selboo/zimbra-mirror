/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_ALL_DISTRIBUTION_LISTS_RESPONSE)
public class GetAllDistributionListsResponse {

    @XmlElement(name=AdminConstants.E_DL, required=false)
    private List <DistributionListInfo> dls = Lists.newArrayList();

    public GetAllDistributionListsResponse() {
        this((List <DistributionListInfo>)null);
    }

    public GetAllDistributionListsResponse(List <DistributionListInfo> dls) {
        setDls(dls);
    }

    public GetAllDistributionListsResponse setDls(Collection<DistributionListInfo> dls) {
        this.dls.clear();
        if (dls != null) {
            this.dls.addAll(dls);
        }
        return this;
    }

    public GetAllDistributionListsResponse addDl(DistributionListInfo dl) {
        dls.add(dl);
        return this;
    }

    public List<DistributionListInfo> getDls() {
        return Collections.unmodifiableList(dls);
    }
}
