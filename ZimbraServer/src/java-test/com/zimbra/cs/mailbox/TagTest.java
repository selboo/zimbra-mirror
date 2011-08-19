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
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.ParsedMessage;

public class TagTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    private static final String tag1 = "foo", tag2 = "bar", tag3 = "baz", tag4 = "qux";

    @Test
    public void name() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        mbox.createTag(null, tag1, MailItem.DEFAULT_COLOR);
        try {
            mbox.createTag(null, tag1, MailItem.DEFAULT_COLOR);
            Assert.fail("failed to detect naming conflict when creating tag");
        } catch (ServiceException e) {
            Assert.assertEquals("incorrect error code when creating tag", MailServiceException.ALREADY_EXISTS, e.getCode());
        }

        Tag tag = mbox.createTag(null, tag2, MailItem.DEFAULT_COLOR);
        int tagId = tag.getId();

        mbox.rename(null, tag.getId(), tag.getType(), tag3, -1);
        Assert.assertEquals("tag rename", tag3, tag.getName());
        mbox.purge(MailItem.Type.TAG);
        try {
            tag = mbox.getTagByName(tag3);
            Assert.assertEquals("fetching renamed tag", tagId, tag.getId());
        } catch (NoSuchItemException nsie) {
            Assert.fail("renamed tag could not be fetched");
        }

        try {
            mbox.rename(null, tag.getId(), tag.getType(), tag1, -1);
            Assert.fail("failed to detect naming conflict when renaming tag");
        } catch (ServiceException e) {
            Assert.assertEquals("incorrect error code when renaming tag", MailServiceException.ALREADY_EXISTS, e.getCode());
        }
    }

    @Test
    public void color() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // color specified as byte
        Tag tag = mbox.createTag(null, tag1, (byte) 2);
        Assert.assertEquals("tag color 2", 2, tag.getColor());

        mbox.purge(MailItem.Type.TAG);
        tag = mbox.getTagByName(tag1);
        Assert.assertEquals("tag color 2", 2, tag.getColor());
        DbTag.debugConsistencyCheck(mbox);

        // color specified as rgb
        Color color = new Color(0x668822);
        mbox.setColor(null, new int[] { tag.getId() }, MailItem.Type.TAG, color);
        tag = mbox.getTagByName(tag1);
        Assert.assertEquals("tag color " + color, color, tag.getRgbColor());

        mbox.purge(MailItem.Type.TAG);
        tag = mbox.getTagByName(tag1);
        Assert.assertEquals("tag color " + color, color, tag.getRgbColor());
        DbTag.debugConsistencyCheck(mbox);

        // color specified as default
        mbox.setColor(null, new int[] { tag.getId() }, MailItem.Type.TAG, MailItem.DEFAULT_COLOR_RGB);
        tag = mbox.getTagByName(tag1);
        Assert.assertEquals("default tag color", MailItem.DEFAULT_COLOR, tag.getColor());
        Assert.assertEquals("default tag color", MailItem.DEFAULT_COLOR_RGB, tag.getRgbColor());

        mbox.purge(MailItem.Type.TAG);
        tag = mbox.getTagByName(tag1);
        Assert.assertEquals("default tag color", MailItem.DEFAULT_COLOR, tag.getColor());
        Assert.assertEquals("default tag color", MailItem.DEFAULT_COLOR_RGB, tag.getRgbColor());
        DbTag.debugConsistencyCheck(mbox);
    }

    private void checkInboxCounts(String msg, Mailbox mbox, int count, int unread, int deleted, int deletedUnread) throws Exception {
        // check folder counts against in-memory folder object
        Folder inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertEquals(msg + " (folder messages)", count, inbox.getSize());
        Assert.assertEquals(msg + " (folder unread)", unread, inbox.getUnreadCount());
        Assert.assertEquals(msg + " (folder deleted)", deleted, inbox.getDeletedCount());
        Assert.assertEquals(msg + " (folder deleted unread)", deletedUnread, inbox.getDeletedUnreadCount());

        // then force a reload from DB to validate persisted data
        mbox.purge(MailItem.Type.FOLDER);
        inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertEquals(msg + " (folder messages)", count, inbox.getSize());
        Assert.assertEquals(msg + " (folder unread)", unread, inbox.getUnreadCount());
        Assert.assertEquals(msg + " (folder deleted)", deleted, inbox.getDeletedCount());
        Assert.assertEquals(msg + " (folder deleted unread)", deletedUnread, inbox.getDeletedUnreadCount());
    }

    private void checkTagCounts(String msg, Mailbox mbox, String tagName, int count, int unread) throws Exception {
        Tag tag = mbox.getTagByName(tagName);
        Assert.assertEquals(msg + " (tag messages)", count, tag.getSize());
        Assert.assertEquals(msg + " (tag unread)", unread, tag.getUnreadCount());
    }

    private void doubleCheckTagCounts(String msg, Mailbox mbox, String tagName, int count, int unread) throws Exception {
        // check folder counts against in-memory tag object
        checkTagCounts(msg, mbox, tagName, count, unread);
        // then force a reload from DB to validate persisted data
        mbox.purge(MailItem.Type.TAG);
        checkTagCounts(msg, mbox, tagName, count, unread);
    }

    @Test
    public void markRead() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        checkInboxCounts("empty folder", mbox, 0, 0, 0, 0);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
        int msgId = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null).getId();
        checkInboxCounts("added message", mbox, 1, 1, 0, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.DELETED, true, null);
        checkInboxCounts("marked message \\Deleted", mbox, 1, 1, 1, 1);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD | Flag.BITMASK_DELETED);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, false, null);
        checkInboxCounts("marked message read", mbox, 1, 0, 1, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_DELETED);

        Tag tag = mbox.createTag(null, tag1, (byte) 4);
        Assert.assertEquals("tag names match", tag1, tag.getName());
        doubleCheckTagCounts("created tag", mbox, tag1, 0, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_DELETED);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag1, true, null);
        checkInboxCounts("tagged message", mbox, 1, 0, 1, 0);
        doubleCheckTagCounts("tagged message", mbox, tag1, 0, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_DELETED, tag1);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, true, null);
        checkInboxCounts("marked message unread", mbox, 1, 1, 1, 1);
        doubleCheckTagCounts("marked message unread", mbox, tag1, 0, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD | Flag.BITMASK_DELETED, tag1);

        mbox.alterTag(null, tag.getId(), MailItem.Type.TAG, Flag.FlagInfo.UNREAD, false, null);
        checkInboxCounts("marked tag read", mbox, 1, 0, 1, 0);
        doubleCheckTagCounts("marked tag read", mbox, tag1, 0, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_DELETED, tag1);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, true, null);
        checkInboxCounts("marked message unread", mbox, 1, 1, 1, 1);
        doubleCheckTagCounts("marked message unread", mbox, tag1, 0, 0);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD | Flag.BITMASK_DELETED, tag1);

        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, Flag.FlagInfo.DELETED, false, null);
        checkInboxCounts("unmarked message \\Deleted", mbox, 1, 1, 0, 0);
        doubleCheckTagCounts("unmarked message \\Deleted", mbox, tag1, 1, 1);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD, tag1);

        mbox.alterTag(null, tag.getId(), MailItem.Type.TAG, Flag.FlagInfo.UNREAD, false, null);
        checkInboxCounts("marked tag read", mbox, 1, 0, 0, 0);
        doubleCheckTagCounts("marked tag read", mbox, tag1, 1, 0);
        checkItemTags(mbox, msgId, 0, tag1);
    }

    private void checkItemTags(Mailbox mbox, int itemId, int expectedFlags, String... expectedTags) throws Exception {
        MailItem item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        Assert.assertEquals("flags match on item", expectedFlags, item.getFlagBitmask());
        Assert.assertTrue("tags match on item: " + TagUtil.encodeTags(item.getTags()), TagUtil.tagsMatch(item.getTags(), expectedTags));

        mbox.purge(MailItem.Type.MESSAGE);

        item = mbox.getItemById(null, itemId, MailItem.Type.UNKNOWN);
        Assert.assertEquals("flags match on item", expectedFlags, item.getFlagBitmask());
        Assert.assertTrue("tags match on item: " + TagUtil.encodeTags(item.getTags()), TagUtil.tagsMatch(item.getTags(), expectedTags));

        DbTag.debugConsistencyCheck(mbox);
    }

    @Test
    public void implicitCreate() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // implicitly create two tags by including them in an addMessage() call
        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD).setTags(new String[] { tag1, tag2 });
        int msgId = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null).getId();
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD, tag1, tag2);

        // implicitly create a third tag via alterTag()
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, tag3, true, null);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD, tag1, tag2, tag3);

        // implicitly create a fourth by overriding item tags
        mbox.setTags(null, msgId, MailItem.Type.MESSAGE, MailItem.FLAG_UNCHANGED, new String[] { tag1, tag3, tag4 }, null);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD, tag1, tag3, tag4);

        // removing a nonexistent tag should *not* do an implicit create
        String bad = "badbadbad";
        mbox.alterTag(null, msgId, MailItem.Type.MESSAGE, bad, false, null);
        try {
            mbox.getTagByName(bad);
            Assert.fail("removing nonexistent tag should not autocreate");
        } catch (NoSuchItemException nsie) { }

        DbTag.debugConsistencyCheck(mbox);

        // validate counts on the tag objects
        checkTagCounts(tag1, mbox, tag1, 1, 1);
        checkTagCounts(tag2, mbox, tag2, 0, 0);
        checkTagCounts(tag3, mbox, tag3, 1, 1);
        checkTagCounts(tag4, mbox, tag4, 1, 1);

        // verify that the tags got persisted to the database
        mbox.purge(MailItem.Type.MESSAGE);
        checkItemTags(mbox, msgId, Flag.BITMASK_UNREAD, tag1, tag3, tag4);

        // re-fetch the tags from the database
        mbox.purge(MailItem.Type.TAG);
        checkTagCounts(tag1, mbox, tag1, 1, 1);
        checkTagCounts(tag2, mbox, tag2, 0, 0);
        checkTagCounts(tag3, mbox, tag3, 1, 1);
        checkTagCounts(tag4, mbox, tag4, 1, 1);
        try {
            mbox.getTagByName(bad);
            Assert.fail("removing nonexistent tag should not autocreate");
        } catch (NoSuchItemException nsie) { }
    }

    private void checkThreeTagCounts(String msg, Mailbox mbox, int count1, int unread1, int count2, int unread2, int count3, int unread3) throws Exception {
        checkTagCounts(msg + ": tag " + tag1, mbox, tag1, count1, unread1);
        checkTagCounts(msg + ": tag " + tag2, mbox, tag2, count2, unread2);
        checkTagCounts(msg + ": tag " + tag3, mbox, tag3, count3, unread3);

        mbox.purge(MailItem.Type.TAG);
        checkTagCounts(msg + ": tag " + tag1 + " [reloaded]", mbox, tag1, count1, unread1);
        checkTagCounts(msg + ": tag " + tag2 + " [reloaded]", mbox, tag2, count2, unread2);
        checkTagCounts(msg + ": tag " + tag3 + " [reloaded]", mbox, tag3, count3, unread3);

        DbTag.debugConsistencyCheck(mbox);
    }

    @Test
    public void itemDelete() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // precreate some but not all of the tags
        mbox.createTag(null, tag2, (byte) 4);
        mbox.createTag(null, tag3, new Color(0x8800FF));
        DbTag.debugConsistencyCheck(mbox);

        DeliveryOptions dopt = new DeliveryOptions().setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD).setTags(new String[] { tag1, tag2 });
        int msgId = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null).getId();
        checkThreeTagCounts("add an unread message", mbox, 1, 1, 1, 1, 0, 0);

        dopt.setFlags(0).setTags(new String[] { tag1, tag3 });
        int msgId2 = mbox.addMessage(null, new ParsedMessage(ThreaderTest.getSecondMessage(), false), dopt, null).getId();
        checkItemTags(mbox, msgId2, 0, tag1, tag3);
        checkThreeTagCounts("add a read message", mbox, 2, 1, 1, 1, 1, 0);

        mbox.delete(null, msgId, MailItem.Type.MESSAGE);
        checkThreeTagCounts("delete the unread message explicitly", mbox, 1, 0, 0, 0, 1, 0);

        mbox.emptyFolder(null, Mailbox.ID_FOLDER_INBOX, true);
        checkThreeTagCounts("delete the read message by emptying its folder", mbox, 0, 0, 0, 0, 0, 0);

        dopt.setFlags(Flag.BITMASK_UNREAD | Flag.BITMASK_DELETED).setTags(new String[] { tag1, tag2 });
        int msgId3 = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null).getId();
        checkItemTags(mbox, msgId3, Flag.BITMASK_UNREAD | Flag.BITMASK_DELETED, tag1, tag2);
        checkThreeTagCounts("add an unread \\Deleted message", mbox, 0, 0, 0, 0, 0, 0);

        dopt.setFlags(Flag.BITMASK_UNREAD).setTags(new String[] { tag1, tag3 });
        int msgId4 = mbox.addMessage(null, new ParsedMessage(ThreaderTest.getSecondMessage(), false), dopt, null).getId();
        checkItemTags(mbox, msgId4, Flag.BITMASK_UNREAD, tag1, tag3);
        checkThreeTagCounts("add an unread non-\\Deleted message", mbox, 1, 1, 0, 0, 1, 1);

        mbox.delete(null, msgId3, MailItem.Type.MESSAGE);
        checkThreeTagCounts("delete the unread \\Deleted message explicitly", mbox, 1, 1, 0, 0, 1, 1);

        mbox.alterTag(null, msgId4, MailItem.Type.MESSAGE, Flag.FlagInfo.DELETED, true, null);
        checkItemTags(mbox, msgId4, Flag.BITMASK_UNREAD | Flag.BITMASK_DELETED, tag1, tag3);
        checkThreeTagCounts("mark the remaining message as \\Deleted", mbox, 0, 0, 0, 0, 0, 0);

        mbox.emptyFolder(null, Mailbox.ID_FOLDER_INBOX, true);
        checkThreeTagCounts("delete that remaining message by emptying its folder", mbox, 0, 0, 0, 0, 0, 0);

        Message msg5 = mbox.addMessage(null, ThreaderTest.getRootMessage(), dopt, null);
        checkThreeTagCounts("add the conversation root", mbox, 1, 1, 0, 0, 1, 1);

        dopt.setConversationId(msg5.getConversationId()).setFlags(0);
        Message msg6 = mbox.addMessage(null, new ParsedMessage(ThreaderTest.getSecondMessage(), false), dopt, null);
        checkThreeTagCounts("add the conversation reply", mbox, 2, 1, 0, 0, 2, 1);

        mbox.setTags(null, msg6.getId(), MailItem.Type.MESSAGE, Flag.BITMASK_UNREAD, new String[] { tag2, tag3 });
        checkThreeTagCounts("retag reply and mark unread", mbox, 1, 1, 1, 1, 2, 2);

        mbox.alterTag(null, msg5.getId(), MailItem.Type.MESSAGE, Flag.FlagInfo.UNREAD, false, null);
        checkThreeTagCounts("mark root read", mbox, 1, 0, 1, 1, 2, 1);

        mbox.delete(null, msg6.getConversationId(), MailItem.Type.CONVERSATION);
        checkThreeTagCounts("delete the entire conversation", mbox, 0, 0, 0, 0, 0, 0);
    }
}
