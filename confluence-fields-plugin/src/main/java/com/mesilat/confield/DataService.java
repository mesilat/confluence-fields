package com.mesilat.confield;

import java.util.List;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public interface DataService {
    public static final int S_OK = 200;

    //ObjectNode getPageDetail(Long id);
    //ArrayNode getPageDetails(Long[] ids);
    ArrayNode getValues();
    ArrayNode getValues(Long fieldId);
    //boolean valueExists(Long fieldId, String text);
    Long getPageId(Long fieldId, String pageName);
    List<String> getMatchingPageNames(Long fieldId, String pattern, int maxNumResults);

    DataServiceResult test(String confluenceId, String filter, String q, int limit) throws DataServiceException;

    boolean isConfluenceField(long fieldId);
    boolean isConfluenceField(String fieldName);

    void reset();
    String getConfluenceBaseUrl(String projectKey, long fieldId);
    String getConfluenceBaseUrl(String projectKey, String fieldName);
    DataServiceResult getPage(String projectKey, long fieldId, String pageTitle) throws DataServiceException;
    DataServiceResult getPage(String projectKey, long fieldId, Long pageId, String pageTitle, ObjectNode filterFields) throws DataServiceException;
    DataServiceResult findPages(String projectKey, long fieldId, String q, int limit) throws DataServiceException;
    DataServiceResult findPages(String projectKey, long fieldId, String q, ObjectNode filterFields, int limit) throws DataServiceException;
    DataServiceResult getPagesById(String projectKey, long fieldId, List<String> pageId) throws DataServiceException;
    DataServiceResult getPagesByTitle(String projectKey, long fieldId, List<String> pageTitle) throws DataServiceException;
    List<String> getFilterFields(String projectKey, long fieldId);
    boolean isSelectMulti(String projectKey, long fieldId);
    boolean isAutoFilter(String projectKey, long fieldId);
    DataServiceResult resolve(String projectKey, long fieldId, String url) throws DataServiceException;
}