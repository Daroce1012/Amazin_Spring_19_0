<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>

<!DOCTYPE html >
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<head>
<title>Amazin</title>
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
			<li><a href="#">Start</a></li>
			<li><a href="http://miw.uniovi.es">About</a></li>
			<li><a href="mailto:dd@email.com">Contact</a></li>
		</ul>
	</nav>
	<section>
		<article>
			<label class="mytitle">Introduce the new book data:</label><br />
			
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
				<input type="submit" />
			</form:form>
			<p style="color: red;">
				<c:out value="${message}" />
			</p>
		</article>
	</section>
	<footer>
		<strong> Master in Web Engineering (miw.uniovi.es).</strong><br /> <em>University
			of Oviedo </em>
	</footer>
</body>