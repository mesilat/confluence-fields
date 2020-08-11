package com.mesilat.confield;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/field")
@Scanned
public class FieldResource {
    public static final int DEFAULT_LIMIT = 25;
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @ComponentImport
    private final CustomFieldManager fieldManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DataService dataService;

    @GET
    @Path("/{field-id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("field-id") Long fieldId){
        LOGGER.debug(String.format("Get field %d detail", fieldId));

        CustomField cf = fieldManager.getCustomFieldObject(fieldId);
        if (cf == null){
            I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(i18n.getText("com.mesilat.confluence-field.err.fieldNotFound", Long.toString(fieldId)))
                .build();
        }

        ObjectNode field = mapper.createObjectNode();
        field.put("id", cf.getIdAsLong());
        field.put("name", cf.getFieldName());
        field.put("desc", cf.getDescription());

        ObjectNode type = mapper.createObjectNode();
        type.put("key", cf.getCustomFieldType().getKey());
        type.put("name", cf.getCustomFieldType().getName());
        field.put("type", type);

        return Response.ok(field).build();
    }

    @GET
    @Path("/{project-key}/{field-id}/pages")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findPages(
        @PathParam("project-key") String projectKey,
        @PathParam("field-id") Long fieldId,
        @QueryParam("q") String q,
        @QueryParam("filter-fields") String filterFields,
        @QueryParam("max-results") Integer limit,
        @Context HttpServletResponse response
    ){
        try {
            if (filterFields != null && !filterFields.isEmpty()){
                ObjectNode filterObject = (ObjectNode)mapper.readTree(filterFields);
                DataServiceResult result = dataService.findPages(projectKey, fieldId, q, filterObject, limit == null? DEFAULT_LIMIT: limit);
                return Response.status(result.getStatus()).entity(result.getText()).build();
            } else {
                DataServiceResult result = dataService.findPages(projectKey, fieldId, q, limit == null? DEFAULT_LIMIT: limit);
                return Response.status(result.getStatus()).entity(result.getText()).build();
            }
        } catch (DataServiceException ex) {
            LOGGER.debug(String.format("Error looking up pages for project %s and field %d", projectKey, fieldId), ex);
            if (ex.getHeader() != null) {
                response.setHeader("X-CONFFIELDS-ADVICE", ex.getHeader());
            }
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        } catch (IOException ex) {
            LOGGER.debug(String.format("Error looking up pages for project %s and field %d", projectKey, fieldId), ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/{project-key}/{field-id}/validate")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response validatePages(
        @PathParam("project-key") String projectKey,
        @PathParam("field-id") Long fieldId,
        ObjectNode query
    ){
        ObjectNode filterFields = query.has("filter-fields")? (ObjectNode)query.get("filter-fields"): null;
        ArrayNode arr = (ArrayNode)query.get("pages"),
            result = mapper.createArrayNode();
        arr.forEach(o -> {
            ObjectNode page = (ObjectNode)o;
            try {
                DataServiceResult res = dataService.getPage(projectKey, fieldId, page.get("id").asLong(), page.get("title").asText(), filterFields);
                if (res.getStatus() == Response.Status.OK.getStatusCode()){
                    ObjectNode resp = (ObjectNode)mapper.readTree(res.getText());
                    if (resp.has("results") && resp.get("results").isArray() && resp.get("results").size() > 0){
                        result.add(page);
                    }
                }
            } catch (DataServiceException | IOException ex) {
                LOGGER.debug(String.format("Validation failed for page %d (%s)", page.get("id").asLong(), page.get("title").asText()), ex);
            }
        });
        
        return Response.ok(result).build();
    }

    @GET
    @Path("/{project-key}/{field-id}/pages-by-id")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getPagesById(
        @PathParam("project-key") String projectKey,
        @PathParam("field-id") Long fieldId,
        @QueryParam("page-id") List<String> pageId,
        @Context HttpServletResponse response
    ){
        try {
            DataServiceResult result = dataService.getPagesById(projectKey, fieldId, pageId);
            return Response.status(result.getStatus()).entity(result.getText()).build();
        } catch (DataServiceException ex) {
            LOGGER.debug(String.format("Error getting pages for project %s and field %d", projectKey, fieldId), ex);
            if (ex.getHeader() != null) {
                response.setHeader("X-CONFFIELDS-ADVICE", ex.getHeader());
            }
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response test(
        @QueryParam("confluence-id") String confluenceId,
        @QueryParam("filter") String filter,
        @QueryParam("q") String q,
        @QueryParam("max-results") Integer limit,
        @Context HttpServletResponse response
    ){
        try {
            DataServiceResult result = dataService.test(confluenceId, filter, q, limit == null? DEFAULT_LIMIT: limit);
            return Response.status(result.getStatus()).entity(result.getText()).build();
        } catch (DataServiceException ex) {
            if (ex.getHeader() != null) {
                response.setHeader("X-CONFFIELDS-ADVICE", ex.getHeader());
            }
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/values")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response values(){
        return Response.ok(dataService.getValues()).build();
    }

    @GET
    @Path("/{field-id}/values")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response values(@PathParam("field-id") Long fieldId){
        return Response.ok(dataService.getValues(fieldId)).build();
    }

    @GET
    @Path("/{project-key}/{field-id}/filter-fields")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response filterFields(
        @PathParam("project-key") String projectKey,
        @PathParam("field-id") Long fieldId
    ){
        ArrayNode arr = mapper.createArrayNode();
        dataService.getFilterFields(projectKey, fieldId).forEach(s -> {
            arr.add(s);
        });
        return Response.ok(arr).build();
    }

    @GET
    @Path("/confluence-links")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response confluenceLinks(){
        ApplicationLinkService appLinkService = ComponentAccessor.getComponent(ApplicationLinkService.class);
        ArrayNode arr = mapper.createArrayNode();
        for (ApplicationLink appLink: appLinkService.getApplicationLinks(ConfluenceApplicationType.class)) {
            ObjectNode obj = mapper.createObjectNode();
            obj.put("id", appLink.getId().get());
            obj.put("name", appLink.getName());
            obj.put("isPrimary", appLink.isPrimary());
            // TODO: obtain actual Confluence instance icon
            obj.put("iconUrl", String.format("%s/%s",
                ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL),
                "download/resources/com.mesilat.confluence-fields/images/16confluence.png"
            ));
            arr.add(obj);
        }
        return Response.ok(arr).build();
    }

    @GET
    @Path("/{field-id}/resolve")
    public Response resolve(
        @PathParam("project-key") String projectKey,
        @PathParam("field-id") Long fieldId,
        @QueryParam("url") String url,
        @Context HttpServletResponse response
    ){
        try {
            DataServiceResult result = dataService.resolve(projectKey, fieldId, url);
            return Response.status(result.getStatus()).entity(result.getText()).build();
        } catch (DataServiceException ex) {
            if (ex.getHeader() != null) {
                response.setHeader("X-CONFFIELDS-ADVICE", ex.getHeader());
            }
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        }
    }
    
    @Inject
    public FieldResource(CustomFieldManager fieldManager, DataService dataService){
        this.fieldManager = fieldManager;
        this.dataService = dataService;
    }
}