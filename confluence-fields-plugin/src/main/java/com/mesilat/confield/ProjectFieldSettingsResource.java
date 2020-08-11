package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import static com.mesilat.confield.DataServiceImpl.RE_FILTER_FIELD;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.java.ao.DBParam;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/project-settings")
@Scanned
public class ProjectFieldSettingsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @ComponentImport
    private final ActiveObjects ao;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/{project-key}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("project-key") String projectKey){
        ArrayNode arr = mapper.createArrayNode();
        FieldSettings[] fields = ao.find(FieldSettings.class);
        Arrays.asList(fields).forEach(field -> {
            ProjectFieldSettings[] pfs = ao.find(ProjectFieldSettings.class, "PROJECT_KEY = ? AND FIELD_ID = ?", projectKey, field.getId());
            if (pfs.length == 0) {
                ObjectNode obj = FieldSettings.toObjectNode(mapper, field);
                if (field.getFilter() != null) {
                    obj.put("filter", getFilterFields(field.getFilter()));
                }
                arr.add(obj);
            } else {
                ObjectNode obj = mapper.createObjectNode();
                obj.put("id", field.getId());
                obj.put("auto", pfs[0].isAutoFilter());
                obj.put("multi", pfs[0].isMultiSelect());
                if (pfs[0].getFilter() != null) {
                    obj.put("filter", getFilterFields(pfs[0].getFilter()));
                }
                arr.add(obj);
            }
        });
        return Response.ok(arr).build();
    }
    public ArrayNode getFilterFields(String filter) {
        ArrayNode filterFields = mapper.createArrayNode();
        Matcher m = RE_FILTER_FIELD.matcher(filter);
        while (m.find()){
            filterFields.add(m.group(1));
        }
        return filterFields;
    }

    @GET
    @Path("/{project-key}/{field-id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("project-key") String projectKey, @PathParam("field-id") Long fieldId){
        LOGGER.debug(String.format("Get project \"%s\" field settings by id: %d", projectKey, fieldId));
        ProjectFieldSettings[] pfs = ao.find(ProjectFieldSettings.class, "PROJECT_KEY = ? AND FIELD_ID = ?", projectKey, fieldId);
        if (pfs.length == 0) {
            FieldSettings fs = ao.get(FieldSettings.class, fieldId);
            if (fs == null){
                return Response.ok(mapper.createObjectNode()).build();
            } else {
                return Response.ok(fs).build();
            }
        } else {
            return Response.ok(pfs[0]).build();
        }
    }

    @PUT
    @Path("/{project-key}/{field-id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response put(@PathParam("project-key") String projectKey, @PathParam("field-id") Long fieldId, ObjectNode fieldSettings){
        try {
            LOGGER.debug(String.format("Post project \"%s\" field settings for %d: %s",
                projectKey,
                fieldId,
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fieldSettings))
            );
        } catch (IOException ignore) {
        }

        ProjectFieldSettings[] _fs = ao.find(ProjectFieldSettings.class, "PROJECT_KEY = ? AND FIELD_ID = ?", projectKey, fieldId);
        ProjectFieldSettings fs = _fs.length > 0? _fs[0]: ao.create(ProjectFieldSettings.class,
            new DBParam("PROJECT_KEY", projectKey),
            new DBParam("FIELD_ID", fieldId)
        );
        fs.setConfluenceId(fieldSettings.get("confluenceId").asText());
        fs.setFilter(fieldSettings.get("filter").asText());
        fs.setMultiSelect(fieldSettings.get("multiSelect").asBoolean());
        fs.setAutoFilter(fieldSettings.get("autoFilter").asBoolean());
        if (fieldSettings.has("definer") && fieldSettings.get("definer").isBoolean() && fieldSettings.get("definer").asBoolean()) {
            fs.setDefiner(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getUsername());
        } else {
            fs.setDefiner(null);
        }
        fs.save();
        dataService.reset();
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Inject
    public ProjectFieldSettingsResource(ActiveObjects ao, DataService dataService){
        this.ao = ao;
        this.dataService = dataService;
    }
}