package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.java.ao.DBParam;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/settings")
@Scanned
public class FieldSettingsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @ComponentImport
    private final ActiveObjects ao;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @AnonymousAllowed
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response list(){
        LOGGER.debug("List confluence fields");
        FieldSettings[] fs = ao.find(FieldSettings.class);
        ObjectNode result = mapper.createObjectNode();
        Arrays.asList(fs).stream().forEach(s -> {
            ObjectNode o = FieldSettings.toObjectNode(mapper, s);
            //o.put("filter", toArrayNode(dataService.getFilterFields(s.getId())));
            result.put(Long.toString(s.getId()), o);
        });
        return Response.ok(result).build();
    }

    @GET
    @Path("/{field-id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("field-id") Long fieldId){
        LOGGER.debug(String.format("Get field %d settings", fieldId));
        FieldSettings fs = ao.get(FieldSettings.class, fieldId);
        if (fs == null){
            return Response.ok(mapper.createObjectNode()).build();
        } else {
            return Response.ok(fs).build();
        }
    }

    @PUT
    @Path("/{field-id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response put(@PathParam("field-id") Long fieldId, ObjectNode fieldSettings){
        try {
            LOGGER.debug(
                String.format("Put field %d settings: %s", fieldId,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fieldSettings)
                )
            );
        } catch (IOException ignore) {
        }

        FieldSettings fs = ao.get(FieldSettings.class, fieldId);
        if (fs == null){
            fs = ao.create(FieldSettings.class, new DBParam("ID", fieldId));
        }
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
    public FieldSettingsResource(ActiveObjects ao, DataService dataService){
        this.ao = ao;
        this.dataService = dataService;
    }

    private ArrayNode toArrayNode(List<String> filterFields) {
        if (filterFields == null) {
            return null;
        }
        ArrayNode arr = mapper.createArrayNode();
        filterFields.forEach(f -> arr.add(f));
        return arr;
    }
}