package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.Default;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Preload
@JsonIgnoreProperties({"entityManager", "entityProxy", "entityType"})
@JsonAutoDetect
public interface FieldSettings extends RawEntity<Long>, CommonFieldSettings  {
    @NotNull
    @PrimaryKey(value = "ID")
    Long getId();

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

    public static ObjectNode toObjectNode(ObjectMapper mapper, FieldSettings settings) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("id", settings.getId());
        obj.put("multi", settings.isMultiSelect());
        obj.put("auto", settings.isAutoFilter());
        return obj;
    }
    public static FieldSettings getSettings(ActiveObjects ao, long fieldId) {
        return ao.get(FieldSettings.class, fieldId);
    }
}