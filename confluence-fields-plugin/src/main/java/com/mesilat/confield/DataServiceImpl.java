package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import net.java.ao.Query;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExportAsService({DataService.class})
@Named
public class DataServiceImpl implements DataService {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");
    public static final Pattern RE_FILTER_FIELD = Pattern.compile("\\$\\{(.+?)\\}");
    private static final Pattern RE_CUSTOM_FIELD = Pattern.compile("^customfield_(\\d+)$");

    private final Map<String,String> confluenceBaseUrl = new HashMap<>();
    private final Map<Long,Boolean> confluenceFields = new HashMap<>();

    @ComponentImport
    private final ApplicationLinkService appLinkService;
    @ComponentImport
    private final ActiveObjects ao;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ArrayNode getValues() {
        ArrayNode arr = mapper.createArrayNode();
        Arrays.asList(ao.find(SupplementaryIndex.class)).stream().forEach(value -> {
            arr.add(toObjectNode(value));
        });
        return arr;
    }
    @Override
    public ArrayNode getValues(Long fieldId) {
        Map<Long,String> values = new HashMap<>();
        ArrayNode arr = mapper.createArrayNode();
        Arrays.asList(ao.find(SupplementaryIndex.class,
            Query
                .select()
                .distinct()
                .where("\"FIELD_ID\" = ?", fieldId)
        )).stream().forEach(value -> {
            values.put(value.getPageId(), value.getPageName() == null? "null": value.getPageName());
        });
        values
            .entrySet()
            .stream()
            .sorted((a,b) -> a.getValue().compareToIgnoreCase(b.getValue()))
            .forEach(e -> arr.add(toObjectNode(e.getKey(), e.getValue())));
        return arr;
    }
    // Used by ConfluenceFieldIndexValueConverter to obtain Confluence page id
    @Override
    public Long getPageId(Long fieldId, String pageName) {
        SupplementaryIndex[] results = ao.find(SupplementaryIndex.class, Query
            .select()
            .where("\"FIELD_ID\" = ? AND \"PAGE_NAME\" = ?", fieldId, pageName)
        );
        return results.length == 0? null: results[0].getPageId();
    }
    // Used by ConfluenceFieldClauseValuesGenerator for search options
    @Override
    public List<String> getMatchingPageNames(Long fieldId, String pattern, int maxNumResults) {
        SupplementaryIndex[] results = ao.find(SupplementaryIndex.class, Query
            .select()
            .distinct()
            .where("\"FIELD_ID\" = ? AND UPPER(\"PAGE_NAME\") LIKE ?", fieldId, String.format("%%%s%%", pattern.toUpperCase()))
            .order("\"PAGE_NAME\"")
            .limit(maxNumResults)
        );
        return Arrays.asList(results).stream().map(r -> r.getPageName()).collect(Collectors.toList());
    }

