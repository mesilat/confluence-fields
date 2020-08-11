package com.mesilat.confield;

import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;

public class ConfluenceFieldValidator implements ClauseValidator {
    private final SupportedOperatorsValidator supportedOperatorsValidator;

    @Override
    public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause) {
        return supportedOperatorsValidator.validate(searcher, terminalClause);
    }

    public ConfluenceFieldValidator(){
        this.supportedOperatorsValidator = new SupportedOperatorsValidator(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY);
    }
}