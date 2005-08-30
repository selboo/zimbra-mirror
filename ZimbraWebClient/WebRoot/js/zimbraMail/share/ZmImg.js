/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

function ZmImg() {
}

// Data for images = filename, width, height]

// Miscellaneous images
ZmImg.M_BANNER 			= ["ImgBanner", 200, 38];
ZmImg.M_DND_MULTI_NO  	= ["ImgDndMultiNoIcon", 52, 52];
ZmImg.M_DND_MULTI_YES  	= ["ImgDndMultiYesIcon", 52, 52];
ZmImg.M_LOGIN  			= ["ImgLoginLogo", 31, 60];
ZmImg.M_SPLASH  		= ["ImgSplashScreen", 351, 222];

ZmImg.I_TMP  			= ["ImgTMPIcon", 100, 100];

// Icons
ZmImg.I_APPT 			= ["ImgAppointmentIcon", 16, 16];
ZmImg.I_APPT_EXCEPTION  = ["ImgApptException", 16, 13];
ZmImg.I_APPT_MEETING  	= ["ImgApptMeeting", 16, 13];
ZmImg.I_APPT_RECUR 		= ["ImgApptRecur", 16, 13];
ZmImg.I_APPT_REMINDER 	= ["ImgApptReminder", 16, 13];
ZmImg.I_ATTACHMENT 		= ["ImgAttachmentIcon", 16, 16];
ZmImg.I_AUDIO  			= ["ImgAudioIcon", 16, 16];
ZmImg.I_BACK_ARROW 		= ["ImgLeftArrowIcon", 16, 16];
ZmImg.I_BINARY 			= ["ImgBinaryDocumentIcon", 16, 16];
ZmImg.I_BLANK 			= [null, 16, 16];
ZmImg.I_BMP 			= ["ImgBmpImageIcon", 16, 16];
ZmImg.I_BOLD_TEXT 		= ["ImgBoldTextIcon", 16, 16];
ZmImg.I_BROWSE 			= ["ImgBrowseIcon", 16, 16];
ZmImg.I_BULLETED_LIST 	= ["ImgBulletedListIcon", 16, 16];
ZmImg.I_CENTER_JUSTIFY 	= ["ImgCenterJustifyIcon", 16, 16];
ZmImg.I_CONTACT 		= ["ImgContactIcon", 16, 16];
ZmImg.I_CONTACT_PICKER 	= ["ImgContactPicker", 16, 16];
ZmImg.I_CONV 			= ["ImgConvIcon", 16, 16];
ZmImg.I_COPY 			= ["ImgCopyIcon", 16, 16];
ZmImg.I_CRITICAL 		= ["ImgCriticalIcon32x32", 32, 32];
ZmImg.I_DATE 			= ["ImgDateIcon", 16, 16];
ZmImg.I_DAY_VIEW 		= ["ImgDayViewIcon", 16, 16];
ZmImg.I_DBL_BACK_ARROW  = ["ImgLeftDoubleArrowIcon", 16, 16];
ZmImg.I_DBL_FORW_ARROW  = ["ImgRightDoubleArrowIcon", 16, 16];
ZmImg.I_DELETE 			= ["ImgDeleteIcon", 16, 16];
ZmImg.I_DELETE_CONV 	= ["ImgDelConvIcon", 16, 16];
ZmImg.I_DELETE_TAG 		= ["ImgDeleteTagIcon", 16, 16];
ZmImg.I_DETACH 			= ["ImgDetachIcon", 16, 16];
ZmImg.I_DOCUMENT 		= ["ImgDocumentIcon", 16, 16];
ZmImg.I_DOMAIN 			= ["ImgDomainIcon", 16, 16];
ZmImg.I_DOOR 			= ["ImgDoorIcon", 16, 16];
ZmImg.I_DOWN_ARROW 		= ["ImgDownArrowIcon", 16, 16];
ZmImg.I_DRAFT_FOLDER 	= ["ImgDraftFolderIcon", 16, 16];
ZmImg.I_DRAFT_MSG 		= ["ImgDraftMsgIcon", 16, 16];
ZmImg.I_ENVELOPE 		= ["ImgEnvelopeIcon", 16, 16];
ZmImg.I_FLAG_ON 		= ["ImgFlagOnIcon", 16, 16];
ZmImg.I_FLAG_OFF 		= ["ImgFlagOffIcon", 16, 16];
ZmImg.I_FOLDER 			= ["ImgFolderIcon", 16, 16];
ZmImg.I_FONT_BACKGROUND = ["ImgFontBackgroundIcon", 16, 16];
ZmImg.I_FONT_COLOR 		= ["ImgFontColorIcon", 16, 16];
ZmImg.I_FORMAT 			= ["ImgFormatIcon", 16, 16];
ZmImg.I_FORWARD 		= ["ImgForwardIcon", 20, 16];
ZmImg.I_FORWARD_ARROW 	= ["ImgRightArrowIcon", 16, 16];
ZmImg.I_FORWARD_STATUS 	= ["ImgMailForwardIcon", 20, 16];
ZmImg.I_FULL_JUSTIFY 	= ["ImgFullJustifyIcon", 16, 16];
ZmImg.I_GAL 			= ["ImgGalIcon", 16, 16];
ZmImg.I_GIF 			= ["ImgGifImageIcon", 15, 16];
ZmImg.I_GLOBE 			= ["ImgGlobeIcon", 16, 16];
ZmImg.I_CHECK 			= ["ImgCheckIcon", 16, 16];
ZmImg.I_HELP 			= ["ImgHelpIcon", 16, 16];
ZmImg.I_HORIZ_RULE 		= ["ImgHorizRuleIcon", 16, 16];
ZmImg.I_HTML 			= ["ImgHtmlDocumentIcon", 16, 16];
ZmImg.I_ICON 			= ["ImgIconIcon", 16, 16];
ZmImg.I_IM 				= ["ImgJabber2Icon", 11, 15];
ZmImg.I_IMAGE 			= ["ImgImageIcon", 16, 16];
ZmImg.I_INDENT 			= ["ImgIndentIcon", 16, 16];
ZmImg.I_INSERT_TABLE 	= ["ImgInsertTableIcon", 16, 16];
ZmImg.I_ITALIC_TEXT 	= ["ImgItalicTextIcon", 16, 16];
ZmImg.I_JPEG 			= ["ImgJpegImageIcon", 16, 16];
ZmImg.I_LEFT_JUSTIFY 	= ["ImgLeftJustifyIcon", 16, 16];
ZmImg.I_LIST 			= ["ImgListIcon", 16, 16];
ZmImg.I_LOGOFF 			= ["ImgLogoffIcon", 16, 16];
ZmImg.I_MAIL 			= ["ImgMailIcon", 16, 16];
ZmImg.I_MAIL_FOLDER 	= ["ImgMailFolderIcon", 16, 16];
ZmImg.I_MAIL_MSG 		= ["ImgMailMessageIcon", 16, 16];
ZmImg.I_MAIL_READ 		= ["ImgMailReadIcon", 20, 16];
ZmImg.I_MAIL_RULE 		= ["ImgMailRuleIcon", 16, 16];
ZmImg.I_MAIL_SENT 		= ["ImgMailSentIcon", 20, 16];
ZmImg.I_MAIL_STATUS 	= ["ImgMailStatusIcon", 16, 16];
ZmImg.I_MAIL_UNREAD 	= ["ImgMailUnreadIcon", 20, 16];
ZmImg.I_MEETING_REQUEST = ["ImgMeetingRequestIcon", 16, 16];
ZmImg.I_MINI_TAG 		= ["ImgMiniTagIcon", 16, 16];
ZmImg.I_MINI_TAG_BLUE 	= ["ImgMiniTagIconBlue", 16, 16];
ZmImg.I_MINI_TAG_CYAN 	= ["ImgMiniTagIconCyan", 16, 16];
ZmImg.I_MINI_TAG_GREEN 	= ["ImgMiniTagIconGreen", 16, 16];
ZmImg.I_MINI_TAG_ORANGE = ["ImgMiniTagIconOrange", 16, 16];
ZmImg.I_MINI_TAG_PINK 	= ["ImgMiniTagIconPink", 16, 16];
ZmImg.I_MINI_TAG_PURPLE = ["ImgMiniTagIconPurple", 16, 16];
ZmImg.I_MINI_TAG_RED 	= ["ImgMiniTagIconRed", 16, 16];
ZmImg.I_MINI_TAG_STACK 	= ["ImgMiniTagStackIcon", 16, 16];
ZmImg.I_MINI_TAG_YELLOW = ["ImgMiniTagIconYellow", 16, 16];
ZmImg.I_MINUS 			= ["ImgMinusIcon", 16, 16];
ZmImg.I_MONTH_VIEW 		= ["ImgMonthViewIcon", 16, 16];
ZmImg.I_MOVE 			= ["ImgMoveToFolderIcon", 16, 16];
ZmImg.I_MS_EXCEL 		= ["ImgMSExcelDocIcon", 16, 16];
ZmImg.I_MS_POWERPOINT 	= ["ImgMSPPTDocIcon", 16, 16];
ZmImg.I_MS_PROJECT 		= ["ImgMSProjectDocIcon", 16, 16];
ZmImg.I_MS_VISIO 		= ["ImgMSVisioDocIcon", 16, 16];
ZmImg.I_MS_WMV 			= ["ImgAudioIcon", 16, 16];
ZmImg.I_MS_WORD 		= ["ImgMSWordDocIcon", 16, 16];
ZmImg.I_NEW_FOLDER 		= ["ImgNewFolderIcon", 16, 16];
ZmImg.I_NEW_TAG 		= ["ImgNewTagIcon", 16, 16];
ZmImg.I_NEW_TIME 		= ["ImgNewTimeIcon", 7, 11];
ZmImg.I_NOTE 			= ["ImgNoteIcon", 16, 16];
ZmImg.I_NUMBERED_LIST 	= ["ImgNumberedListIcon", 16, 16];
ZmImg.I_OUTDENT 		= ["ImgOutdentIcon", 16, 16];
ZmImg.I_PADLOCK 		= ["ImgPadlockIcon", 16, 16];
ZmImg.I_PANE_DOUBLE 	= ["ImgPaneDoubleIcon", 16, 16];
ZmImg.I_PANE_SINGLE 	= ["ImgPaneSingleIcon", 16, 16];
ZmImg.I_PDF 			= ["ImgPDFDocIcon", 16, 16];
ZmImg.I_PO 				= ["ImgPOIcon", 16, 16];
ZmImg.I_PLUS 			= ["ImgPlusIcon", 16, 16];
ZmImg.I_PREFERENCES 	= ["ImgPreferencesIcon", 16, 16];
ZmImg.I_PRINTER 		= ["ImgPrinterIcon", 16, 16];
ZmImg.I_PROPERTIES 		= ["ImgPropertiesIcon", 16, 16];
ZmImg.I_QUESTION_MARK 	= ["ImgquestionMarkIcon", 16, 16];
ZmImg.I_READ_MSG 		= ["ImgReadMessageIcon", 16, 16];
ZmImg.I_RED_X 			= ["ImgRedXIcon", 16, 16];
ZmImg.I_RED_CLOSE_ICON 	= ["ImgRedCloseIcon", 16, 16];
ZmImg.I_RENAME 			= ["ImgRenameIcon", 16, 16];
ZmImg.I_REPLY 			= ["ImgReplyIcon", 20, 16];
ZmImg.I_REPLY_ALL 		= ["ImgReplyAllIcon", 16, 16];
ZmImg.I_REPLY_STATUS 	= ["ImgMailReplyIcon", 20, 16];
ZmImg.I_RIGHT_JUSTIFY 	= ["ImgRightJustifyIcon", 16, 16];
ZmImg.I_SAVE 			= ["ImgSaveIcon", 16, 16];
ZmImg.I_SEARCH 			= ["ImgSearchIcon", 16, 16];
ZmImg.I_SEARCH_ALL 		= ["ImgSearchAllIcon", 20, 16];
ZmImg.I_SEARCH_CALENDAR = ["ImgSearchCalendarIcon", 20, 16];
ZmImg.I_SEARCH_CONTACTS = ["ImgSearchContactsIcon", 20, 16];
ZmImg.I_SEARCH_FOLDER 	= ["ImgSearchFolderIcon", 16, 16];
ZmImg.I_SEARCH_GAL 		= ["ImgSearchGALIcon", 20, 16];
ZmImg.I_SEARCH_MAIL 	= ["ImgSearchMailIcon", 20, 16];
ZmImg.I_SENT_FOLDER 	= ["ImgSentItemsFolderIcon", 16, 16];
ZmImg.I_SPAM_FOLDER 	= ["ImgJunkFolderIcon", 16, 16];
ZmImg.I_SUBSCRIPT 		= ["ImgSubscriptIcon", 16, 16];
ZmImg.I_STRIKETHRU_TEXT = ["ImgStrikeThruTextIcon", 16, 16];
ZmImg.I_SUPERSCRIPT 	= ["ImgSuperscriptIcon", 16, 16];
ZmImg.I_TASK 			= ["ImgTaskIcon", 16, 16];
ZmImg.I_TAG 			= ["ImgTagIcon", 16, 16];
ZmImg.I_TAG_BLUE 		= ["ImgTagIconBlue", 16, 16];
ZmImg.I_TAG_CYAN 		= ["ImgTagIconCyan", 16, 16];
ZmImg.I_TAG_FOLDER 		= ["ImgTagFolderIcon", 16, 16];
ZmImg.I_TAG_GREEN 		= ["ImgTagIconGreen", 16, 16];
ZmImg.I_TAG_ORANGE 		= ["ImgTagIconOrange", 16, 16];
ZmImg.I_TAG_PINK 		= ["ImgTagIconPink", 16, 16];
ZmImg.I_TAG_PURPLE 		= ["ImgTagIconPurple", 16, 16];
ZmImg.I_TAG_RED 		= ["ImgTagIconRed", 16, 16];
ZmImg.I_TAG_YELLOW 		= ["ImgTagIconYellow", 16, 16];
ZmImg.I_TELEPHONE 		= ["ImgTelephoneIcon", 16, 16];
ZmImg.I_TO 				= ["ImgMailToIcon", 16, 16];
ZmImg.I_TRASH 			= ["ImgTrashIcon", 16, 16];
ZmImg.I_URL 			= ["ImgURLIcon", 16, 16];
ZmImg.I_UNDERLINE_TEXT 	= ["ImgUnderlineTextIcon", 16, 16];
ZmImg.I_UNDO 			= ["ImgUndoIcon", 16, 16];
ZmImg.I_UP_ARROW 		= ["ImgUpArrowIcon", 16, 16];
ZmImg.I_WEEK_VIEW 		= ["ImgWeekViewIcon", 16, 16];
ZmImg.I_WORK_WEEK_VIEW 	= ["ImgWorkWeekViewIcon", 16, 16];
ZmImg.I_ZIP 			= ["ImgZipDocumentIcon", 16, 16];

