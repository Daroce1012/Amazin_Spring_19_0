<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

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
			<li><a href="menu"><spring:message code="start"/></a></li>
			<li><a href="showBooks"><spring:message code="catalog.title"/></a></li>
			<li><a href="http://miw.uniovi.es"><spring:message code="about"/></a></li>
			<li><a href="mailto:dd@email.com"><spring:message code="contact"/></a></li>
		</ul>
	</nav>
	<section>
		<article>
			<label class="mytitle"><spring:message code="newBook.title"/>:</label><br />
			
			<form:form modelAttribute="book">
				<form:errors path="" />
				<br />
				<spring:message code="book.title"/>: <form:input path="title" />
				<form:errors path="title" />
				<br />
				<spring:message code="book.description"/>: <form:input path="description" />
				<form:errors path="description" />
				<br />
				<spring:message code="book.author"/>: <form:input path="author" />
				<form:errors path="author" />
				<br />
				<spring:message code="book.basePrice"/>: <form:input path="basePrice" />
				<form:errors path="basePrice" />
				<br />
				<spring:message code="book.stock"/>: <form:input path="stock" />
				<form:errors path="stock" />
				<br />
				<input type="submit" value="<spring:message code='newBook.submit'/>" />
			</form:form>
			<p style="color: red;">
				<c:out value="${message}" />
			</p>
		</article>
	</section>
	<footer>
		<strong> <spring:message code="footer1"/></strong><br /> 
		<em><spring:message code="footer2"/> </em>
	</footer>
</body>