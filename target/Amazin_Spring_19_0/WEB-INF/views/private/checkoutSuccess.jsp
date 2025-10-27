<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" language="java"
	errorPage=""%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ page isELIgnored="false"%>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<head>
<title>Amazin - Purchase Successful</title>
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
			<li><a href="newBook"><spring:message code="navigation.addNew"/></a></li>
			<li><a href="http://miw.uniovi.es"><spring:message code="about"/></a></li>
			<li><a href="mailto:dd@email.com"><spring:message code="contact"/></a></li>
		</ul>
	</nav>
	<section>
		<article>
			<h2><spring:message code="cart.purchaseSuccess"/></h2>
			
			<div style="background-color: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 15px; margin: 20px 0; border-radius: 5px;">
				<spring:message code="${message}"/>
			</div>
			
			<p><spring:message code="cart.thankYou"/></p>
			
			<br>
			<a href="showBooks"><spring:message code="cart.continueShopping"/></a>
		</article>
	</section>
	<footer>
		<strong> <spring:message code="footer1"/></strong><br /> 
		<em><spring:message code="footer2"/> </em>
	</footer>
</body>