// Disabled icons
ZmImg.ID_ATTACHMENT 	= ["ImgAttachmentIcon", 16, 16];
ZmImg.ID_BACK_ARROW 	= ["ImgLeftArrowIconDis", 16, 16];
ZmImg.ID_BROWSE 		= ["ImgBrowseIconDis", 16, 16];
ZmImg.ID_COLOR_WHEEL 	= ["ImgColorWheelIconDis", 16, 16];
ZmImg.ID_CONV 			= ["ImgConvIconDis", 16, 16];
ZmImg.ID_DAY_VIEW 		= ["ImgDayViewIconDis", 16, 16];
ZmImg.ID_DBL_BACK_ARROW = ["ImgLeftDoubleArrowIconDis", 16, 16];
ZmImg.ID_DBL_FORW_ARROW = ["ImgRightDoubleArrowIconDis", 16, 16];
ZmImg.ID_DELETE 		= ["ImgDeleteIconDis", 16, 16];
ZmImg.ID_DELETE_TAG 	= ["ImgDeleteTagIconDis", 16, 16];
ZmImg.ID_DOWN_ARROW 	= ["ImgDownArrowIconDis", 16, 16];
ZmImg.ID_FORMAT 		= ["ImgFormatIconDis", 16, 16];
ZmImg.ID_FORWARD 		= ["ImgForwardIconDis", 16, 16];
ZmImg.ID_FORWARD_ARROW 	= ["ImgRightArrowIconDis", 16, 16];
ZmImg.ID_IM 			= ["ImgIMUnavailableIcon", 11, 15];
ZmImg.ID_MAIL 			= ["ImgMailIconDis", 16, 16];
ZmImg.ID_MAIL_MSG 		= ["ImgMailMessageIconDis", 16, 16];
ZmImg.ID_MONTH_VIEW 	= ["ImgMonthViewIconDis", 16, 16];
ZmImg.ID_MOVE 			= ["ImgMoveToFolderIconDis", 16, 16];
ZmImg.ID_PRINTER 		= ["ImgPrinterIconDis", 16, 16];
ZmImg.ID_REPLY 			= ["ImgReplyIconDis", 16, 16];
ZmImg.ID_REPLY_ALL 		= ["ImgReplyAllIconDis", 16, 16];
ZmImg.ID_SAVE 			= ["ImgSaveIconDis", 16, 16];
ZmImg.ID_SEARCH 		= ["ImgSearchIconDis", 16, 16];
ZmImg.ID_SPAM_FOLDER 	= ["ImgJunkFolderIconDis", 16, 16];
ZmImg.ID_TAG 			= ["ImgTagIconDis", 16, 16];
ZmImg.ID_TAG_FOLDER 	= ["ImgTagFolderIconDis", 16, 16];
ZmImg.ID_UP_ARROW 		= ["ImgUpArrowIconDis", 16, 16];
ZmImg.ID_WEEK_VIEW 		= ["ImgWeekViewIconDis", 16, 16];
ZmImg.ID_WORK_WEEK_VIEW = ["ImgWorkWeekViewIconDis", 16, 16];

