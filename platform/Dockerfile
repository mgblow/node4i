FROM maven:3.8-jdk-11
RUN mkdir /app
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests -P production
CMD ["java", "-cp", "platform/target/platform-jar-with-dependencies.jar", "org.eclipse.milo.platform.Server", "smart-factory", "212.16.71.177", "9193", "9093"]