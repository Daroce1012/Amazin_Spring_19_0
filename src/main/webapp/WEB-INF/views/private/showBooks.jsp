<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" language="java"
	import="java.util.*, com.miw.model.Book,com.miw.presentation.book.*"
	errorPage=""%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
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
			<li><a href="newBook"><spring:message code="navigation.addNew"/></a></li>
			<li><a href="viewCart"><spring:message code="cart.viewCart"/></a></li>
			<li><a href="myReservations"><spring:message code="reservation.myReservations"/></a></li>
			<li><a href="http://miw.uniovi.es"><spring:message code="about"/></a></li>
			<li><a href="mailto:dd@email.com"><spring:message code="contact"/></a></li>
		</ul>
	</nav>
	<section>
		<article>
			<!-- Mostrar mensajes de éxito o error -->
			<c:if test="${not empty message}">
				<div style="background-color: #d4edda; border: 1px solid #c3e6cb; color: #155724; padding: 15px; margin: 20px 0; border-radius: 5px;">
					<spring:message code="${message}"/>
				</div>
			</c:if>
			
			<c:if test="${not empty error}">
				<div style="background-color: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; padding: 15px; margin: 20px 0; border-radius: 5px;">
					<spring:message code="${error}"/>
				</div>
			</c:if>
			
			<table>
				<caption><spring:message code="catalog.title"/>:</caption>
				<thead>
					<tr>
						<th><spring:message code="book.title"/></th>
						<th><spring:message code="book.author"/></th>
						<th><spring:message code="book.description"/></th>
						<th><spring:message code="book.price"/></th>
						<th><spring:message code="book.stock"/></th>
						<th><spring:message code="cart.addToCart"/></th>
						<th><spring:message code="reservation.reserve"/></th>
					</tr>
				</thead>

				<tbody>
					<c:forEach var='book' items="${booklist}">
						<tr>
							<td><c:out value="${book.title}" /></td>
							<td><c:out value="${book.author}" /></td>
							<td><c:out value="${book.description}" /></td>
							<td><c:out value="${book.price}" /> &euro;</td>
							<td><c:out value="${book.stock}" /> <spring:message code="book.units"/></td>
							<td>
								<c:if test="${book.stock > 0}">
									<form action="addToCart" method="post" style="display: inline;">
										<input type="hidden" name="bookId" value="${book.id}" />
										<input type="number" name="quantity" value="1" min="1" max="${book.stock}" style="width: 50px;" />
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<input type="submit" value="<spring:message code='cart.addToCart'/>" />
									</form>
								</c:if>
								<c:if test="${book.stock <= 0}">
									<span style="color: red;"><spring:message code="cart.outOfStock"/></span>
								</c:if>
							</td>
							<td>
								<c:if test="${book.stock > 0}">
									<form action="reserveBook" method="post" style="display: inline;">
										<input type="hidden" name="bookId" value="${book.id}" />
										<input type="number" name="quantity" value="1" min="1" max="${book.stock}" style="width: 50px;" />
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<input type="submit" value="<spring:message code='reservation.reserve'/>" />
									</form>
								</c:if>
								<c:if test="${book.stock <= 0}">
									<span style="color: red;"><spring:message code="cart.outOfStock"/></span>
								</c:if>
							</td>
						</tr>						
					</c:forEach>
				</tbody>
			</table>
		</article>
	</section>
	<footer>
		<strong> <spring:message code="footer1"/></strong><br /> 
		<em><spring:message code="footer2"/> </em>
	</footer>
</body>