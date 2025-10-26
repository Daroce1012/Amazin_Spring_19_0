<!DOCTYPE html>
<%@ page contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" language="java"
	import="java.util.*, com.miw.model.Reservation"
	errorPage=""%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ page isELIgnored="false"%>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<head>
<title>Amazin - My Reservations</title>
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
			<li><a href="viewCart"><spring:message code="cart.viewCart"/></a></li>
			<li><a href="myReservations"><spring:message code="reservation.myReservations"/></a></li>
			<li><a href="http://miw.uniovi.es"><spring:message code="about"/></a></li>
			<li><a href="mailto:dd@email.com"><spring:message code="contact"/></a></li>
		</ul>
	</nav>
	<section>
		<article>
			<h2><spring:message code="reservation.myReservations"/></h2>
			
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
			
			<!-- Verificar si hay reservas -->
			<c:if test="${empty reservations}">
				<p><spring:message code="reservation.noReservations"/></p>
				<p><a href="showBooks"><spring:message code="navigation.backToCatalog"/></a></p>
			</c:if>
			
			<c:if test="${!empty reservations}">
				<table>
					<thead>
					<tr>
						<th><spring:message code="cart.title"/></th>
						<th><spring:message code="cart.author"/></th>
						<th><spring:message code="cart.unitPrice"/></th>
						<th><spring:message code="cart.quantity"/></th>
						<th><spring:message code="reservation.paidAmount"/> (5%)</th>
						<th><spring:message code="reservation.remainingAmount"/> (95%)</th>
						<th><spring:message code="reservation.date"/></th>
						<th><spring:message code="reservation.actions"/></th>
					</tr>
					</thead>
					<tbody>
						<c:forEach var='reservation' items="${reservations}">
							<tr>
								<td><c:out value="${reservation.book.title}" /></td>
								<td><c:out value="${reservation.book.author}" /></td>
								<td><c:out value="${reservation.book.price}" /> &euro;</td>
								<td><c:out value="${reservation.quantity}" /></td>
								<td><c:out value="${reservation.reservationPrice}" /> &euro;</td>
								<td><c:out value="${reservation.remainingAmount}" /> &euro;</td>
								<td><c:out value="${reservation.reservationDate}" /></td>
								<td>
									<form action="purchaseReservation" method="post" style="display:inline;">
										<input type="hidden" name="reservationId" value="${reservation.id}"/>
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<input type="submit" value="<spring:message code='reservation.buy'/>"/>
									</form>
									<form action="cancelReservationFromPage" method="post" style="display:inline; margin-left: 5px;">
										<input type="hidden" name="reservationId" value="${reservation.id}"/>
										<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
										<input type="submit" value="<spring:message code='reservation.cancel'/>" 
										       data-confirm="<spring:message code='reservation.confirmCancel'/>"
										       onclick="return confirm(this.getAttribute('data-confirm'))"/>
									</form>
								</td>
							</tr>
						</c:forEach>
					</tbody>
					<tfoot>
						<tr>
							<td colspan="4"><strong><spring:message code="cart.total"/> (<spring:message code="reservation.paidAmount"/>):</strong></td>
							<td><strong>
								<c:set var="totalPaid" value="0" />
								<c:forEach var='reservation' items="${reservations}">
									<c:set var="totalPaid" value="${totalPaid + reservation.reservationPrice}" />
								</c:forEach>
								<c:out value="${totalPaid}" /> &euro;
							</strong></td>
							<td colspan="3"></td>
						</tr>
						<tr>
							<td colspan="4"><strong><spring:message code="cart.total"/> (<spring:message code="reservation.remainingAmount"/>):</strong></td>
							<td><strong>
								<c:set var="totalRemaining" value="0" />
								<c:forEach var='reservation' items="${reservations}">
									<c:set var="totalRemaining" value="${totalRemaining + reservation.remainingAmount}" />
								</c:forEach>
								<c:out value="${totalRemaining}" /> &euro;
							</strong></td>
							<td colspan="3"></td>
						</tr>
					</tfoot>
				</table>
				
				<p>
					<a href="showBooks"><spring:message code="navigation.backToCatalog"/></a> | 
					<a href="viewCart"><spring:message code="cart.viewCart"/></a>
				</p>
			</c:if>
		</article>
	</section>
	<footer>
		<strong> <spring:message code="footer1"/></strong><br /> 
		<em><spring:message code="footer2"/> </em>
	</footer>
</body>
</html>

