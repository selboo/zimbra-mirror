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
package com.zimbra.cs.index;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.MailboxIndex.BrowseTerm;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.stats.ZimbraPerf;


/**
 * An updated Lucene provider that uses the IndexWritersCache to manage the index LRU.  This class 
 * is currently activated via a LC key but will eventually become the default
 */
public class LuceneIndex extends IndexWritersCache.IndexWriter implements ILuceneIndex, ITextIndex{

    
    static {
        System.setProperty("org.apache.lucene.FSDirectory.class", "com.zimbra.cs.index.Z23FSDirectory");
    }
    
    static void flushAllWriters() {
        if (DebugConfig.disableIndexing)
            return;
        
        sIndexWritersCache.flushAllWriters();
    }

    static void shutdown() {
        if (DebugConfig.disableIndexing)
            return;

        sIndexReadersCache.signalShutdown();
        try {
            sIndexReadersCache.join();
        } catch (InterruptedException e) {}

        sIndexWritersCache.shutdown();
    }

    static void startup() {
        if (DebugConfig.disableIndexing) {
            ZimbraLog.index.info("Indexing is disabled by the localconfig 'debug_disable_indexing' flag");
            return;
        }
        
        if (sIndexWritersCache != null) {
            // in case startup is somehow called twice
            sIndexWritersCache.shutdown();
        }
        
        sMaxUncommittedOps = LC.zimbra_index_max_uncommitted_operations.intValue();
        sIndexReadersCache = new IndexReadersCache(LC.zimbra_index_reader_lru_size.intValue(), 
            LC.zimbra_index_reader_idle_flush_time.longValue() * 1000, 
            LC.zimbra_index_reader_idle_sweep_frequency.longValue() * 1000);
        sIndexReadersCache.start();
        
        sIndexWritersCache = new IndexWritersCache(); 
    }

    /**
    Finds and returns the smallest of three integers 
     */
    private static final int min(int a, int b, int c) {
        int t = (a < b) ? a : b;
        return (t < c) ? t : c;
    }
    
    public long getBytesWritten() {
        return mIdxDirectory.getBytesWritten();
    }
    public long getBytesRead() {
        return mIdxDirectory.getBytesRead();
    }        
    
    
    LuceneIndex(MailboxIndex mbidx, String idxParentDir, int mailboxId) throws ServiceException {
        mMbidx = mbidx;
        mIndexWriter = null;
        
        // this must be different from the idxParentDir (see the IMPORTANT comment below)
        String idxPath = idxParentDir + File.separatorChar + '0';

        {
            File parentDirFile = new File(idxParentDir);

            // IMPORTANT!  Don't make the actual index directory (mIdxDirectory) yet!  
            //
            // The runtime open-index code checks the existance of the actual index directory:  
            // if it does exist but we cannot open the index, we do *NOT* create it under the 
            // assumption that the index was somehow corrupted and shouldn't be messed-with....on the 
            // other hand if the index dir does NOT exist, then we assume it has never existed (or 
            // was deleted intentionally) and therefore we should just create an index.
            if (!parentDirFile.exists())
                parentDirFile.mkdirs();

            if (!parentDirFile.canRead()) {
                throw ServiceException.FAILURE("Cannot READ index directory (mailbox="+mailboxId+ " idxPath="+idxPath+")", null);
            }
            if (!parentDirFile.canWrite()) {
                throw ServiceException.FAILURE("Cannot WRITE index directory (mailbox="+mailboxId+ " idxPath="+idxPath+")", null);
            }

            // the Lucene code does not atomically swap the "segments" and "segments.new"
            // files...so it is possible that a previous run of the server crashed exactly in such
            // a way that we have a "segments.new" file but not a "segments" file.  We we will check here 
            // for the special situation that we have a segments.new
            // file but not a segments file...
            File segments = new File(idxPath, "segments");
            if (!segments.exists()) {
                File segments_new = new File(idxPath, "segments.new");
                if (segments_new.exists()) 
                    segments_new.renameTo(segments);
            }
            
            try {
                // must call getDirectory then setLockFactory via 2 calls -- there's the possibility
                // that the directory we're returned is actually a cached FSDirectory (e.g. if the index
                // was deleted and re-created) in which case we should be using the existing LockFactory
                // and not creating a new one
                mIdxDirectory = (Z23FSDirectory)FSDirectory.getDirectory(idxPath);
                if (mIdxDirectory.getLockFactory() == null || !(mIdxDirectory.getLockFactory() instanceof SingleInstanceLockFactory))
                    mIdxDirectory.setLockFactory(new SingleInstanceLockFactory());
            } catch (IOException e) {
                throw ServiceException.FAILURE("Cannot create FSDirectory at path: "+idxPath, e);
            }
        }
    }

