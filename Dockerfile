FROM eclipse-temurin:21-jre-jammy
# copy application JAR (with libraries inside)

COPY target/odp-adapter-*.jar /opt/application.jar
# specify default command
CMD ["/opt/java/openjdk/bin/java", "-jar", "/opt/application.jar"]