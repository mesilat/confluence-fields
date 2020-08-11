package com.mesilat.confield;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;

@Preload
public interface SupplementaryIndex extends Entity {
    @Indexed
    Long getIssueId();
    void setIssueId(Long issueId);
    @Indexed
    Long getFieldId();
    void setFieldId(Long fieldId);
    @Indexed
    Long getPageId();
    void setPageId(Long pageId);
    @Indexed
    String getPageName();
    void setPageName(String pageName);
}