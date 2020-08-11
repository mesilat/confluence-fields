package com.mesilat.confield;

import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.customfields.view.NullCustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final Issue issue;
    private final CustomField field;
    private final FieldLayoutItem fieldLayoutItem;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    public String getFormValues(CustomFieldParams customFieldParams) {
        if (customFieldParams instanceof NullCustomFieldParams){
            return "[]";
        } else {
            return String.format("[%s]", String.join(",", customFieldParams.getValuesForKey(null)));
        }
    }
    public List<Map<String,Object>> getDetailsForValues(String value) throws TypeNotInstalledException {
        String projectKey = issue == null? null: issue.getProjectObject().getKey();

        List<Map<String,Object>> results = new ArrayList<>();
        if (value == null){
            return results;
        } else {
            value = String.format("[%s]", value);
        }

        try {
            ArrayNode arr = (ArrayNode)mapper.readTree(value);
            arr.forEach(n -> {
                ObjectNode obj = (ObjectNode)n;
                Map<String,Object> page = new HashMap<>();
                page.put("id", obj.get("id").asLong());
                page.put("url", String.format(
                    "%s/pages/viewpage.action?pageId=%s",
                    dataService.getConfluenceBaseUrl(projectKey, field.getIdAsLong()),
                    obj.get("id").asText())
                );
                page.put("title", obj.get("title").asText());
                results.add(page);
            });
        } catch (IOException ex) {
            LOGGER.warn(String.format("Failed to parse JSON: %s", value), ex);
        }

        return results;
    }
    public String getTextForValue(Object value) {
        List<String> list = new ArrayList<>();
        if (value == null){
            return "";
        } else {
            try {
                ArrayNode arr = (ArrayNode)mapper.readTree(String.format("[%s]", value));
                arr.forEach(n -> {
                    ObjectNode obj = (ObjectNode)n;
                    list.add(obj.get("title").asText());
                });
            } catch (IOException ex) {
                return value.toString();
            }
            return String.join(", ", list);
        }
    }

    public String getParamsForFilter(){
        String projectKey = issue == null? null: issue.getProjectObject().getKey();
        return String.join(",", dataService.getFilterFields(projectKey, field.getIdAsLong()));
    }
    public boolean isSelectMulti(){
        String projectKey = issue == null? null: issue.getProjectObject().getKey();
        return dataService.isSelectMulti(projectKey, field.getIdAsLong());
    }
    public boolean isAutoFilter(){
        String projectKey = issue == null? null: issue.getProjectObject().getKey();
        return dataService.isAutoFilter(projectKey, field.getIdAsLong());
    }

    public TemplateHelper(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem, DataService dataService) {
        this.issue = issue;
        this.field = field;
        this.fieldLayoutItem = fieldLayoutItem;
        this.dataService = dataService;
    }
}