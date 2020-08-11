package com.mesilat.confield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParamRequestWrapper extends HttpServletRequestWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");
    private final DataService dataService;

    @Override
    public String getParameter(String name){
        if (dataService.isConfluenceField(name)){
            return super.getParameter(name) == null? null
                : super.getParameter(name).replace("~[$]~", ",");
        } else {
            return super.getParameter(name);
        }
    }
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> map = new HashMap<>();
        super.getParameterMap().forEach((key, val) -> {
            if (dataService.isConfluenceField(key)){
                if (val == null){
                    map.put(key, val);
                } else {
                    List<String> arr = new ArrayList<>();
                    for (int i = 0; i < val.length; i++){
                        arr.add(val[i] == null? null: val[i].replace("~[$]~", ","));
                    }
                    map.put(key, arr.toArray(new String[]{}));
                }
            } else {
                map.put(key, val);
            }
        });
        return map;
    }
    @Override
    public Enumeration<String> getParameterNames() {
        return super.getParameterNames();
    }
    @Override
    public String[] getParameterValues(String name) {
        if (dataService.isConfluenceField(name)){
            //return super.getParameterValues(name);
            List<String> values = new ArrayList<>();
            String[] arr = super.getParameterValues(name);
            if (arr == null){
                return arr;
            }
            Arrays.asList(arr).forEach(val -> {
                values.add(val == null? null: val.replace("~[$]~", ","));
            });
            return values.toArray(new String[] {});
        } else {
            return super.getParameterValues(name);
        }
    }

    public ParamRequestWrapper(HttpServletRequest request, DataService dataService){
        super(request);
        this.dataService = dataService;
    }
}