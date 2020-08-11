package com.mesilat.confield;

import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.util.IndexValueConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceFieldIndexValueConverter implements IndexValueConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final long fieldId;
    private final DataService dataService;

    /**
     * Convert page name to page id (used as index value)
     * @param rawValue Page name
     * @return Page Id
     */
    @Override
    public String convertToIndexValue(QueryLiteral rawValue) {
        if (rawValue == null){
            return null;
        }

        Long pageId = dataService.getPageId(fieldId, rawValue.asString());
        if (pageId == null){
            LOGGER.trace(String.format("Page ID not found for %s", rawValue.asString()));
            return rawValue.asString();
        } else {
            LOGGER.trace(String.format("Page ID found in supplementary index"));
            return Long.toString(pageId);
        }
    }

    public ConfluenceFieldIndexValueConverter(long fieldId, DataService dataService){
        this.fieldId = fieldId;
        this.dataService = dataService;
    }
}