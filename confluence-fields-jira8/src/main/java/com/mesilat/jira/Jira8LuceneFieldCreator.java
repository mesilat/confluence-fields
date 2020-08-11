package com.mesilat.jira;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.BytesRef;

public class Jira8LuceneFieldCreator implements LuceneFieldCreator {
    @Override
    public void addSearchableField(Document doc, String name, String value) {
        doc.add(new StringField(name, value, Store.YES));
        doc.add(new SortedSetDocValuesField(name, new BytesRef(value)));
    }
    @Override
    public void addNotSearchableField(Document doc, String name, String value) {
        doc.add(new StoredField(name, value));
    }
}