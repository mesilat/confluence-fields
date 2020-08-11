package com.mesilat.jira;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mesilat.confield.DataService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

@Path("/changehistory")
@Scanned
public class ChangeHistoryResource {
    //private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @ComponentImport
    private final ChangeHistoryManager changeHistoryManager;
    @ComponentImport
    private final CustomFieldManager customFieldManager;
    private final DataService dataService;
    
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("id") Long id){
        Job job = new Job();
        ObjectNode response = job.process(id);
        return Response.ok(response).build();
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response post(ArrayNode arr){
        List<Long> changeIds = new ArrayList<>();
        arr.forEach(changeId -> changeIds.add(changeId.asLong()));
        Job job = new Job();
        ObjectNode response = job.process(changeIds);
        return Response.ok(response).build();
    }

    @Inject
    public ChangeHistoryResource(ChangeHistoryManager changeHistoryManager, CustomFieldManager customFieldManager, DataService dataService){
        this.changeHistoryManager = changeHistoryManager;
        this.customFieldManager = customFieldManager;
        this.dataService = dataService;
    }

    private static class CustomFieldInfo {
        private String name;
        private Long id;
        private boolean multi;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public boolean isMulti() {
            return multi;
        }
        public void setMulti(boolean multi) {
            this.multi = multi;
        }
    }
    private class Job {
        private Map<String, CustomFieldInfo> fields;
        private final Map<String, String> urls = new HashMap<>();

        private CustomFieldInfo getCustomFieldInfo(Issue issue, String name){
            if (fields == null){
                fields = new HashMap<>();
                
                List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
                String projectKey = issue == null? null: issue.getProjectObject().getKey();
                for (int i = 0; i < customFields.size(); i++){
                    if (dataService.isConfluenceField(customFields.get(i).getIdAsLong())){
                        CustomFieldInfo cfi = new CustomFieldInfo();
                        cfi.setId(customFields.get(i).getIdAsLong());
                        cfi.setName(customFields.get(i).getFieldName());
                        cfi.setMulti(dataService.isSelectMulti(projectKey, cfi.getId()));
                        fields.put(customFields.get(i).getFieldName(), cfi);
                    }
                }
            }

            return fields.get(name);
        }
        private List<String> getValues(String text, boolean multi){
            if (text == null || text.isEmpty()){
                return Arrays.asList();
            } else if (multi){
                List<String> result = new ArrayList<>();
                for (String value : text.split(",")){
                    result.add(value.trim());
                }
                return result;
            } else {
                return Arrays.asList(text.trim());
            }
        }
        private ObjectNode getResponse(AtomicReference<ObjectNode> response){
            if (response.get() == null){
                response.set(mapper.createObjectNode());
            }
            return response.get();
        }
        private void putUrlToResponse(Issue issue, String fieldName, String text, AtomicReference<ObjectNode> response){
            if (text == null || text.isEmpty()){
                return;
            }
            CustomFieldInfo cfi = getCustomFieldInfo(issue, fieldName);
            if (cfi == null){
                return;
            }
            String projectKey = issue == null? null: issue.getProjectObject().getKey();
            getValues(text, cfi.isMulti()).forEach(value -> {
                String key = String.format("%s: %s", fieldName, value);
                if (urls.containsKey(key)){
                    getResponse(response).put(value, urls.get(key));
                } else {
                    Long oldPageId = dataService.getPageId(cfi.getId(), value);
                    if (oldPageId != null){
                        String url = String.format("%s/pages/viewpage.action?pageId=%d",
                            dataService.getConfluenceBaseUrl(projectKey, cfi.getId()),
                            oldPageId
                        );
                        urls.put(key, url);
                        getResponse(response).put(value, url);
                    }
                }
            });
        }
        public ObjectNode process(Long changeId){
            ChangeHistory change = changeHistoryManager.getChangeHistoryById(changeId);
            if (change == null){
                return null;
            }

            AtomicReference<ObjectNode> response = new AtomicReference();
            change.getChangeItems().forEach(_item -> {
                if ("custom".equals(_item.get("fieldtype"))){
                    String fieldName = _item.getString("field");
                    putUrlToResponse(change.getIssue(), fieldName, (String)_item.get("oldstring"), response);
                    putUrlToResponse(change.getIssue(), fieldName, (String)_item.get("newstring"), response);
                }
            });

            return response.get();
        }
        public ObjectNode process(List<Long> changeIds){
            ObjectNode result = mapper.createObjectNode();
            changeIds.forEach(changeId -> {
                ObjectNode change = process(changeId);
                if (change != null){
                    result.put(changeId.toString(), change);
                }
            });
            return result;
        }
    }
}