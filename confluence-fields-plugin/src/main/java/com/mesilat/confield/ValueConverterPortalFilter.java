package com.mesilat.confield;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
POST http://127.0.0.1/servicedesk/servicedesk/customer/portal/1/create/1
GET http://127.0.0.1/servicedesk/servicedesk/customer/portal/1/TP-14
*/
public class ValueConverterPortalFilter implements Filter{
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");
    private static final Pattern PATTERN_CV_JSON_FRAGMENT = Pattern.compile("<div id=\"jsonPayload\" class=\"cv-json-fragment\">([^<]*)</div>");

    private final DataService dataService;

    @Override
    public void init(FilterConfig filterConfig)throws ServletException{
    }
    @Override
    public void destroy(){
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)throws IOException,ServletException{
        HttpServletRequest req = (HttpServletRequest)request;
        if ("POST".equals(req.getMethod()) && req.getServletPath().matches("/servicedesk/customer/portal/\\d+/create/\\d+")){
            if (request.getContentType().startsWith("multipart/form-data")){
                FormRequestWrapper wrapper = new FormRequestWrapper((HttpServletRequest)request, dataService);
                chain.doFilter(wrapper, response);
            } else {
                ParamRequestWrapper requestWrapper = new ParamRequestWrapper((HttpServletRequest)request, dataService);
                //chain.doFilter(requestWrapper, response);

                GenericResponseWrapper responseWrapper = new GenericResponseWrapper((HttpServletResponse)response);
                chain.doFilter(requestWrapper, responseWrapper);

                String json = responseWrapper.getCaptureAsString();
                json = convertJson(json);
                response.getWriter().write(json);
            }
        } else if ("POST".equals(req.getMethod()) && req.getServletPath().matches("/rest/servicedesk/\\d+/customer/models")) {
            GenericResponseWrapper responseWrapper = new GenericResponseWrapper((HttpServletResponse)response);
            chain.doFilter(request, responseWrapper);

            String json = responseWrapper.getCaptureAsString();
            json = convertJson(json);
            response.getWriter().write(json);
        } else if ("GET".equals(req.getMethod()) && req.getServletPath().matches("/servicedesk/customer/portal/\\d+/[A-Z]+\\-\\d+")) {
            HttpServletResponse res = (HttpServletResponse) response;
            GenericResponseWrapper responseWrapper = new GenericResponseWrapper(res);
            chain.doFilter(request, responseWrapper);

            String payload = responseWrapper.getCaptureAsString();
            payload = convertFragment(payload);
            response.getWriter().write(payload);
        } else {
            chain.doFilter(request, response);
        }
    }

    void convertIssueObject(ObjectMapper mapper, ObjectNode issue) throws IOException {
        ArrayNode fields = (ArrayNode)issue.get("fields");
        fields.forEach(f -> {
            try {
                ObjectNode field = (ObjectNode)f;
                String fieldId = field.get("id").asText();
                if (
                       dataService.isConfluenceField(fieldId)
                    && field.has("value")
                    && field.get("value").isObject()
                    && field.get("value").has("html")
                    && field.get("value").get("html").isTextual()
                ) {
                    String baseUrl = dataService.getConfluenceBaseUrl(null/* TODO: projectKey from issue */, fieldId);
                    String html = StringEscapeUtils.unescapeHtml4(field.get("value").get("html").asText());
                    ArrayNode fieldValues = (ArrayNode)mapper.readTree(String.format("[%s]", html));
                    List<String> aa = new ArrayList<>();
                    fieldValues.forEach(n -> {
                        ObjectNode obj = (ObjectNode)n;
                        String a = String.format("<a href='%s/pages/viewpage.action?pageId=%s' target='_blank'>%s</a>",
                            baseUrl,
                            obj.get("id").asText(),
                            StringEscapeUtils.escapeHtml4(obj.get("title").asText())
                        );
                        aa.add(a);
                    });
                    html = String.join(", ", aa);
                    ((ObjectNode)field.get("value")).put("html", html);
                }
            } catch (Throwable e) {
                LOGGER.warn("Failed to convert value to <A>", e);
            }
        });
    }
    String convertJson(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(json);
        if (
                rootNode.has("reqDetails")
             && rootNode.get("reqDetails").isObject()
             && rootNode.get("reqDetails").has("issue")
             && rootNode.get("reqDetails").get("issue").isObject()
             && rootNode.get("reqDetails").get("issue").has("fields")
             && rootNode.get("reqDetails").get("issue").get("fields").isArray()
        ) {
            ObjectNode issue = (ObjectNode)rootNode.get("reqDetails").get("issue");
            convertIssueObject(mapper, issue);
            return mapper.writeValueAsString(rootNode);
        } else if (
                rootNode.has("issue")
             && rootNode.get("issue").isObject()
             && rootNode.get("issue").has("fields")
             && rootNode.get("issue").get("fields").isArray()
        ) {
            ObjectNode issue = (ObjectNode)rootNode.get("issue");
            convertIssueObject(mapper, issue);
            return mapper.writeValueAsString(rootNode);
        } else {
            return json;
        }
    }
    String convertFragment(String payload) throws IOException {
        Matcher m = PATTERN_CV_JSON_FRAGMENT.matcher(payload);
        if (m.find()) {
            String json = StringEscapeUtils.unescapeHtml4(m.group(1));
            json = StringEscapeUtils.escapeHtml4(convertJson(json));
            return m.replaceFirst(String.format("<div id=\"jsonPayload\" class=\"cv-json-fragment\">%s</div>", Matcher.quoteReplacement(json)));
        } else {
            return payload; // Not converted
        }
    }
    
    @Inject
    public ValueConverterPortalFilter(DataService dataService){
        this.dataService = dataService;
    }
}