package com.mesilat.jira;

import org.apache.lucene.document.Document;

public interface LuceneFieldCreator {
    void addSearchableField(Document doc, String name, String value);
    void addNotSearchableField(Document doc, String name, String value);
}