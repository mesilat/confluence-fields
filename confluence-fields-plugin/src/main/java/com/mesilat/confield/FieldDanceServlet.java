package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.confluence.ConfluenceApplicationType;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.inject.Inject;

@Scanned
public class FieldDanceServlet extends HttpServlet{
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");
    
    @ComponentImport
    private final ApplicationLinkService appLinkService;
    @ComponentImport
    private final ActiveObjects ao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fieldId = req.getParameter("field-id");
        if (fieldId == null){
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Please specify field id");
        } else {
            FieldSettings fs = ao.get(FieldSettings.class, Long.parseLong(fieldId));
            if (fs == null){
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid field id");
            } else {
                try {
                    ApplicationLink link = fs.getConfluenceId() == null
                        ? appLinkService.getPrimaryApplicationLink(ConfluenceApplicationType.class)
                        : appLinkService.getApplicationLink(new ApplicationId(fs.getConfluenceId()));
                    if (link == null){
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid confluence link");
                    } else {
                        resp.sendRedirect(
                            String.format("%s/plugins/servlet/applinks/oauth/login-dance/access?applicationLinkID=%s",
                                ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL),
                                link.getId().get()
                            )
                        );
                    }
                } catch (TypeNotInstalledException ex) {
                    throw new ServletException(ex);
                }
            }
        }
    }

    @Inject
    public FieldDanceServlet(ApplicationLinkService appLinkService, ActiveObjects ao){
        this.appLinkService = appLinkService;
        this.ao = ao;
    }
}