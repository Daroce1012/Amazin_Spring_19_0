<!DOCTYPE html>
<%@ page contentType="text/html; charset=iso-8859-1"
	pageEncoding="iso-8859-1" language="java"
	import="java.util.*, com.miw.model.Cart, com.miw.model.CartItem"
	errorPage=""%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ page isELIgnored="false"%>

<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">

<head>
<title>Amazin - Shopping Cart</title>
<link href="<c:url value="/resources/css/style.css" />" rel="stylesheet">
</head>
<body>
	<header>
		<h1 class="header">Amazin.com</h1>
		<h2 class="centered">
			Welcome to the <em>smallest</em> online shop in the world!!
		</h2>
	</header>
	<nav>
		<ul>
			<li><a href="menu"><spring:message code="start"/></a></li>
			<li><a href="showBooks">Catalog</a></li>
			<li><a href="newBook">Add New</a></li>
			<li><a href="myReservations"><spring:message code="reservation.myReservations"/></a></li>
			<li><a href="http://miw.uniovi.es"><spring:message code="about"/></a></li>
			<li><a href="mailto:dd@email.com"><spring:message code="contact"/></a></li>
		</ul>
	</nav>
	<section>
		<article>
			<h2><spring:message code="cart.shoppingCart"/></h2>
			
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
			
			<c:if test="${cart.isEmpty()}">
				<p><spring:message code="cart.empty"/></p>
				<a href="showBooks"><spring:message code="cart.continueShopping"/></a>
			</c:if>
			
			<c:if test="${!cart.isEmpty()}">
				<table>
					<thead>
					<tr>
						<th><spring:message code="cart.title"/></th>
						<th><spring:message code="cart.author"/></th>
						<th><spring:message code="cart.type"/></th>
						<th><spring:message code="cart.unitPrice"/></th>
						<th><spring:message code="cart.quantity"/></th>
						<th><spring:message code="cart.subtotal"/></th>
						<th><spring:message code="cart.actions"/></th>
					</tr>
					</thead>
					<tbody>
						<c:forEach var='item' items="${cart.items}">
							<tr>
								<td><c:out value="${item.title}" /></td>
								<td><c:out value="${item.author}" /></td>
								<td>
									<c:choose>
										<c:when test="${item.reserved}">
											<span style="color: orange; font-weight: bold;">
												<spring:message code="cart.reservation"/> (95% <spring:message code="cart.pending"/>)
											</span>
										</c:when>
										<c:otherwise>
											<spring:message code="cart.purchase"/>
										</c:otherwise>
									</c:choose>
								</td>
								<td><c:out value="${item.unitPrice}" /> &euro;</td>
								<td><c:out value="${item.quantity}" /></td>
								<td><c:out value="${item.subtotal}" /> &euro;</td>
							<td>
								<form action="purchaseItem" method="post" style="display:inline; margin-right: 5px;">
									<input type="hidden" name="bookId" value="${item.bookId}"/>
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="submit" value="<spring:message code='cart.buyItem'/>"/>
								</form>
								<form action="removeFromCart" method="post" style="display:inline;">
									<input type="hidden" name="bookId" value="${item.bookId}"/>
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
									<input type="submit" value="<spring:message code='cart.remove'/>"/>
								</form>
							</td>
							</tr>
						</c:forEach>
					</tbody>
					<tfoot>
						<tr>
							<td colspan="6"><strong><spring:message code="cart.total"/>:</strong></td>
							<td><strong><c:out value="${total}" /> &euro;</strong></td>
						</tr>
					</tfoot>
				</table>
				
				<br>
				<form action="checkout" method="post">
					<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
					<input type="submit" value="<spring:message code='cart.completePurchase'/>" />
				</form>
				
				<br>
				<form action="clearCart" method="post" style="display: block; margin-bottom: 15px;">
					<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
					<input type="submit" value="<spring:message code='cart.clearCart'/>" 
					       data-confirm="<spring:message code='cart.confirmClear'/>" 
					       onclick="return confirm(this.getAttribute('data-confirm'))" />
				</form>
				
				<a href="showBooks"><spring:message code="cart.continueShopping"/></a>
			</c:if>
		</article>
	</section>
	<footer>
		<strong> Master in Web Engineering (miw.uniovi.es).</strong><br /> <em>University
			of Oviedo </em>
	</footer>
</body>
