<%@ taglib uri="tld/webwork.tld" prefix="ww" %>
<%@ taglib uri="tld/webwork.tld" prefix="ui" %>
<%@ taglib uri="tld/webwork.tld" prefix="aui" %>
<html>
<head>
	<title><ww:text name="'com.mesilat.confluence-field.pickurl.title'" /></title>
    <%@ include file="/includes/js/multipickerutils.jsp" %>
</head>
<body>
    <ui:soy moduleKey="'jira.webresources:soy-templates'" template="'JIRA.Templates.Headers.pageHeader'">
        <ui:param name="'mainContent'">
            <h1><ww:text name="'com.mesilat.confluence-field.pickurl.title'" /></h1>
        </ui:param>
    </ui:soy>

    <ui:soy moduleKey="'com.atlassian.auiplugin:aui-experimental-soy-templates'" template="'aui.page.pagePanel'">
        <ui:param name="'content'">
            <ui:soy moduleKey="'com.atlassian.auiplugin:aui-experimental-soy-templates'" template="'aui.page.pagePanelContent'">
                <ui:param name="'content'">

            <ww:if test="permission == true">
                <script type="text/javascript">
                    function select(value)
                    {
                        //opener.AJS.$('#'+AJS.$.trim(AJS.$("#openElement").text())).val(value).change();
                        alert(value);
                        window.close();
                    }
                </script>
                <p>Select url</p>
            </ww:if>
            <ww:else>
                <aui:component template="auimessage.jsp" theme="'aui'">
                    <aui:param name="'messageType'">warning</aui:param>
                    <aui:param name="'messageHtml'">
                        <p><ww:text name="'userpicker.nopermissions'" /></p>
                    </aui:param>
                </aui:component>
            </ww:else>

                </ui:param>
            </ui:soy>
        </ui:param>
    </ui:soy>
</body>
</html>
