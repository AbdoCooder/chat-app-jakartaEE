# Stage 1: Build the war file using Maven and JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Deploy the war file to TomEE
FROM tomee:jre21-Temurin-ubuntu-plus
# Delete the default Tomcat landing page
RUN rm -rf /usr/local/tomee/webapps/ROOT
COPY --from=build /app/target/*.war /usr/local/tomee/webapps/ROOT.war
EXPOSE 8080