    public void addDocument(IndexItem redoOp, Document[] docs, int indexId, long receivedDate, 
        String sortSubject, String sortSender, boolean deleteFirst) throws IOException {
        if (docs.length == 0)
            return;
    
        synchronized(getLock()) {        
            
            beginWriting();
            try {
                assert(mIndexWriter != null);

                for (Document doc : docs) {
                    // doc can be shared by multiple threads if multiple mailboxes
                    // are referenced in a single email
                    synchronized (doc) {
                        doc.removeFields(LuceneFields.L_SORT_SUBJECT);
                        doc.removeFields(LuceneFields.L_SORT_NAME);
                        //                                                                                                  store, index, tokenize
                        doc.add(new Field(LuceneFields.L_SORT_SUBJECT, sortSubject, Field.Store.NO, Field.Index.UN_TOKENIZED));
                        doc.add(new Field(LuceneFields.L_SORT_NAME,    sortSender, Field.Store.NO, Field.Index.UN_TOKENIZED));
                        
                        doc.removeFields(LuceneFields.L_MAILBOX_BLOB_ID);
                        doc.add(new Field(LuceneFields.L_MAILBOX_BLOB_ID, Integer.toString(indexId), Field.Store.YES, Field.Index.UN_TOKENIZED));
                        
                        // If this doc is shared by mult threads, then the date might just be wrong,
                        // so remove and re-add the date here to make sure the right one gets written!
                        doc.removeFields(LuceneFields.L_SORT_DATE);
                        String dateString = DateField.timeToString(receivedDate);
                        doc.add(new Field(LuceneFields.L_SORT_DATE, dateString, Field.Store.YES, Field.Index.UN_TOKENIZED));
                        
                        if (null == doc.get(LuceneFields.L_ALL)) {
                            doc.add(new Field(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE, Field.Store.NO, Field.Index.NO_NORMS, Field.TermVector.NO));
                        }
                        
                        if (deleteFirst) {
                            String itemIdStr = Integer.toString(indexId);
                            Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                            mIndexWriter.updateDocument(toDelete, doc);
                        } else {
                            mIndexWriter.addDocument(doc);
                        }
                        
                    } // synchronized(doc)
                } // foreach Document

                if (redoOp != null) {
                    mUncommittedRedoOps.add(redoOp);
                }
                
                // tim: this might seem bad, since an index in steady-state-of-writes will never get flushed, 
                // however we also track the number of uncomitted-operations on the index, and will force a 
                // flush if the index has had a lot written to it without a flush.
                updateLastWriteTime();
            } finally {
                doneWriting();
            }

        }
    }
    
