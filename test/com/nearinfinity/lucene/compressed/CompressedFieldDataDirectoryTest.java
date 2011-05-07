package com.nearinfinity.lucene.compressed;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

public class CompressedFieldDataDirectoryTest {
    
    @Test
    public void testCompressedFieldDataDirectoryBasic() throws CorruptIndexException, IOException {
        RAMDirectory dir = new RAMDirectory();
        CompressionCodec compression = new DeflaterCompressionCodec();
        CompressedFieldDataDirectory directory = new CompressedFieldDataDirectory(dir, compression);
        IndexWriter writer = new IndexWriter(directory, new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);
        addDocs(writer,0,10);
        writer.close();
        testFetches(directory);
    }
    
    @Test
    public void testCompressedFieldDataDirectoryTransition() throws CorruptIndexException, LockObtainFailedException, IOException {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);
        addDocs(writer,0,5);
        writer.close();
        
        CompressionCodec compression = new DeflaterCompressionCodec();
        CompressedFieldDataDirectory directory = new CompressedFieldDataDirectory(dir, compression);
        writer = new IndexWriter(directory, new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);
        addDocs(writer,5,5);
        writer.close();
        testFetches(directory);
    }
    
    @Test
    public void testCompressedFieldDataDirectoryMixedBlockSize() throws CorruptIndexException, LockObtainFailedException, IOException {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);
        addDocs(writer,0,5);
        writer.close();
        
        CompressionCodec compression = new DeflaterCompressionCodec();
        CompressedFieldDataDirectory directory1 = new CompressedFieldDataDirectory(dir, compression, 2);
        writer = new IndexWriter(directory1, new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);
        addDocs(writer,5,2);
        writer.close();
        
        CompressedFieldDataDirectory directory2 = new CompressedFieldDataDirectory(dir, compression, 4);
        writer = new IndexWriter(directory2, new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);
        addDocs(writer,7,3);
        writer.close();
        testFetches(directory2);
    }
    
    private void testFetches(Directory directory) throws CorruptIndexException, IOException {
        IndexReader reader = IndexReader.open(directory);
        for (int i = 0; i < reader.maxDoc(); i++) {
            String id = Integer.toString(i);
            Document document = reader.document(i);
            assertEquals(id,document.get("id"));
        }
    }

    private void addDocs(IndexWriter writer, int starting, int amount) throws CorruptIndexException, IOException {
        for (int i = 0; i < amount; i++) {
            int index = starting + i;
            writer.addDocument(getDoc(index));
        }
    }

    private Document getDoc(int index) {
        Document document = new Document();
        document.add(new Field("id",Integer.toString(index),Store.YES,Index.NOT_ANALYZED_NO_NORMS));
        return document;
    }

}
