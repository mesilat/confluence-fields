package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Default;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Preload
@JsonIgnoreProperties({"entityManager", "entityProxy", "entityType"})
@JsonAutoDetect
@Table("PrjFieldSettings")
public interface ProjectFieldSettings extends Entity, CommonFieldSettings {
    @NotNull
    Long getFieldId();
    void setFieldId(Long fieldId);

    @StringLength(30)
    String getProjectKey();
    void setProjectKey(String projectKey);

    @StringLength(36)
    @Override
    String getConfluenceId();
    @Override
    void setConfluenceId(String confluenceId);

    @StringLength(StringLength.UNLIMITED)
    @Override
    String getFilter();
    @Override
    void setFilter(String filter);

    @Default("true")
    @Override
    boolean isMultiSelect();
    @Override
    void setMultiSelect(boolean multiSelect);

    @Default("false")
    @Override
    boolean isAutoFilter();
    @Override
    void setAutoFilter(boolean autoFilter);

    @StringLength(30)
    @Override
    String getDefiner();
    @Override
    void setDefiner(String definer);

    public static ObjectNode toObjectNode(ObjectMapper mapper, ProjectFieldSettings settings) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("id", settings.getID());
        obj.put("field-id", settings.getFieldId());
        if (settings.getProjectKey() != null) {
            obj.put("project-key", settings.getProjectKey());
        }
        obj.put("multi", settings.isMultiSelect());
        obj.put("auto", settings.isAutoFilter());
        return obj;
    }
    public static ProjectFieldSettings getSettings(ActiveObjects ao, String projectKey, long fieldId) {
        ProjectFieldSettings[] pfs = ao.find(ProjectFieldSettings.class, "PROJECT_KEY = ? AND FIELD_ID = ?", projectKey, fieldId);
        if (pfs.length == 0) {
            return null;
        } else {
            return pfs[0];
        }
    }
}