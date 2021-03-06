<%--
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
--%>
<%@ tag body-content="empty" %>
<%@ attribute name="mailbox" rtexprvalue="true" required="true" type="com.zimbra.cs.taglib.bean.ZMailboxBean"%>
<%@ attribute name="selected" rtexprvalue="true" required="true" %>
<%@ attribute name="prev" rtexprvalue="true" required="true" type="java.lang.String"  %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="com.zimbra.i18n" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="app" uri="com.zimbra.htmlclient" %>

<table>
    <tr class='Tabs'>
        <td class='TabSpacer'/>
        <td class='TabSpacer'/>
        <td class='Tab ${selected=='general' ? 'TabSelected' : 'TabNormal'}'>
            <a href="<c:url value="/h/options?selected=general&prev=${prev}"/>">
                <span><fmt:message key="general"/></span>
            </a>
        </td>
        <c:if test="${zm:isMailEnabled(mailbox)}">
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='mail' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=mail&prev=${prev}"/>">
                    <span><fmt:message key="mail"/></span></a>
            </td>
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='composing' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=composing&prev=${prev}"/>">
                    <span><fmt:message key="optionsComposing"/></span></a>
            </td>
        <c:set var="maxSigs" value="${mailbox.accountInfo.attrs.zimbraSignatureMaxNumEntries[0]}"/>
        <c:if test="${maxSigs ne 0}">
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='signatures' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=signatures&prev=${prev}"/>">
                    <span><fmt:message key="optionsSignatures"/></span></a>
            </td>
        </c:if>
        </c:if>
        <c:if test="${mailbox.features.voice}">
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='voice' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=voice&prev=${prev}"/>">
                    <span><fmt:message key="callManager"/></span></a>
            </td>
        </c:if>
        <c:if test="${mailbox.features.contacts}">
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='addressbook' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=addressbook&prev=${prev}"/>">
                    <span><fmt:message key="addressBook"/></span></a>
            </td>
        </c:if>
        <c:if test="${mailbox.features.calendar}">
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='calendar' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=calendar&prev=${prev}"/>">
                    <span><fmt:message key="calendar"/></span></a>
            </td>
        </c:if>
        <c:if test="${zm:isMailEnabled(mailbox)}">
        <td class='TabSpacer'/>
        <td class='Tab ${selected=='accounts' ? 'TabSelected' : 'TabNormal'}'>
            <a href="<c:url value="/h/options?selected=accounts&prev=${prev}"/>">
                <span><fmt:message key="optionsAccounts"/></span></a>
        </td>
        </c:if>
        <c:if test="${mailbox.features.filters and zm:isMailEnabled(mailbox)}">                                                                                       
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='filter' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=filter&prev=${prev}"/>">
                    <span><fmt:message key="mailFilters"/></span></a>
            </td>
        </c:if>
        <c:if test="${mailbox.prefs.useKeyboardShortcuts}">
            <td class='TabSpacer'/>
            <td class='Tab ${selected=='shortcuts' ? 'TabSelected' : 'TabNormal'}'>
                <a href="<c:url value="/h/options?selected=shortcuts&prev=${prev}"/>">
                    <span><fmt:message key="shortcuts"/></span></a>
            </td>
        </c:if>
        <c:if test="${mailbox.attrs.zimbraStandardClientCustomPrefTabsEnabled[0] eq 'TRUE'}">
            <c:set var="customTabs" value="${mailbox.attrs.zimbraStandardClientCustomPrefTab}"/>
            <c:forEach var="customTab" items="${customTabs}" varStatus="status">
                <c:set var="tab" value="${fn:split(customTab,',')}"/>
                <c:set var="tabNameKey" value="customTab_${fn:toLowerCase(fn:replace(tab[0],' ',''))}"/>
                <td class='TabSpacer'/>
                <td class='Tab ${selected==fn:toLowerCase(tab[0]) ? 'TabSelected' : 'TabNormal'}'>
                    <a href="<c:url value="/h/options?selected=${tab[0]}&prev=${prev}&status=${status.index}"/>">
                        <span> <fmt:message key="${tabNameKey}"/> </span>
                    </a>
                </td>    
            </c:forEach> 
        </c:if>
        <td class='TabFiller'>&nbsp;</td>
    </tr>
</table>