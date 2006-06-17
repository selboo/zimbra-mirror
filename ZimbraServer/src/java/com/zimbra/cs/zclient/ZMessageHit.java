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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import java.util.List;

public interface ZMessageHit extends ZSearchHit {

    /**
     * @return message's id
     */
    public String getId();

    public String getFlags();

    public long getSize();

    public long getDate();
    
    public String getConversationId();
    
    /**
     * @return comma-separated list of tag ids
     */
    public String getTagIds();

    public String getSortFied();
    
    public String getSubject();
    
    public float getScore();
    
    public String getFragment();
    
    public ZEmailAddress getSender();

    public boolean getContentMatched();
    
    /**
     *  @return names (1.2.3...) of mime part(s) that matched, or empty list.
     */
    public List<String> getMimePartHits();
}