    public List<Integer> deleteDocuments(List<Integer> itemIds) throws IOException {
        synchronized(getLock()) {
            beginWriting();
            try {
                for (int i = 0; i < itemIds.size(); i++) {
                    try {
                        String itemIdStr = Integer.toString(itemIds.get(i));
                        Term toDelete = new Term(LuceneFields.L_MAILBOX_BLOB_ID, itemIdStr);
                        mIndexWriter.deleteDocuments(toDelete);
                        // NOTE!  The numDeleted may be < you expect here, the document may
                        // already be deleted and just not be optimized out yet -- some lucene
                        // APIs (e.g. docFreq) will still return the old count until the indexes 
                        // are optimized...
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("Deleted index documents for itemId "+itemIdStr);
                        }
                    } catch (IOException ioe) {
                        sLog.debug("deleteDocuments exception on index "+i+" out of "+itemIds.size()+" (id="+itemIds.get(i)+")");
                        List<Integer> toRet = new ArrayList<Integer>(i);
                        for (int j = 0; j < i; j++)
                            toRet.add(itemIds.get(j));
                        return toRet;
                    }
                }
            } finally {
                doneWriting();
            }
            return itemIds; // success
        }
    }

    public void deleteIndex() throws IOException
    {
        synchronized(getLock()) {        
            IndexWriter writer = null;
            try {
                flush();
                // FIXME maybe: under Windows only, this can fail.  Might need way to forcibly close all open indices???
                //              closeIndexReader();
                if (sLog.isDebugEnabled())
                    sLog.debug("****Deleting index " + mIdxDirectory.toString());

                // can use default analyzer here since it is easier, and since we aren't actually
                // going to do any indexing...
                writer = new IndexWriter(mIdxDirectory, true, ZimbraAnalyzer.getDefaultAnalyzer(), true, null);
                
                if (ZimbraLog.index_lucene.isDebugEnabled())
                    writer.setInfoStream(new PrintStream(new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    private void enumerateTermsForField(String regex, Term firstTerm, TermEnumInterface callback) throws IOException
    {
        synchronized(getLock()) {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                IndexReader iReader = searcher.getReader();

                TermEnum terms = iReader.terms(firstTerm);
                boolean hasDeletions = iReader.hasDeletions();
                
                // HACK!
                boolean stripAtBeforeRegex = false;
                if (callback instanceof DomainEnumCallback) 
                    stripAtBeforeRegex = true;
                
                Pattern p = null;
                if (regex != null && regex.length() > 0) {
                    p = Pattern.compile(regex);
                }

                do {
                    Term cur = terms.term();
                    if (cur != null) {
                        if (!cur.field().equals(firstTerm.field())) {
                            break;
                        }

                        boolean skipIt = false;
                        if (p != null) {
                            String compareTo = cur.text();
                            if (stripAtBeforeRegex)
                                if (compareTo.length() > 1 && compareTo.charAt(0)=='@')
                                    compareTo = compareTo.substring(1);
                            if (!(p.matcher(compareTo).matches()))
                                skipIt = true;
                        }
                        
                        if (!skipIt) {
                            // NOTE: the term could exist in docs, but they might all be deleted. Unfortunately this means  
                            // that we need to actually walk the TermDocs enumeration for this document to see if it is
                            // non-empty
                            if ((!hasDeletions) || (iReader.termDocs(cur).next())) {
                                callback.onTerm(cur, terms.docFreq());
                            }
                        }
                    }
                } while (terms.next());
            } finally {
                searcher.release();
            }
        }
        
    }

    /**
     * @return TRUE if all tokens were expanded or FALSE if no more tokens could be expanded
     */
    public boolean expandWildcardToken(Collection<String> toRet, String field, String token, int maxToReturn) throws ServiceException 
    {
        // all lucene text should be in lowercase...
        token = token.toLowerCase();

        try {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                Term firstTerm = new Term(field, token);

                IndexReader iReader = searcher.getReader();

                TermEnum terms = iReader.terms(firstTerm);

                do {
                    Term cur = terms.term();
                    if (cur != null) {
                        if (!cur.field().equals(firstTerm.field())) {
                            break;
                        }

                        String curText = cur.text();

                        if (curText.startsWith(token)) {
                            if (toRet.size() >= maxToReturn) 
                                return false;

                            // we don't care about deletions, they will be filtered later
                            toRet.add(cur.text());
                        } else {
                            if (curText.compareTo(token) > 0)
                                break;
                        }
                    }
                } while (terms.next());

                return true;
            } finally {
                searcher.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }
    }
    
    /**
     * Force all outstanding index writes to go through. Do not return until complete 
     */
    public void flush() {
        synchronized(getLock()) {
            sIndexWritersCache.flush(this);
            sIndexReadersCache.removeIndexReader(this);
        }
    }

    /**
     * @param fieldName - a lucene field (e.g. LuceneFields.L_H_CC)
     * @param collection - Strings which correspond to all of the domain terms stored in a given field.
     * @throws IOException
     */
    public void getDomainsForField(String fieldName, String regex, Collection<BrowseTerm> collection) throws IOException
    {
        if (regex == null)
            regex = "";
        enumerateTermsForField(regex, new Term(fieldName,""),new DomainEnumCallback(collection));
    }
    
    /**
     * @param collection - Strings which correspond to all of the attachment types in the index
     * @throws IOException
     */
    public void getAttachments(String regex, Collection<BrowseTerm> collection) throws IOException
    {
        if (regex == null)
            regex = "";
        enumerateTermsForField(regex, new Term(LuceneFields.L_ATTACHMENTS, ""), new TermEnumCallback(collection));
    }

    public void getObjects(String regex, Collection<BrowseTerm> collection) throws IOException
    {
        if (regex == null)
            regex = "";
        enumerateTermsForField(regex, new Term(LuceneFields.L_OBJECTS, ""), new TermEnumCallback(collection));
    }

  
    /**
     * @return A refcounted RefCountedIndexSearcher for this index.  Caller is responsible for 
     *            calling RefCountedIndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     * 
     * @throws IOException
     */
    public RefCountedIndexSearcher getCountedIndexSearcher() throws IOException
    {
        synchronized(getLock()) {        
            RefCountedIndexSearcher searcher = null;
            RefCountedIndexReader cReader = getCountedIndexReader();
            searcher = new RefCountedIndexSearcher(cReader);
            return searcher;
        }
    }

    public String toString() { return "LuceneIndex at "+mIdxDirectory.toString(); }

    long getLastWriteTime() { return mLastWriteTime; }

    private final Object getLock() { return mMbidx.getLock(); }

    public Sort getSort(SortBy searchOrder) {
        synchronized(getLock()) {
            if (searchOrder != mLatestSortBy) { 
                switch (searchOrder) {
                    case NONE:
                        return null;
                    case DATE_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case DATE_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, false));
                        mLatestSortBy = searchOrder;
                        break;
                    case SUBJ_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case SUBJ_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.STRING, false));
                        mLatestSortBy = searchOrder;
                        break;
                    case NAME_DESCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, true));
                        mLatestSortBy = searchOrder;
                        break;
                    case NAME_ASCENDING:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.STRING, false));
                        mLatestSortBy = searchOrder;
                        break;
                    case SCORE_DESCENDING:
                        return null;
                    default:
                        mLatestSort = new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.STRING, true));
                       mLatestSortBy = SortBy.DATE_ASCENDING;
                }
            }
            return mLatestSort;
        }
    }
    
    public List<SpellSuggestQueryInfo.Suggestion> suggestSpelling(String field, String token) throws ServiceException {
        LinkedList<SpellSuggestQueryInfo.Suggestion> toRet = null;

        token = token.toLowerCase();
        
        try {
            RefCountedIndexSearcher searcher = this.getCountedIndexSearcher();
            try {
                IndexReader iReader = searcher.getReader();
                
                Term term = new Term(field, token);
                int freq = iReader.docFreq(term);
                int numDocs = iReader.numDocs();

                if (freq == 0 && numDocs > 0) {
                    toRet = new LinkedList<SpellSuggestQueryInfo.Suggestion>();

//                    float frequency = ((float)freq)/((float)numDocs);
//
//                    int suggestionDistance = Integer.MAX_VALUE;

                    FuzzyTermEnum fuzzyEnum = new FuzzyTermEnum(iReader, term, 0.5f, 1);
                    if (fuzzyEnum != null) {
                        do {
                            Term cur = fuzzyEnum.term();
                            if (cur != null) {
                                String curText = cur.text();
                                int curDiff = editDistance(curText, token, curText.length(), token.length());
                                
                                SpellSuggestQueryInfo.Suggestion sug = new SpellSuggestQueryInfo.Suggestion();
                                sug.mStr = curText;
                                sug.mEditDist = curDiff;
                                sug.mDocs = fuzzyEnum.docFreq();
                                toRet.add(sug);
                            }
                        } while(fuzzyEnum.next());
                    }
                }
            } finally {
                searcher.release();
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException opening index", e);
        }

        return toRet;
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            if (mIdxDirectory != null) 
                mIdxDirectory.close();
            mIdxDirectory = null;
        } finally {
            super.finalize();
        }
    }

    /**
    Levenshtein distance also known as edit distance is a measure of similiarity
    between two strings where the distance is measured as the number of character 
    deletions, insertions or substitutions required to transform one string to 
    the other string. 
    <p>This method takes in four parameters; two strings and their respective 
    lengths to compute the Levenshtein distance between the two strings.
    The result is returned as an integer.
     */ 
    private final int editDistance(String s, String t, int n, int m) {
        if (e.length <= n || e[0].length <= m) {
            e = new int[Math.max(e.length, n+1)][Math.max(e[0].length, m+1)];
        }
        int d[][] = e; // matrix
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s

        if (n == 0) return m;
        if (m == 0) return n;

        // init matrix d
        for (i = 0; i <= n; i++) d[i][0] = i;
        for (j = 0; j <= m; j++) d[0][j] = j;

        // start computing edit distance
        for (i = 1; i <= n; i++) {
            s_i = s.charAt(i - 1);
            for (j = 1; j <= m; j++) {
                if (s_i != t.charAt(j-1))
                    d[i][j] = min(d[i-1][j], d[i][j-1], d[i-1][j-1])+1;
                else d[i][j] = min(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]);
            }
        }

        // we got the result!
        return d[n][m];
    }
    
    /**
     * @return A refcounted IndexReader for this index.  Caller is responsible for 
     *            calling IndexReader.release() on the index before allowing it to go
     *            out of scope (otherwise a RuntimeException will occur)
     * 
     * @throws IOException
     */
    private RefCountedIndexReader getCountedIndexReader() throws IOException
    {
        BooleanQuery.setMaxClauseCount(10000); 

        synchronized(getLock()) {
        	sIndexWritersCache.flush(this); // flush writer if writing
        	
            RefCountedIndexReader toRet = sIndexReadersCache.getIndexReader(this);
            if (toRet != null)
                return toRet;
            
            IndexReader reader = null;
            try {
                reader = IndexReader.open(mIdxDirectory);
            } catch(IOException e) {
                // Handle the special case of trying to open a not-yet-created
                // index, by opening for write and immediately closing.  Index
                // directory should get initialized as a result.
                File indexDir = mIdxDirectory.getFile();
                if (indexDirIsEmpty(indexDir)) {
                    beginWriting();
                    doneWriting();
                    flush();
                    try {
                        reader = IndexReader.open(mIdxDirectory);
                    } catch (IOException e1) {
                        if (reader != null)
                            reader.close();
                        throw e1;
                    }
                } else {
                    if (reader != null)
                        reader.close();
                    throw e;
                }
            }
            
            synchronized(mOpenReaders) {
                toRet = new RefCountedIndexReader(this, reader); // refcount starts at 1
                mOpenReaders.add(toRet);
            }
                
            sIndexReadersCache.putIndexReader(this, toRet); // addrefs if put in cache
            return toRet; 
        }
    }
    
    /**
     * Check to see if it is OK for us to create an index in the specified 
     * directory.
     * 
     * @param indexDir
     * @return TRUE if the index directory is empty or doesn't exist,
     *             FALSE if the index directory exists and has files in it  
     * @throws IOException
     */
    private boolean indexDirIsEmpty(File indexDir) {
        if (!indexDir.exists()) {
            // dir doesn't even exist yet.  Create the parents and return true
            indexDir.mkdirs();
            return true;
        }
        
        // Empty directory is okay, but a directory with any files
        // implies index corruption.
        File[] files = indexDir.listFiles();
        int numFiles = 0;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String fname = f.getName();
            if (f.isDirectory() && (fname.equals(".") || fname.equals("..")))
                continue;
            numFiles++;
        }
        return (numFiles <= 0);
    }
    
    
    private void doneWriting() throws IOException {
        assert(Thread.holdsLock(getLock()));
        if (mUncommittedRedoOps.size() > sMaxUncommittedOps) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Flushing " + toString() + " because of too many uncommitted redo ops");
            }
            flush();
        } else {
            sIndexWritersCache.doneWriting(this);
        }
        updateLastWriteTime();
    }
    
    private void beginWriting() throws IOException
    {
        assert(Thread.holdsLock(getLock()));
        
        // uncache the IndexReader if it is cached
        sIndexReadersCache.removeIndexReader(this);
        
        sIndexWritersCache.beginWriting(this);
    }
    
    void doWriterOpen() throws IOException {
        if (mIndexWriter != null)
            return; // already open!
        
        assert(Thread.holdsLock(getLock()));

        boolean useBatchIndexing;
        try {
            useBatchIndexing = mMbidx.useBatchedIndexing();
        } catch (ServiceException e) {
            throw new IOException("Caught IOException checking BatchedIndexing flag "+e);
        }
        
        final LuceneConfigSettings.Config config;
        if (useBatchIndexing) {
            config = LuceneConfigSettings.batched;
        } else {
            config = LuceneConfigSettings.nonBatched;
        }
        
        try {
//          sLog.debug("MI"+this.toString()+" Opening IndexWriter(1) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());
            mIndexWriter = new IndexWriter(mIdxDirectory, config.autocommit, mMbidx.getAnalyzer(), false, null);
            if (ZimbraLog.index_lucene.isDebugEnabled())
                mIndexWriter.setInfoStream(new PrintStream(new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
//          sLog.debug("MI"+this.toString()+" Opened IndexWriter(1) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());

        } catch (IOException e1) {
            sLog.debug("Caught exception trying to open index: "+e1, e1);
            File indexDir  = mIdxDirectory.getFile();
            if (indexDirIsEmpty(indexDir)) {
//              sLog.debug("MI"+this.toString()+" Opening IndexWriter(2) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());
                mIndexWriter = new IndexWriter(mIdxDirectory, config.autocommit, mMbidx.getAnalyzer(), true, null);
                if (ZimbraLog.index_lucene.isDebugEnabled())
                    mIndexWriter.setInfoStream(new PrintStream(new LoggingOutputStream(ZimbraLog.index_lucene, Log.Level.debug)));
                
//              sLog.debug("MI"+this.toString()+" Opened IndexWriter(2) "+ writer+" for "+this+" dir="+mIdxDirectory.toString());
                if (mIndexWriter == null) 
                    throw new IOException("Failed to open IndexWriter in directory "+indexDir.getAbsolutePath());
            } else {
                mIndexWriter = null;
                IOException ioe = new IOException("Could not create index " + mIdxDirectory.toString() + " (directory already exists)");
                ioe.initCause(e1);
                throw ioe;
            }
        }

        if (config.useSerialMergeScheduler)
            mIndexWriter.setMergeScheduler(new SerialMergeScheduler());
        
        mIndexWriter.setMaxBufferedDocs(config.maxBufferedDocs);
        mIndexWriter.setRAMBufferSizeMB(((double)config.ramBufferSizeKB)/1024.0);
        mIndexWriter.setMergeFactor(config.mergeFactor);
        
        if (config.useDocScheduler) {
            LogDocMergePolicy policy = new LogDocMergePolicy();
            mIndexWriter.setMergePolicy(policy);
            policy.setUseCompoundDocStore(config.useCompoundFile);
            policy.setUseCompoundFile(config.useCompoundFile);
            policy.setMergeFactor(config.mergeFactor);
            policy.setMinMergeDocs((int)config.minMerge);
            if (config.maxMerge != Integer.MAX_VALUE) 
                policy.setMaxMergeDocs((int)config.maxMerge);
        } else {
            LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
            mIndexWriter.setMergePolicy(policy);
            policy.setUseCompoundDocStore(config.useCompoundFile);
            policy.setUseCompoundFile(config.useCompoundFile);
            policy.setMergeFactor(config.mergeFactor);
            policy.setMinMergeMB(((double)config.minMerge)/1024.0);
            if (config.maxMerge != Integer.MAX_VALUE)
                policy.setMaxMergeMB(((double)config.maxMerge)/1024.0);
        }
    }
    
    void doWriterClose() {
        if (mIndexWriter == null) {
            return;
        }

        if (sLog.isDebugEnabled())
            sLog.debug("Closing IndexWriter " + mIndexWriter + " for " + this);

        IndexWriter writer = mIndexWriter;
        mIndexWriter = null;

        boolean success = true;
        try {
            // Flush all changes to file system before committing redos.
            writer.close();
        } catch (IOException e) {
            success = false;
            sLog.error("Caught Exception " + e + " in LuceneIndex.closeIndexWriter", e);
            // TODO: Is it okay to eat up the exception?
        } finally {
            // Write commit entries to redo log for all IndexItem entries
            // whose changes were written to disk by mIndexWriter.close()
            // above.
            for (Iterator<IndexItem> iter = mUncommittedRedoOps.iterator(); iter.hasNext();) {
                IndexItem op = iter.next();
                if (success) {
                    if (op.commitAllowed())
                        op.commit();
                    else {
                        if (sLog.isDebugEnabled()) {
                            sLog.debug("IndexItem (" + op +
                            ") not allowed to commit yet; attaching to parent operation");
                        }
                        op.attachToParent();
                    }
                } else
                    op.abort();
                iter.remove();
            }
            assert(mUncommittedRedoOps.size() == 0);
        }
    }
    

    private void updateLastWriteTime() { mLastWriteTime = System.currentTimeMillis(); }

    private static IndexReadersCache sIndexReadersCache;
    private static IndexWritersCache sIndexWritersCache;
    
    private static Log sLog = ZimbraLog.index;
    
    /**
     * If documents are being constantly added to an index, then it will stay at the front of the LRU cache
     * and will never flush itself to disk: this setting specifies the maximum number of writes we will allow
     * to the index before we force a flush.  Higher values will improve batch-add performance, at the cost
     * of longer-lived transactions in the redolog.
     */
    private static int sMaxUncommittedOps;
    
    /**
     * This static array saves us from the time required to create a new array
     * everytime editDistance is called.
     */
    private int e[][] = new int[1][1];
    private Z23FSDirectory mIdxDirectory = null;
    
    private IndexWriter mIndexWriter;
    
    private volatile long mLastWriteTime = 0;
    
    private Sort mLatestSort = null;
    private SortBy mLatestSortBy = null;
    private MailboxIndex mMbidx;
    private ArrayList<IndexItem>mUncommittedRedoOps = new ArrayList<IndexItem>();
    
    
    static abstract class DocEnumInterface {
        void maxDocNo(int num) {};
        abstract boolean onDocument(Document doc, boolean isDeleted);
    }
    static class DomainEnumCallback implements TermEnumInterface {
        DomainEnumCallback(Collection<BrowseTerm> collection) {
            mCollection = collection;
        }

        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1 && text.charAt(0) == '@') {
                mCollection.add(new BrowseTerm(text.substring(1), docFreq));
            }           
        }
        private Collection<BrowseTerm> mCollection;
    }
    static class TermEnumCallback implements TermEnumInterface {
        TermEnumCallback(Collection<BrowseTerm> collection) {
            mCollection = collection;
        }

        public void onTerm(Term term, int docFreq) {
            String text = term.text();
            if (text.length() > 1) {
                mCollection.add(new BrowseTerm(text, docFreq));
            }           
        }
        private Collection<BrowseTerm> mCollection;
    }
    interface TermEnumInterface {
        abstract void onTerm(Term term, int docFreq); 
    }
    
    public void onReaderClose(RefCountedIndexReader ref) {
        synchronized(mOpenReaders) {
            mOpenReaders.remove(ref);
        }
    }
    
    private List<RefCountedIndexReader> mOpenReaders = new ArrayList<RefCountedIndexReader>();
    
    public IndexReader reopenReader(IndexReader reader) throws IOException {
        return reader.reopen();
    }
}
