package com.mesilat.confield;

import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.customfields.searchers.renderer.CustomFieldRenderer;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.issue.search.searchers.util.SearchContextRenderHelper;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.plugin.customfield.CustomFieldSearcherModuleDescriptor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.FieldVisibilityManager;
import webwork.action.Action;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.node.ObjectNode;

public abstract class AbstractFieldSearchRenderer extends CustomFieldRenderer {
    private final CustomField customField;
    private final CustomFieldValueProvider customFieldValueProvider;

    @Override
    public String getEditHtml(
        final ApplicationUser user,
        SearchContext searchContext,
        FieldValuesHolder fieldValuesHolder,
        Map<?,?> displayParameters,
        Action action
    ){
        HashMap<String, Object> velocityParams = new HashMap<>();

        velocityParams.put("fieldkey", customField.getCustomFieldType().getKey());
        velocityParams.put("allOptions", getValidOptions());

        SearchContextRenderHelper.addSearchContextParams(searchContext, velocityParams);
        return getEditHtml(searchContext, fieldValuesHolder, displayParameters, action, velocityParams);
    }

    @Override
    public String getViewHtml(
        final ApplicationUser user,
        SearchContext searchContext,
        FieldValuesHolder fieldValuesHolder,
        Map<?,?> displayParameters,
        Action action
    ){
        HashMap<String, Object> velocityParams = new HashMap<>();

        velocityParams.put("fieldkey", customField.getCustomFieldType().getKey());
        velocityParams.put("selectedOptions", getSelectedOptions(fieldValuesHolder));

        SearchContextRenderHelper.addSearchContextParams(searchContext, velocityParams);
        return super.getViewHtml(searchContext, fieldValuesHolder, displayParameters, action, velocityParams);
    }

    private Set<ValueOption> getSelectedOptions(final FieldValuesHolder fieldValuesHolder) {
        @SuppressWarnings({"unchecked"})
        Collection<String> selectedOptionIds = (Collection<String>)customFieldValueProvider.getStringValue(customField, fieldValuesHolder);
        Set<ValueOption> options = new HashSet<>();
        selectedOptionIds.forEach(selectedOptionId -> {
            options.add(new ValueOption(selectedOptionId));
        });
        return options;
    }
    protected abstract List<ValueOption> getValidOptions();

    public AbstractFieldSearchRenderer(
            ClauseNames clauseNames,
            CustomFieldSearcherModuleDescriptor customFieldSearcherModuleDescriptor,
            CustomField customField,
            CustomFieldValueProvider customFieldValueProvider,
            FieldVisibilityManager fieldVisibilityManager) {
        super(clauseNames, customFieldSearcherModuleDescriptor, customField, customFieldValueProvider, fieldVisibilityManager);
        this.customField = customField;
        this.customFieldValueProvider = customFieldValueProvider;
    }

    public static class ValueOption {
        private final String title;

        public String getOptionId(){
            return title;
        }
        public String getValue(){
            return title;
        }

        public ValueOption(String title){
            this.title = title;
        }
        public ValueOption(ObjectNode obj){
            if (obj != null && obj.has("title") && obj.get("title").isTextual()){
                title = obj.get("title").asText();
            } else {
                throw new IllegalArgumentException();
            }
        }
    }
}