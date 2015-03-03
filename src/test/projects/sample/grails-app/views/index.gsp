<%@ page import="com.example.Book" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Welcome to Grails</title>
	</head>
	<body>
		<%
		    def book = new Book(title: 'some title')
			book.save(flush: true)
		%>
		${book.toString()}
	</body>
</html>
