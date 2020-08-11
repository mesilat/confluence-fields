package com.mesilat.confield;

import com.atlassian.jira.issue.customfields.searchers.CustomFieldSearcherClauseHandler;
import com.atlassian.jira.issue.customfields.searchers.MultiSelectSearcher;
import com.atlassian.jira.issue.customfields.searchers.information.CustomFieldSearcherInformation;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.searchers.information.SearcherInformation;
import com.atlassian.jira.issue.search.searchers.renderer.SearchRenderer;
import com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer;
import com.atlassian.jira.issue.statistics.TextFieldSorter;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.util.JiraComponentFactory;
import com.atlassian.jira.util.JiraComponentLocator;
import com.atlassian.jira.web.FieldVisibilityManager;

public abstract class AbstractFieldSearcher extends MultiSelectSearcher {
    protected final FieldVisibilityManager fieldVisibilityManager;
    protected final DataService dataService;
    protected final CustomFieldInputHelper customFieldInputHelper;
    protected final JqlOperandResolver jqlOperandResolver;

    protected volatile CustomFieldSearcherInformation searcherInformation;
    protected volatile SearchInputTransformer searchInputTransformer;
    protected volatile SearchRenderer searchRenderer;
    protected volatile CustomFieldSearcherClauseHandler customFieldSearcherClauseHandler;

    @Override
    public abstract void init(CustomField field);

    @Override
    public SearcherInformation<CustomField> getSearchInformation() {
        if (searcherInformation == null) {
            throw new IllegalStateException("FieldSearcher was not yet initialized");
        }
        return searcherInformation;
    }
    @Override
    public SearchInputTransformer getSearchInputTransformer() {
        if (searchInputTransformer == null) {
            throw new IllegalStateException("FieldSearcher was not yet initialized");
        }
        return searchInputTransformer;
    }
    @Override
    public SearchRenderer getSearchRenderer() {
        if (searchRenderer == null){
            throw new IllegalStateException("FieldSearcher was not yet initialized");
        }
        return searchRenderer;
    }
    @Override
    public CustomFieldSearcherClauseHandler getCustomFieldSearcherClauseHandler() {
        if (customFieldSearcherClauseHandler == null) {
            throw new IllegalStateException("FieldSearcher was not yet initialized");
        }
        return customFieldSearcherClauseHandler;
    }
    @Override
    public LuceneFieldSorter getSorter(CustomField cf) {
        return new TextFieldSorter(cf.getId());
    }

    public AbstractFieldSearcher(
        FieldVisibilityManager fieldVisibilityManager,
        CustomFieldInputHelper customFieldInputHelper,
        JqlOperandResolver operandResolver,
        DataService dataService
    ){
        super(new JiraComponentLocator(), JiraComponentFactory.getInstance());
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.customFieldInputHelper = customFieldInputHelper;
        this.jqlOperandResolver = operandResolver;
        this.dataService = dataService;
    }
}