// Large icons
ZmImg.IL_AUDIO 			= ["ImgAudioIcon48x48", 48, 47];
ZmImg.IL_BINARY 		= ["ImgBinaryDocument48x48Icon", 48, 48];
ZmImg.IL_BMP 			= ["ImgBmpImageIcon48x48", 48, 48];
ZmImg.IL_DOCUMENT 		= ["ImgDocumentIcon48x48", 48, 48];
ZmImg.IL_ENVELOPE 		= ["ImgEnvelopeIcon48x48", 48, 48];
ZmImg.IL_GIF 			= ["ImgGifImageIcon48x48", 48, 48];
ZmImg.IL_HTML 			= ["ImgHtmlDocumentIcon48x48", 48, 48];
ZmImg.IL_IMAGE 			= ["ImgImageIcon48x48", 48, 48];
ZmImg.IL_JPEG 			= ["ImgJpegImageIcon48x48", 48, 48];
ZmImg.IL_MS_EXCEL 		= ["ImgMSExcelDocIcon48x48", 48, 48];
ZmImg.IL_MS_POWERPOINT 	= ["ImgMSPPTDocIcon48x48", 48, 48];
ZmImg.IL_MS_PROJECT 	= ["ImgMSProjectDocIcon48x48", 48, 48];
ZmImg.IL_MS_VISIO 		= ["ImgMSVisioDocIcon48x48", 48, 48];
ZmImg.IL_MS_WMV 		= ["ImgAudioIcon48x48", 48, 48];
ZmImg.IL_MS_WORD 		= ["ImgMSWordDocIcon48x48", 48, 48];
ZmImg.IL_PDF 			= ["ImgPDFDocIcon48x48", 48, 48];
ZmImg.IL_ZIP 			= ["ImgZipDocumentIcon48x48", 48, 48];


