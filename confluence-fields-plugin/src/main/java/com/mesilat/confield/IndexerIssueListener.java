package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndexerIssueListener implements InitializingBean, DisposableBean {
    public static final String COMMA_REPLACEMENT_PATTERN = "~[$]~";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("ru.dp.erp-plugin");

    public static final String CONFLUENCE_FIELD_KEY   = "com.mesilat.confluence-fields:confluence-field";

    private final EventPublisher eventPublisher;
    private final CustomFieldManager customFieldManager;
    private final ActiveObjects ao;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }
    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        Long eventTypeId = issueEvent.getEventTypeId();

        if (eventTypeId.equals(EventType.ISSUE_CREATED_ID)) {
            Thread t = new Thread(()->{
                onIssueCreated(issueEvent.getIssue());
            });
            t.start();
        } else if (eventTypeId.equals(EventType.ISSUE_UPDATED_ID)) {
            Thread t = new Thread(()->{
                onIssueUpdated(issueEvent.getIssue());
            });
            t.start();
        } else if (eventTypeId.equals(EventType.ISSUE_DELETED_ID)) {
            Thread t = new Thread(()->{
                onIssueDeleted(issueEvent.getIssue());
            });
            t.start();
        }
    }

    private void onIssueCreated(Issue issue) {
        ao.executeInTransaction(() -> {
            getConfluenceFields(issue).forEach(field -> {
                Map<Long,String> values = getCustomFieldValues(issue, field);
                save(issue.getId(), field.getIdAsLong(), values);
            });
            return null;
        });
    }
    private void onIssueUpdated(Issue issue) {
        ao.executeInTransaction(() -> {
            getConfluenceFields(issue).forEach(field -> {
                Map<Long,String> values = getCustomFieldValues(issue, field);
                save(issue.getId(), field.getIdAsLong(), values);
            });
            return null;
        });
    }
    private void onIssueDeleted(Issue issue) {
        ao.executeInTransaction(() -> {
            ao.deleteWithSQL(SupplementaryIndex.class, "ISSUE_ID = ?", issue.getId());
            return null;
        });
    }

    private List<CustomField> getConfluenceFields(Issue issue){
        List<CustomField> confuenceFields = new ArrayList<>();
        customFieldManager.getCustomFieldObjects(issue).forEach(field -> {
            if (CONFLUENCE_FIELD_KEY.equals(field.getCustomFieldType().getKey())){
                confuenceFields.add(field);
            }
        });
        return confuenceFields;
    }
    private Map<Long,String> getCustomFieldValues(Issue issue, CustomField cf){
        Map<Long,String> values = new HashMap<>();
        String rawValue = (String)issue.getCustomFieldValue(cf);
        if (rawValue != null && !rawValue.isEmpty()){
            rawValue = rawValue.replace(COMMA_REPLACEMENT_PATTERN, ",");
            try {
                ArrayNode arr = (ArrayNode)mapper.readTree(String.format("[%s]", rawValue));
                arr.forEach(v -> {
                    values.put(Long.parseLong(v.get("id").asText()), v.get("title").asText());
                });
            } catch (IOException ex) {
                LOGGER.warn(String.format("Failed to parse: %s", rawValue), ex);
            }
        }
        return values;
    }    
    private void save(Long issueId, Long fieldId, Map<Long,String> values){
        Map<Long,SupplementaryIndex> oldValues = new HashMap<>();
        Arrays.asList(ao.find(SupplementaryIndex.class, "ISSUE_ID = ? and FIELD_ID = ?", issueId, fieldId)).forEach(rec -> {
            oldValues.put(rec.getPageId(), rec);
        });

        values.forEach((id, title) -> {
            if (oldValues.containsKey(id)){
                SupplementaryIndex index = oldValues.get(id);
                if (!equals(title, index.getPageName())){
                    index.setPageName(title);
                    index.save();
                }
                oldValues.remove(id);
            } else {
                SupplementaryIndex index = ao.create(SupplementaryIndex.class);
                index.setIssueId(issueId);
                index.setFieldId(fieldId);
                index.setPageId(id);
                index.setPageName(title);
                index.save();
            }
        });

        //oldValues.values().forEach(rec -> {
        //    ao.delete(rec);
        //});
    }
    private static boolean equals(String a, String b){
        return a == null && b == null
            || a != null && a.equals(b)
            || b != null && b.equals(a);
    }

    @Autowired
    public IndexerIssueListener(
        @JiraImport EventPublisher eventPublisher,
        @JiraImport CustomFieldManager customFieldManager,
        @JiraImport ActiveObjects ao
    ) {
        this.eventPublisher = eventPublisher;
        this.customFieldManager = customFieldManager;
        this.ao = ao;
    }
}