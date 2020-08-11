package com.mesilat.confield;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.query.ActualValueEqualityQueryFactory;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.GenericClauseQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.clause.TerminalClause;

import java.util.ArrayList;
import java.util.List;

public class ConfluenceFieldClauseQueryFactory implements ClauseQueryFactory {
    private final ClauseQueryFactory delegateClauseQueryFactory;

    @Override
    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause) {
        return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
    }

    public ConfluenceFieldClauseQueryFactory(String documentConstant, DataService dataService, JqlOperandResolver operandResolver, CustomField field) {
        final List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<>();
        operatorFactories.add(new ActualValueEqualityQueryFactory(new ConfluenceFieldIndexValueConverter(field.getIdAsLong(), dataService)));
        delegateClauseQueryFactory = new GenericClauseQueryFactory(documentConstant, operatorFactories, operandResolver);
    }
}