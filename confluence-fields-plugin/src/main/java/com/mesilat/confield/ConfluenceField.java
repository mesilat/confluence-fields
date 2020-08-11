package com.mesilat.confield;

import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.webresource.api.assembler.PageBuilderService;

@Scanned
public class ConfluenceField extends FieldBase {
    private static final String FIELD_TYPE = "confluence-page";

    @Override
    public String getFieldType() {
        return FIELD_TYPE;
    }

    public ConfluenceField(
        @JiraImport CustomFieldValuePersister customFieldValuePersister,
        @JiraImport GenericConfigManager genericConfigManager,
        @JiraImport TextFieldCharacterLengthValidator textFieldCharacterLengthValidator,
        @JiraImport JiraAuthenticationContext jiraAuthenticationContext,
        @JiraImport PageBuilderService pageBuilderService,
        DataService dataService
    ){
        super(
            customFieldValuePersister,
            genericConfigManager,
            textFieldCharacterLengthValidator,
            jiraAuthenticationContext,
            pageBuilderService,
            dataService
        );
    }
}