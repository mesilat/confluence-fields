package com.mesilat.confield;

import com.atlassian.jira.bc.issue.search.QueryContextConverter;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.customfields.searchers.transformer.MultiSelectCustomFieldSearchInputTransformer;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.customfields.view.CustomFieldParamsImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer;
import com.atlassian.jira.issue.search.searchers.transformer.SimpleNavigatorCollectorVisitor;
import com.atlassian.jira.issue.transport.ActionParams;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.util.JqlSelectOptionsUtil;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.operand.SingleValueOperand;
import com.atlassian.query.operator.Operator;
import java.util.Collections;

public class ConfluenceFieldSearchInputTransformer extends MultiSelectCustomFieldSearchInputTransformer implements SearchInputTransformer {
    private final ClauseNames clauseNames;

    @Override
    public void populateFromParams(ApplicationUser au, FieldValuesHolder fvh, ActionParams ap) {
        super.populateFromParams(au, fvh, ap);
    }
    
    @Override
    public void validateParams(
        final ApplicationUser user,
        final SearchContext searchContext,
        final FieldValuesHolder fieldValuesHolder,
        final I18nHelper i18n,
        final ErrorCollection errors
    ){
    }

    @Override
    public void populateFromQuery(ApplicationUser au, FieldValuesHolder fvh, Query query, SearchContext sc) {
        super.populateFromQuery(au, fvh, query, sc);
    }
    
    @Override
    public boolean doRelevantClausesFitFilterForm(
        final ApplicationUser user,
        final Query query,
        final SearchContext searchContext
    ){
        final NavigatorConversionResult result = convertForNavigator(query);
        if (!result.fitsNavigator()) {
            return false;
        } else if (result.getValue() == null) {
            return true;
        } else {
            final SingleValueOperand value = result.getValue();
            String objectName = value.getStringValue() == null ? value.getLongValue().toString() : value.getStringValue();
            // TODO: check 
            return objectName != null;
        }
    }
    @Override
    protected CustomFieldParams getParamsFromSearchRequest(
        final ApplicationUser user,
        final Query query,
        final SearchContext searchContext
    ){
        final NavigatorConversionResult result = convertForNavigator(query);
        if (result.fitsNavigator() && result.getValue() != null) {
            final SingleValueOperand value = result.getValue();
            final String objectName = value.getStringValue() == null ? value.getLongValue().toString() : value.getStringValue();
            if (objectName != null) {
                return new CustomFieldParamsImpl(getCustomField(), Collections.singleton(objectName));
            }
        }
        return null;
    }

    @Override
    public Clause getSearchClause(ApplicationUser au, FieldValuesHolder fvh) {
        return super.getSearchClause(au, fvh);
    }
    
    private NavigatorConversionResult convertForNavigator(final Query query) {
        SimpleNavigatorCollectorVisitor collectorVisitor = createSimpleNavigatorCollectorVisitor();
        final NavigatorConversionResult result;
        if (query != null && query.getWhereClause() != null) {
            query.getWhereClause().accept(collectorVisitor);
            if (!collectorVisitor.isValid()) {
                result = new NavigatorConversionResult(false, null);
            } else if (collectorVisitor.getClauses().isEmpty()) {
                result = new NavigatorConversionResult(true, null);
            } else if (collectorVisitor.getClauses().size() == 1 &&
                    checkOperand(collectorVisitor.getClauses().get(0).getOperator()) &&
                    collectorVisitor.getClauses().get(0).getOperand() instanceof SingleValueOperand) {
                result = new NavigatorConversionResult(true, (SingleValueOperand) collectorVisitor.getClauses().get(0).getOperand());
            } else {
                result = new NavigatorConversionResult(false, null);
            }
        } else {
            result = new NavigatorConversionResult(true, null);
        }
        return result;
    }
    private SimpleNavigatorCollectorVisitor createSimpleNavigatorCollectorVisitor() {
        return new SimpleNavigatorCollectorVisitor(clauseNames.getJqlFieldNames());
    }
    private boolean checkOperand(final Operator operator) {
        return operator == Operator.EQUALS || operator == Operator.IS || operator == Operator.LIKE || operator == Operator.IN;
    }

    public ConfluenceFieldSearchInputTransformer(
        final String urlParameterName,
        final ClauseNames clauseNames,
        final CustomField field,
        final JqlOperandResolver jqlOperandResolver,
        final JqlSelectOptionsUtil jqlSelectOptionsUtil,
        final QueryContextConverter queryContextConverter,
        final CustomFieldInputHelper customFieldInputHelper
    ) {
        super(urlParameterName, clauseNames, field, jqlOperandResolver, jqlSelectOptionsUtil, queryContextConverter, customFieldInputHelper);
        this.clauseNames = clauseNames;
    }
}