package com.mesilat.confield;

import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.bc.issue.search.QueryContextConverter;
import com.atlassian.jira.issue.customfields.searchers.SimpleCustomFieldContextValueGeneratingClauseHandler;
import com.atlassian.jira.issue.customfields.searchers.information.CustomFieldSearcherInformation;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.jql.context.MultiClauseDecoratorContextFactory;
import com.atlassian.jira.jql.context.SelectCustomFieldClauseContextFactory;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.util.JqlSelectOptionsUtil;
import com.atlassian.jira.util.JiraComponentFactory;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.component.ComponentLocator;
import com.mesilat.jira.LuceneFieldCreator;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Scanned
public class ConfluenceFieldSearcher extends AbstractFieldSearcher {
    private final LuceneFieldCreator luceneFieldCreator;

    @Override
    public void init(CustomField field) {
        final FieldIndexer indexer = new ConfluenceFieldIndexer(fieldVisibilityManager, field, luceneFieldCreator);
        final ClauseNames clauseNames = field.getClauseNames();
        final JqlSelectOptionsUtil jqlSelectOptionsUtil = ComponentLocator.getComponent(JqlSelectOptionsUtil.class);
        final QueryContextConverter queryContextConverter = ComponentLocator.getComponent(QueryContextConverter.class);

        this.searcherInformation = new CustomFieldSearcherInformation(
            field.getId(),
            field.getNameKey(),
            Collections.<FieldIndexer>singletonList(indexer),
            new AtomicReference<>(field)
        );
        
        this.searchInputTransformer = new ConfluenceFieldSearchInputTransformer(
            searcherInformation.getId(),
            clauseNames,
            field,
            jqlOperandResolver,
            jqlSelectOptionsUtil,
            queryContextConverter,
            customFieldInputHelper
        );

        this.searchRenderer = new ConfluenceFieldSearchRenderer(
            clauseNames,
            getDescriptor(),
            field,
            new ConfluenceFieldValueProvider(),
            fieldVisibilityManager,
            dataService
        );

        this.customFieldSearcherClauseHandler = new SimpleCustomFieldContextValueGeneratingClauseHandler(
            new ConfluenceFieldValidator(),
            new ConfluenceFieldClauseQueryFactory(field.getId(), dataService, jqlOperandResolver, field),
            ComponentLocator.getComponent(MultiClauseDecoratorContextFactory.Factory.class).create(
                JiraComponentFactory.getInstance().createObject(
                    SelectCustomFieldClauseContextFactory.class,
                    field
                ),
                false
            ),
            new ConfluenceFieldClauseValuesGenerator(field.getIdAsLong(), dataService),
            OperatorClasses.EQUALITY_OPERATORS,
            JiraDataTypes.OPTION
        );
    }

    public ConfluenceFieldSearcher(
        @JiraImport FieldVisibilityManager fieldVisibilityManager,
        @JiraImport CustomFieldInputHelper customFieldInputHelper,
        @JiraImport JqlOperandResolver operandResolver,
        DataService dataService,
        LuceneFieldCreator luceneFieldCreator
    ){
        super(fieldVisibilityManager, customFieldInputHelper, operandResolver, dataService);
        this.luceneFieldCreator = luceneFieldCreator;
    }
}