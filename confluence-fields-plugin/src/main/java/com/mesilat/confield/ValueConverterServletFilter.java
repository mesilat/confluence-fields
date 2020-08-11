package com.mesilat.confield;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/*
http://127.0.0.1/jira/secure/CreateIssue!default.jspa
http://127.0.0.1/jira/secure/EditIssue!default.jspa?id=11707
*/
public class ValueConverterServletFilter implements Filter{
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
        if ("POST".equals(req.getMethod())){
            if (request.getContentType().startsWith("multipart/form-data")){
                FormRequestWrapper wrapper = new FormRequestWrapper((HttpServletRequest)request, dataService);
                chain.doFilter(wrapper, response);
            } else {
                ParamRequestWrapper wrapper = new ParamRequestWrapper((HttpServletRequest)request, dataService);
                chain.doFilter(wrapper, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Inject
    public ValueConverterServletFilter(DataService dataService){
        this.dataService = dataService;
    }
}