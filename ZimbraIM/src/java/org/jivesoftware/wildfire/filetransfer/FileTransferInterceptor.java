/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package org.jivesoftware.wildfire.filetransfer;

/**
 * Interface used to capture a file transfer before it begins.
 *
 * @author Alexander Wenckus
 */
public interface FileTransferInterceptor {
    /**
     * Invokes the interceptor on the specified file transfer. The interceptor can either modify
     * the file transfer or throw a FileTransferRejectedException. The file transfer went sent to
     * the interceptor can be in two states, ready and not ready. The not ready state indicates
     * that this event was fired when the file transfer request was sent by the initatior. The ready
     * state indicates that the file transfer is ready to begin, and the channels can be
     * manipulated by the interceptor.
     * <p>
     * It is recommended for the the sake of user experience that
     * when in the not ready state, any processing done on the file transfer should be quick.
     *
     * @param transfer the transfer being intercepted
     * @param isReady true if the transfer is ready to commence or false if this is related to the
     * initial file transfer request. An exception at this point will cause the transfer to
     * not go through.
     *
     */
    void interceptFileTransfer(FileTransfer transfer, boolean isReady)
            throws FileTransferRejectedException;
}
