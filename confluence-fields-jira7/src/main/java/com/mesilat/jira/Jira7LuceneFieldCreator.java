package com.mesilat.jira;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

public class Jira7LuceneFieldCreator implements LuceneFieldCreator {
    @Override
    public void addSearchableField(Document doc, String name, String value) {
        doc.add(new Field(name, value, Store.YES, Index.NOT_ANALYZED));
    }
    @Override
    public void addNotSearchableField(Document doc, String name, String value) {
        doc.add(new Field(name, value, Store.YES, Index.NO));
    }
}