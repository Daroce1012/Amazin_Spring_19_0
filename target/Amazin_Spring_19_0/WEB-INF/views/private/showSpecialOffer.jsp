<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" language="java"
	import="java.util.*, com.miw.model.Book,com.miw.presentation.book.*"
	errorPage=""%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page isELIgnored="false"%>

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
			<table>
				<caption><spring:message code="specialOffer.title"/>:</caption>
				<thead>
					<tr>
						<th><spring:message code="book.title"/></th>
						<th><spring:message code="book.author"/></th>
						<th><spring:message code="book.description"/></th>
						<th><spring:message code="book.price"/></th>
					</tr>
				</thead>

				<tbody>
					<tr>
						<td><c:out value="${book.title}" /></td>
						<td><c:out value="${book.author}" /></td>
						<td><c:out value="${book.description}" /></td>
						<td><c:out value="${String.format('%.2f', book.price)}" /> &euro;</td>
					</tr>
				</tbody>
			</table>
		</article>
	</section>
	<footer>
		<strong> <spring:message code="footer1"/></strong><br /> 
		<em><spring:message code="footer2"/> </em>
	</footer>
</body>