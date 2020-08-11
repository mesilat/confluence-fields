package com.mesilat.confield;

import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator.Result;
import com.atlassian.jira.jql.values.ClauseValuesGenerator.Results;
import com.atlassian.jira.user.ApplicationUser;
import java.util.stream.Collectors;

public class ConfluenceFieldClauseValuesGenerator implements ClauseValuesGenerator {
    private final long fieldId;
    private final DataService dataService;

    /**
     * List possible values (page names) for a field
     * @param user Current user
     * @param jqlClauseName 
     * @param valuePrefix Text to search
     * @param maxNumResults Max results to fetch
     * @return Page names
     */
    @Override
    public Results getPossibleValues(final ApplicationUser user, final String jqlClauseName, final String valuePrefix, final int maxNumResults) {
        return new Results(
            dataService
                .getMatchingPageNames(fieldId, valuePrefix, maxNumResults)
                .stream()
                .map(pageName -> new Result(pageName, pageName))
                .collect(Collectors.toList())
        );
    }

    public ConfluenceFieldClauseValuesGenerator(long fieldId, DataService dataService) {
        this.fieldId = fieldId;
        this.dataService = dataService;
    }
}