<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<!DOCTYPE html>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<head>
<title>Amazin</title>
<link href="<c:url value="/resources/css/style.css" />" rel="stylesheet">
</head>
<body>
	<!-- Selector de idioma -->
	<jsp:include page="../languageSelector.jsp" />
	
	<header>
		<h1 class="header">Amazin.com</h1>
		<h2 class="centered">
			<spring:message code="welcome"/>
		</h2>
	</header>
	<nav>
		<ul>
			<li><a href="#"><spring:message code="start"/></a></li>
			<li><a href="http://miw.uniovi.es"><spring:message code="about"/></a></li>
			<li><a href="mailto:dd@email.com"><spring:message code="contact"/></a></li>
		</ul>
	</nav>
	<section>
		<article id="a01">
			<label class="mytitle"><spring:message code="menu.chooseOption"/>:</label><br /> 
			<a href="showBooks"><spring:message code="menu.showCatalog"/></a><br /> 
			<a href="showSpecialOffer"><spring:message code="menu.showSpecialOffers"/></a><br>
			<spring:message code="menu.visits"/>: <c:out value="${loginCounter.logins}"/> 
		</article>
	</section>
	<footer>
		<strong> <spring:message code="footer1"/></strong><br /> 
		<em><spring:message code="footer2"/> </em>
	</footer>
</body>
