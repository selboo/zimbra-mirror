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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jul 12, 2004
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * @author dkarp
 */
public class Tag extends MailItem {

    Tag(Mailbox mbox, UnderlyingData ud) throws ServiceException {
        super(mbox, ud);
        if (mData.type != TYPE_TAG && mData.type != TYPE_FLAG)
            throw new IllegalArgumentException();
    }

    public byte getIndex() {
        return getIndex(mId);
    }

    public static byte getIndex(int id) {
        return (byte) (id - TAG_ID_OFFSET);
    }

    /** Returns whether this id falls in the acceptable tag ID range (64..127).
     *  Does <u>not</u> verify that such a tag exists.
     * 
     * @param id  Item id to check.
     * @see MailItem#TAG_ID_OFFSET
     * @see MailItem#MAX_TAG_COUNT */
    public static boolean validateId(int id) {
        return (id >= MailItem.TAG_ID_OFFSET && id < MailItem.TAG_ID_OFFSET + MailItem.MAX_TAG_COUNT);
    }

    public long getBitmask() {
        return 1L << getIndex();
    }

    boolean canTag(MailItem item) {
        return item.isTaggable();
    }


    public static long tagsToBitmask(String csv) {
        long bitmask = 0;
        if (csv != null && !csv.equals("")) {
            String[] tags = csv.split(",");
            for (int i = 0; i < tags.length; i++) {
                int value = 0;
                try {
                    value = Integer.parseInt(tags[i]);
                } catch (NumberFormatException e) {
                    ZimbraLog.mailbox.error("unable to parse tags: '" + csv + "'", e);
                    throw e;
                }
                
                if (!validateId(value))
                    continue;
                // FIXME: should really check this against the existing tags in the mailbox
                bitmask |= 1L << Tag.getIndex(value);
            }
        }
        return bitmask;
    }

    static String bitmaskToTags(long bitmask) {
        if (bitmask == 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; bitmask != 0 && i < MAX_TAG_COUNT - 1; i++)
            if ((bitmask & (1L << i)) != 0) {
                if (sb.length() > 0)
                    sb.append(',');
                sb.append(i + TAG_ID_OFFSET);
                bitmask &= ~(1L << i);
            }
        return sb.toString();
    }

    static List<Tag> bitmaskToTagList(Mailbox mbox, long bitmask) throws ServiceException {
        if (bitmask == 0)
            return Collections.emptyList();
        ArrayList<Tag> tags = new ArrayList<Tag>();
        for (int i = 0; bitmask != 0 && i < MAX_TAG_COUNT - 1; i++)
            if ((bitmask & (1L << i)) != 0) {
                tags.add(mbox.getTagById(i + TAG_ID_OFFSET));
                bitmask &= ~(1L << i);
            }
        return tags;
    }


    @Override boolean isTaggable()      { return false; }
    @Override boolean isCopyable()      { return false; }
    @Override boolean isMovable()       { return false; }
    @Override boolean isMutable()       { return true; }
    @Override boolean isIndexed()       { return false; }
    @Override boolean canHaveChildren() { return false; }


    static Tag create(Mailbox mbox, int id, String name, byte color)
    throws ServiceException {
        if (!validateId(id))
            throw MailServiceException.INVALID_ID(id);
        Folder tagFolder = mbox.getFolderById(Mailbox.ID_FOLDER_TAGS);
        if (!tagFolder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions");
        name = validateItemName(name);
        try {
            // if we can successfully get a tag with that name, we've got a naming conflict
            mbox.getTagByName(name);
            throw MailServiceException.ALREADY_EXISTS(name);
        } catch (MailServiceException.NoSuchItemException nsie) {}

        UnderlyingData data = new UnderlyingData();
        data.id          = id;
        data.type        = MailItem.TYPE_TAG;
        data.folderId    = tagFolder.getId();
        data.date        = mbox.getOperationTimestamp();
        data.name        = name;
        data.subject     = name;
        data.metadata    = encodeMetadata(color);
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Tag tag = new Tag(mbox, data);
        tag.finishCreation(null);
        return tag;
    }

    /** Updates the unread state of all items with the tag.  Persists the
     *  change to the database and cache, and also updates the unread counts
     *  for the tag and the affected items' parents and {@link Folder}s
     *  appropriately.  <i>Note: Tags may only be marked read, not unread.</i>
     * 
     * @perms {@link ACL#RIGHT_READ} on the folder,
     *        {@link ACL#RIGHT_WRITE} on all affected messages. */
    @Override
    void alterUnread(boolean unread) throws ServiceException {
        if (unread)
            throw ServiceException.INVALID_REQUEST("tags can only be marked read", null);
        if (!canAccess(ACL.RIGHT_READ))
            throw ServiceException.PERM_DENIED("you do not have the necessary permissions on the tag");
        if (!isUnread())
            return;

        // decrement the in-memory unread count of each message.  each message will
        // then implicitly decrement the unread count for its conversation, folder
        // and tags.
        List<Integer> targets = new ArrayList<Integer>();
        boolean missed = false;
        for (UnderlyingData data : DbMailItem.getUnreadMessages(this)) {
            Message msg = mMailbox.getMessage(data);
            if (msg.checkChangeID() || !msg.canAccess(ACL.RIGHT_WRITE)) {
                msg.updateUnread(unread ? 1 : -1);
                msg.mData.metadataChanged(mMailbox);
                targets.add(msg.getId());
            } else
                missed = true;
        }

        // Mark all messages with this tag as read in the database
        if (!missed)
            DbMailItem.alterUnread(this, unread);
        else
            DbMailItem.alterUnread(mMailbox, targets, unread);
    }

    private static final String INVALID_PREFIX = "\\";

    static String validateItemName(String name) throws ServiceException {
        name = MailItem.validateItemName(name == null ? null : name.trim());
        if (name.startsWith(INVALID_PREFIX))
            throw MailServiceException.INVALID_NAME(name);
        return name;
    }

    @Override
    void purgeCache(PendingDelete info, boolean purgeItem) throws ServiceException {
        // remove the tag from all items in the database
        DbMailItem.clearTag(this);
        // dump entire item cache (necessary now because we reuse tag ids)
        mMailbox.purge(TYPE_MESSAGE);
        // remove tag from tag cache
        super.purgeCache(info, purgeItem);
    }

    /** Persists the tag's current unread count to the database. */
    protected void saveTagCounts() throws ServiceException {
        DbMailItem.persistCounts(this, encodeMetadata());
    }


    @Override
    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);
    }

    @Override
    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor);
    }

    private static String encodeMetadata(byte color) {
        return encodeMetadata(new Metadata(), color).toString();
    }

    static Metadata encodeMetadata(Metadata meta, byte color) {
        return MailItem.encodeMetadata(meta, color);
    }


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("tag: {");
        appendCommonMembers(sb);
        sb.append("}");
        return sb.toString();
    }
}
