package com.mesilat.confield;

import com.atlassian.query.operand.SingleValueOperand;

/**
 * A result object for the {@link com.atlassian.jira.issue.customfields.searchers.transformer.AbstractSingleValueCustomFieldSearchInputTransformer#convertForNavigator} method.
 */
public class NavigatorConversionResult {
    private final boolean fitsNavigator;
    private final SingleValueOperand value;

    public NavigatorConversionResult(final boolean fitsNavigator, final SingleValueOperand value) {
        this.fitsNavigator = fitsNavigator;
        this.value = value;
    }

    /**
     * @return true if the {@link com.atlassian.jira.issue.search.SearchRequest} fits the navigator, false otherwise.
     */
    public boolean fitsNavigator() {
        return fitsNavigator;
    }

    /**
     * @return the single value for the customfield in the {@link com.atlassian.jira.issue.search.SearchRequest}, or null
     * if there was no value, or null if the SearchRequest did not fit in the navigator.
     */
    public SingleValueOperand getValue() {
        return value;
    }
}