    @Override
    public DataServiceResult test(String confluenceId, String filter, String q, int limit) throws DataServiceException {
        LOGGER.debug(String.format("Test find pages: %s | %s | %s | %d", confluenceId, filter, q, limit));
        try {
            I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            ApplicationLink link = confluenceId == null
                    ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                    : appLinkService.getApplicationLink(new ApplicationId(confluenceId));
            
            if (link == null){
                throw new DataServiceException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    i18nHelper.getText("com.mesilat.confluence-field.err.linkNotFound", confluenceId == null? "default": confluenceId)
                );
            }

            ApplicationLinkRequestFactory reqFactory = link.createAuthenticatedRequestFactory();
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\"");
            if (q != null && !q.isEmpty()){
                cql.append(" and title~\"")
                    .append(q)
                    .append("\"");
            }
            if (filter != null && !filter.isEmpty()){
                cql.append(" and ").append(filter);
            }
            String url = String.format("/rest/api/content/search?cql=%s&limit=%d", URLEncoder.encode(cql.toString(), "UTF-8"), limit);
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        }
    }


    @Override
    public boolean isConfluenceField(long fieldId) {
        if (confluenceFields.containsKey(fieldId)){
            return confluenceFields.get(fieldId);
        }

        int c = ao.count(FieldSettings.class, "ID = ?", fieldId);
        confluenceFields.put(fieldId, c > 0);
        return c > 0;
    }
    @Override
    public boolean isConfluenceField(String fieldName) {
        if (fieldName == null){
            return false;
        }
        Matcher m = RE_CUSTOM_FIELD.matcher(fieldName);
        if (m.matches()) {
            return isConfluenceField(Long.parseLong(m.group(1)));
        } else {
            return false;
        }
    }
    @Override
    public String getConfluenceBaseUrl(String projectKey, long fieldId){
        String key = String.format("%s:%d", projectKey == null? "": projectKey, fieldId);
        if (!confluenceBaseUrl.containsKey(key)){
            try {
                CommonFieldSettings cfs = getSettings(projectKey, fieldId);
                ApplicationLink link = cfs == null || cfs.getConfluenceId() == null
                    ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                    : appLinkService.getApplicationLink(new ApplicationId(cfs.getConfluenceId()));
                confluenceBaseUrl.put(key, link.getDisplayUrl().toString());
            } catch (TypeNotInstalledException ex) {
                throw new RuntimeException(ex);
            }
        }
        return confluenceBaseUrl.get(key);
    }
    @Override
    public String getConfluenceBaseUrl(String projectKey, String fieldName) {
        if (fieldName == null){
            return null;
        }
        Matcher m = RE_CUSTOM_FIELD.matcher(fieldName);
        if (m.matches()) {
            return getConfluenceBaseUrl(projectKey, Long.parseLong(m.group(1)));
        } else {
            return null;
        }
    }
    @Override
    public void reset(){
        confluenceBaseUrl.clear();
        confluenceFields.clear();
    }
    @Override
    public DataServiceResult getPage(String projectKey, long fieldId, String pageTitle) throws DataServiceException {
        LOGGER.debug(String.format("Get page for project %s and field %d", projectKey, fieldId));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder cql = new StringBuilder();
            if (pageTitle.contains("\"")){
                cql.append("type=\"page\" and title='").append(pageTitle.replace("\"", "\\\\\"")).append("'");
            } else {
                cql.append("type=\"page\" and title=\"").append(pageTitle).append("\"");
            }
            if (cfs.getFilter() != null && !cfs.getFilter().isEmpty()){
                cql.append(" and ").append(cfs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s&limit=1", URLEncoder.encode(cql.toString(), "UTF-8"));
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }
    @Override
    public DataServiceResult getPage(String projectKey, long fieldId, Long pageId, String pageTitle, ObjectNode filterFields) throws DataServiceException {
        LOGGER.debug(String.format("Get page for project %s and field %d with filters", projectKey, fieldId));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\"");
            if (pageId != null){
                cql.append(" and id=")
                    .append(pageId);
            }
            if (pageTitle != null && !pageTitle.isEmpty()){
                cql.append(" and title=\"")
                    .append(pageTitle)
                    .append("\"");
            }
            if (cfs.getFilter() != null && !cfs.getFilter().isEmpty()){
                Matcher m = RE_FILTER_FIELD.matcher(cfs.getFilter());
                StringBuffer sb = new StringBuffer(cfs.getFilter().length());
                while (m.find()){
                    if (filterFields.has(m.group(1)) && !filterFields.get(m.group(1)).isNull()){
                        m.appendReplacement(sb, filterFields.get(m.group(1)).asText());
                    } else {
                        m.appendReplacement(sb, m.group(1));
                    }
                }
                m.appendTail(sb);
                cql.append(" and ").append(sb.toString());
            }
            LOGGER.debug(String.format("CQL=%s", cql.toString()));
            String url = String.format("/rest/api/content/search?cql=%s&limit=%d", URLEncoder.encode(cql.toString(), "UTF-8"), 999);
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    DataServiceResult result = execute(request);
                    LOGGER.debug(String.format("Result=%s", result.getText()));
                    return result;
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }
    @Override
    public DataServiceResult findPages(String projectKey, long fieldId, String q, int limit) throws DataServiceException {
        LOGGER.debug(String.format("Find pages for project %s and field %d", projectKey, fieldId));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\"");
            if (q != null && !q.isEmpty()){
                cql.append(" and title~\"")
                    .append(q)
                    .append("\"");
            }
            if (cfs != null && cfs.getFilter() != null && !cfs.getFilter().isEmpty()){
                cql.append(" and ").append(cfs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s&limit=%d", URLEncoder.encode(cql.toString(), "UTF-8"), limit);
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }
    @Override
    public DataServiceResult findPages(String projectKey, long fieldId, String q, ObjectNode filterFields, int limit) throws DataServiceException {
        LOGGER.debug(String.format("Find pages for project %s and field %d", projectKey, fieldId));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\"");
            if (q != null && !q.isEmpty()){
                cql.append(" and title~\"")
                    .append(q)
                    .append("\"");
            }
            if (cfs.getFilter() != null && !cfs.getFilter().isEmpty()){
                Matcher m = RE_FILTER_FIELD.matcher(cfs.getFilter());
                StringBuffer sb = new StringBuffer(cfs.getFilter().length());
                while (m.find()){
                    if (filterFields.has(m.group(1)) && !filterFields.get(m.group(1)).isNull()){
                        m.appendReplacement(sb, filterFields.get(m.group(1)).asText());
                    } else {
                        m.appendReplacement(sb, m.group(1));
                    }
                }
                m.appendTail(sb);
                cql.append(" and ").append(sb.toString());
            }
            LOGGER.debug(String.format("CQL=%s", cql.toString()));
            String url = String.format("/rest/api/content/search?cql=%s&limit=%d", URLEncoder.encode(cql.toString(), "UTF-8"), limit);
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    DataServiceResult result = execute(request);
                    LOGGER.debug(String.format("Result=%s", result.getText()));
                    return result;
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }
    @Override
    public DataServiceResult getPagesById(String projectKey, long fieldId, List<String> pageId) throws DataServiceException {
        LOGGER.debug(String.format("Get pages for project %s and field %d", projectKey, fieldId));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\" and id in (")
                .append(String.join(",", pageId))
                .append(")");
            //if (fs != null && fs.getFilter() != null && !fs.getFilter().isEmpty()){
            //    cql.append(" and ").append(fs.getFilter());
            //}
            String url = String.format("/rest/api/content/search?cql=%s", URLEncoder.encode(cql.toString(), "UTF-8"));
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }
    @Override
    public DataServiceResult getPagesByTitle(String projectKey, long fieldId, List<String> pageTitle) throws DataServiceException {
        LOGGER.debug(String.format("Get pages for project %s and field %d", projectKey, fieldId));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder cql = new StringBuilder();
            cql.append("type=\"page\" and title in (")
               .append(
                    String.join(",", pageTitle
                        .stream()
                        .map(title -> {
                            if (title.contains("\"")){
                                return String.format("'%s'", title.replace("\"", "\\\\\""));
                            } else {
                                return String.format("\"%s\"", title);
                            }
                        })
                        .collect(Collectors.toList())
                    )
               )
               .append(")");

            if (cfs.getFilter() != null && !cfs.getFilter().isEmpty()){
                cql.append(" and ").append(cfs.getFilter());
            }
            String url = String.format("/rest/api/content/search?cql=%s", URLEncoder.encode(cql.toString(), "UTF-8"));
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, url);
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }
    @Override
    public List<String> getFilterFields(String projectKey, long fieldId) {
        List<String> filterFields = new ArrayList<>();
        CommonFieldSettings cfs = getSettings(projectKey, fieldId);
        if (cfs != null && cfs.getFilter() != null){
            Matcher m = RE_FILTER_FIELD.matcher(cfs.getFilter());
            while (m.find()){
                filterFields.add(m.group(1));
            }
        }
        return filterFields;
    }
    @Override
    public boolean isSelectMulti(String projectKey, long fieldId) {
        CommonFieldSettings cfs = getSettings(projectKey, fieldId);
        return cfs == null? true: cfs.isMultiSelect();
    }
    @Override
    public boolean isAutoFilter(String projectKey, long fieldId) {
        CommonFieldSettings cfs = getSettings(projectKey, fieldId);
        return cfs == null? true: cfs.isAutoFilter();
    }
    @Override
    public DataServiceResult resolve(String projectKey, long fieldId, String url) throws DataServiceException {
        LOGGER.debug(String.format("Resolve page for project %s and field %d and url %s", projectKey, fieldId, url));
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

        try {
            CommonFieldSettings cfs = getSettings(projectKey, fieldId);
            if (cfs.getDefiner() != null) {
                ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(
                    ComponentAccessor.getUserManager().getUserByName(cfs.getDefiner())
                );
            }
            ApplicationLink appLink = getApplicationtLink(cfs);
            URI uri = new URI(url);
            if (appLink.getDisplayUrl().getPort() != uri.getPort()
                || !appLink.getDisplayUrl().getHost().equals(uri.getHost())
                || !appLink.getDisplayUrl().getScheme().equals(uri.getScheme())
            ) {
                throw new DataServiceException(Response.Status.BAD_REQUEST, "Url mismatch - cannot resolve");
            }
            if (appLink.getDisplayUrl().getPath() != null && !uri.getPath().startsWith(appLink.getDisplayUrl().getPath())) {
                throw new DataServiceException(Response.Status.BAD_REQUEST, "Url mismatch - cannot resolve");
            }
            
            ApplicationLinkRequestFactory reqFactory = createFactory(cfs);
            StringBuilder address = new StringBuilder();
            if (appLink.getDisplayUrl().getPath() == null) {
                address.append(uri.getPath());
            } else {
                address.append(uri.getPath().substring(appLink.getDisplayUrl().getPath().length()));
            }
            if (uri.getQuery() != null) {
                address.append("?").append(URLEncoder.encode(uri.getQuery(), "UTF-8"));
            }
            while (true){
                try {
                    ApplicationLinkRequest request = reqFactory.createRequest(Request.MethodType.GET, address.toString());
                    return execute(request);
                } catch(Throwable ex){
                    throwIfNotNonceUsed(ex);
                }
            }
        } catch(CredentialsRequiredException ex) {
            throw new DataServiceException(Response.Status.UNAUTHORIZED, ex.getMessage(), ex, "authorize jira");
        } catch (Throwable ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
        } finally {
            ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
        }
    }

    private DataServiceResult execute(ApplicationLinkRequest request) throws ResponseException {
        return request.execute(new ApplicationLinkResponseHandler<DataServiceResult>() { 
            @Override 
            public DataServiceResult credentialsRequired(com.atlassian.sal.api.net.Response response) throws ResponseException {
                LOGGER.debug(String.format("Response from remote server: code=%d", response.getStatusCode()));
                return new DataServiceResult(response.getStatusCode(), response.getResponseBodyAsString());
            } 
            @Override 
            public DataServiceResult handle(com.atlassian.sal.api.net.Response response) throws ResponseException {
                LOGGER.debug(String.format("Response from remote server: code=%d", response.getStatusCode()));
                return new DataServiceResult(response.getStatusCode(), response.getResponseBodyAsString());
            } 
        });
    }
    private ApplicationLink getApplicationtLink(CommonFieldSettings cfs) throws DataServiceException {
        try {
            I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            ApplicationLink link = cfs == null || cfs.getConfluenceId() == null
                ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                : appLinkService.getApplicationLink(new ApplicationId(cfs.getConfluenceId()));

            if (link == null){
                throw new DataServiceException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    i18nHelper.getText("com.mesilat.confluence-field.err.linkNotFound", cfs == null? "default": cfs.getConfluenceId())
                );
            } else {
                return link;
            }
        } catch (TypeNotInstalledException ex) {
            throw new DataServiceException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }
    private ApplicationLinkRequestFactory createFactory(CommonFieldSettings cfs) throws DataServiceException {
        return getApplicationtLink(cfs).createAuthenticatedRequestFactory();
    }
    private void throwIfNotNonceUsed(Throwable ex) throws Throwable {
        try {
            Method m = ex.getClass().getMethod("getOAuthProblem");
            String oauthProblem = (String)m.invoke(ex);
            if (!"nonce_used".equals(oauthProblem)){
                throw ex;
            }
        } catch(Throwable ignore){
            throw ex;
        }
    }
    private ObjectNode toObjectNode(SupplementaryIndex value){
        ObjectNode obj = mapper.createObjectNode();
        obj.put("id", value.getID());
        obj.put("issueId", value.getIssueId());
        obj.put("fieldId", value.getFieldId());
        obj.put("page", toObjectNode(value.getPageId(), value.getPageName()));
        return obj;
    }
    private ObjectNode toObjectNode(Long id, String title){
        ObjectNode obj = mapper.createObjectNode();
        obj.put("id", id);
        obj.put("title", title);
        return obj;
    }
    private CommonFieldSettings getSettings(String projectKey, long fieldId) {
        CommonFieldSettings cfs = ProjectFieldSettings.getSettings(ao, projectKey, fieldId);
        return cfs == null? FieldSettings.getSettings(ao, fieldId): cfs;
    }

    @Inject
    public DataServiceImpl(ApplicationLinkService appLinkService, ActiveObjects ao){
        this.appLinkService = appLinkService;
        this.ao = ao;
    }
}