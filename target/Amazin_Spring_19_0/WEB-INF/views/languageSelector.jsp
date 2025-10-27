<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<script type="text/javascript">
function changeLanguage(lang) {
    // Obtener el contexto de la aplicación
    var contextPath = '${pageContext.request.contextPath}';
    // Redirigir al controlador de cambio de idioma
    window.location.href = contextPath + '/changeLanguage?lang=' + lang;
}
</script>

<div style="text-align: right; padding: 10px; margin-bottom: 10px;">
    <label for="languageSelect"><spring:message code="language.select"/>: </label>
    <select id="languageSelect" onchange="changeLanguage(this.value)" style="padding: 5px; font-size: 14px;">
        <option value="es" ${pageContext.response.locale.language == 'es' ? 'selected' : ''}>Español</option>
        <option value="en" ${pageContext.response.locale.language == 'en' ? 'selected' : ''}>English</option>
    </select>
</div>

