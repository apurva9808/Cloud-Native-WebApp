# webapp

created a 2nd branch named work 
made restapi 
checked on postman


Web App Repository for CSYE 6225
This repository will contain the contents for a basic Spring Boot API for the first Assignment for CSYE-6225
CloudCsyeApplication
This is a Spring Boot project that connects to a PostgreSQL database. The application provides a /healthz endpoint, which returns HTTP status 200 OK when the database connection is healthy, and 503 Service Unavailable when the database is down.

Prerequisites
Java 19 (OpenJDK)
PostgreSQL 15 (installed via Homebrew)
Setup Instructions
PostgreSQL Setup
Install PostgreSQL 15 using Homebrew:
bash
Copy code
brew install postgresql
Start the PostgreSQL server by using the following command:
bash
Copy code
brew services start postgresql
Ensure that the PostgreSQL server is up and running before launching the application.
Running the Application
Ensure that Java 19 is installed and properly configured on your machine.
Navigate to the project directory where the CloudCsyeApplication resides.
Run the Spring Boot application using the command below:
bash
Copy code
./mvnw spring-boot:run

added yml file for test



Health Check Flow
To test the application's health check endpoint, follow these steps:

Start the PostgreSQL server.
Run the Spring Boot application.
Access the /healthz endpoint using a browser or an HTTP client tool (e.g., curl):
If the database is connected, the response should return HTTP status 200 OK.
Stop the PostgreSQL server using the appropriate command:
bash
Copy code
brew services stop postgresql
Access the /healthz endpoint again:
The response should return HTTP status 503 Service Unavailable, indicating the database is down.

set secret in github
add region also
add values
 addsecret key
 added value

added config secret

added changes

changed organ
added logger in postmapping
