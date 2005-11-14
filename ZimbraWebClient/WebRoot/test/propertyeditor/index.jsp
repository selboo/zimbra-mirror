<!-- 
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite Web Client

The Initial Developer of the Original Code is Zimbra, Inc.
Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
All Rights Reserved.

Contributor(s):

***** END LICENSE BLOCK *****
-->

<%@ page language="java" 
         import="java.lang.*, java.util.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Property Editor test</title>
    <script language="JavaScript">
    	DwtConfigPath = "/ajax/js/dwt/config";
    </script>
    	
<% 
   String contextPath = (String)request.getContextPath(); 
   String vers = (String)request.getAttribute("version");
   String ext = (String)request.getAttribute("fileExtension");
   if (vers == null){
      vers = "";
   }
   if (ext == null){
      ext = "";
   }
%>
    <jsp:include page="../../public/Messages.jsp"/>
    <jsp:include page="../../public/Ajax.jsp"/>
    <jsp:include page="../../public/Dwt.jsp"/>
    <script type="text/javascript" src="<%= contextPath %>/js/ajax/dwt/widgets/DwtPropertyEditor.js<%= ext %>?v=<%= vers %>"></script>
    <script type="text/javascript" src="script.js"></script>

<style type="text/css">
<!--
<%String hiRes = (String) request.getParameter("hiRes");
  // load dynamically generated image files
  if (hiRes != null) {
%>
        @import url(/zimbra/img/hiRes/imgs.css?v=<%= vers %>);
        @import url(/zimbra/img/hiRes/skins/steel/skin.css?v=<%= vers %>);
<% } else { %>
        @import url(/zimbra/img/loRes/imgs.css?v=<%= vers %>);
        @import url(/zimbra/img/loRes/skins/steel/skin.css?v=<%= vers %>);
<% } %>
        @import url(/zimbra/js/zimbraMail/config/style/dwt.css?v=<%= vers %>);
        @import url(/zimbra/js/zimbraMail/config/style/common.css?v=<%= vers %>);
        @import url(/zimbra/js/zimbraMail/config/style/zm.css?v=<%= vers %>);
        @import url(/zimbra/js/zimbraMail/config/style/spellcheck.css?v=<%= vers %>);
        @import url(/zimbra/skins/steel/skin.css?v=<%= vers %>);
-->
</style>
    <style type="text/css">
      <!--
        @import url(style.css);
      -->
    </style>

  </head>
    <body>
    <noscript><p><b>Javascript must be enabled to use this.</b></p></noscript>
    <script language="JavaScript">   	
   		function launch() {
   			DBG = new AjxDebug(AjxDebug.NONE, null, false);
 	    	App.run();
	    }
        AjxCore.addOnloadListener(launch);
    </script>
    </body>
</html>