// border pieces

// "card" border
ZmImg.CARD_TL = ["Imgcard_TL", 8,   8];
ZmImg.CARD_TM = ["Imgcard_TM", 8,  20];
ZmImg.CARD_TR = ["Imgcard_TR", 8,  11];
ZmImg.CARD_ML = ["Imgcard_ML", 20,  8];
ZmImg.CARD_MM = ["Imgcard_MM", 20, 20];
ZmImg.CARD_MR = ["Imgcard_MR", 20, 11];
ZmImg.CARD_BL = ["Imgcard_BL", 10,  8];
ZmImg.CARD_BM = ["Imgcard_BM", 10, 20];
ZmImg.CARD_BR = ["Imgcard_BR", 10, 11];

ZmImg.SELECTED_CARD_TL = ["Imgselected_card_TL", 8,   8];
ZmImg.SELECTED_CARD_TM = ["Imgselected_card_TM", 8,  20];
ZmImg.SELECTED_CARD_TR = ["Imgselected_card_TR", 8,  11];
ZmImg.SELECTED_CARD_ML = ["Imgselected_card_ML", 20,  8];
ZmImg.SELECTED_CARD_MM = ["Imgselected_card_MM", 20, 20];
ZmImg.SELECTED_CARD_MR = ["Imgselected_card_MR", 20, 11];
ZmImg.SELECTED_CARD_BL = ["Imgselected_card_BL", 10,  8];
ZmImg.SELECTED_CARD_BM = ["Imgselected_card_BM", 10, 20];
ZmImg.SELECTED_CARD_BR = ["Imgselected_card_BR", 10, 11];


// "balloon" border
ZmImg.BALLOON_TL = ["Imgballoon_TL", 20, 20];
ZmImg.BALLOON_TM = ["Imgballoon_TM", 20, 20];
ZmImg.BALLOON_TR = ["Imgballoon_TR", 20, 20];
ZmImg.BALLOON_ML = ["Imgballoon_ML", 20, 20];
ZmImg.BALLOON_MM = ["Imgballoon_MM", 20, 20];
ZmImg.BALLOON_MR = ["Imgballoon_MR", 20, 20];
ZmImg.BALLOON_BL = ["Imgballoon_BL", 20, 20];
ZmImg.BALLOON_BM = ["Imgballoon_BM", 20, 20];
ZmImg.BALLOON_BR = ["Imgballoon_BR", 20, 20];

ZmImg.TL_BALLOON_TIP = ["ImgTL_balloon_tip", 20, 72];

// free/busy images
ZmImg.CAL_FB_KEY = ["Imgcal_fb_key", 300, 75];
ZmImg.CAL_FB_NEXT_DAY = ["Imgcal_fb_next_day", 20, 254];
ZmImg.CAL_FB_PREV_DAY = ["Imgcal_fb_prev_day", 20, 254];
