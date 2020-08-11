package com.mesilat.confield;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.impl.AbstractCustomFieldIndexer;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.mesilat.jira.LuceneFieldCreator;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceFieldIndexer extends AbstractCustomFieldIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");
    private final LuceneFieldCreator luceneFieldCreator;

    @Override
    public void addDocumentFieldsSearchable(Document doc, Issue issue) {
        Object value = customField.getValue(issue);
        
        if (value != null){
            ObjectMapper mapper = new ObjectMapper();
            try {
                ArrayNode arr = (ArrayNode)mapper.readTree(String.format("[%s]", value));
                arr.forEach(n -> {
                    if (n.isObject()){
                        ObjectNode obj = (ObjectNode)n;
                        if (!obj.has("id")){
                            return;
                        }
                        String id = obj.get("id").asText();                            
                        luceneFieldCreator.addSearchableField(doc, customField.getId(), id);
                    }
                });
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to parse value %s", value), ex);
            }
        }
    }
    @Override
    public void addDocumentFieldsNotSearchable(Document doc, Issue issue) {
        Object value = customField.getValue(issue);
        
        if (value != null){
            ObjectMapper mapper = new ObjectMapper();
            try {
                ArrayNode arr = (ArrayNode)mapper.readTree(String.format("[%s]", value));
                arr.forEach(n -> {
                    if (n.isObject()){
                        ObjectNode obj = (ObjectNode)n;
                        if (!obj.has("id")){
                            return;
                        }
                        String id = obj.get("id").asText();                            
                        luceneFieldCreator.addNotSearchableField(doc, customField.getId(), id);
                    }
                });
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to parse value %s", value), ex);
            }
        }
    }

    public ConfluenceFieldIndexer(FieldVisibilityManager fieldVisibilityManager, CustomField customField, LuceneFieldCreator luceneFieldCreator){
        super(fieldVisibilityManager, customField);
        this.luceneFieldCreator = luceneFieldCreator;
    }
}