package com.mesilat.confield;

import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.plugin.customfield.CustomFieldSearcherModuleDescriptor;
import com.atlassian.jira.web.FieldVisibilityManager;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.node.ArrayNode;

import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceFieldSearchRenderer extends AbstractFieldSearchRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final DataService dataService;

    @Override
    protected List<ValueOption> getValidOptions() {
        List<ValueOption> options = new ArrayList<>();
        ArrayNode arr = dataService.getValues(getField().getIdAsLong());
        arr.forEach(n -> {
            if (n instanceof ObjectNode){
                options.add(new ValueOption((ObjectNode)n));
            }
        });
        return options;
    }

    public ConfluenceFieldSearchRenderer(
            ClauseNames clauseNames,
            CustomFieldSearcherModuleDescriptor customFieldSearcherModuleDescriptor,
            CustomField field,
            CustomFieldValueProvider customFieldValueProvider,
            FieldVisibilityManager fieldVisibilityManager,
            DataService dataService
    ) {
        super(clauseNames, customFieldSearcherModuleDescriptor, field, customFieldValueProvider, fieldVisibilityManager);
        this.dataService = dataService;
    }
}