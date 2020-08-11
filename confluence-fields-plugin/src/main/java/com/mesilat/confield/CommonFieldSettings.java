package com.mesilat.confield;

public interface CommonFieldSettings {
    String getConfluenceId();
    void setConfluenceId(String confluenceId);

    String getFilter();
    void setFilter(String filter);

    boolean isMultiSelect();
    void setMultiSelect(boolean multiSelect);

    boolean isAutoFilter();
    void setAutoFilter(boolean autoFilter);

    String getDefiner();
    void setDefiner(String definer);
}
