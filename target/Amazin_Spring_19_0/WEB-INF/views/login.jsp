<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Amazin</title>
    <link href="<c:url value='/resources/css/style.css' />" rel="stylesheet">
</head>
<body>
	<!-- Selector de idioma -->
	<jsp:include page="languageSelector.jsp" />
	
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
        <article>
            <label class="mytitle"><spring:message code="login.title"/>:</label><br />
           <form method="post" action="<c:url value='/login' />">
           		 <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
			    <label for="username"><spring:message code="login.user"/>:</label>
			    <input type="text" id="username" name="username">
			    <br>
			    <label for="password"><spring:message code="login.password"/>:</label>
			    <input type="password" id="password" name="password">
			    <br>
			    <button type="submit"><spring:message code="login.submit"/></button>
			    <button type="reset"><spring:message code="login.reset"/></button>
			</form>

            <p style="color: red;">
		    <c:if test="${not empty errorMessage}">
		        <spring:message code="${errorMessage}"/>
		    </c:if>
			</p>
        </article>
    </section>
    <footer>
        <strong><spring:message code="footer1"/></strong><br /> 
        <em><spring:message code="footer2"/></em>
    </footer>
</body>
</html>
