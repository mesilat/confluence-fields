package com.mesilat.confield;

import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.transport.FieldValuesHolder;

public class ConfluenceFieldValueProvider implements CustomFieldValueProvider {
    
    @Override
    public Object getStringValue(CustomField customField, FieldValuesHolder fieldValuesHolder) {
        CustomFieldParams customFieldParams = customField.getCustomFieldValues(fieldValuesHolder);
        return customFieldParams.getValuesForNullKey();
    }

    @Override
    public Object getValue(CustomField customField, FieldValuesHolder fieldValuesHolder) {
        CustomFieldType customFieldType = customField.getCustomFieldType();
        final CustomFieldParams customFieldParams = customField.getCustomFieldValues(fieldValuesHolder);
        return customFieldType.getValueFromCustomFieldParams(customFieldParams);
    }
}