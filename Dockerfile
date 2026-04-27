FROM eclipse-temurin:25-jdk-jammy AS build

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tar ca-certificates \
 && rm -rf /var/lib/apt/lists/* \
 && MAVEN_VERSION=3.9.6 \
 && curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o /tmp/maven.tar.gz \
 && tar -xzf /tmp/maven.tar.gz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
 && rm /tmp/maven.tar.gz

ENV MAVEN_HOME=/opt/maven
ENV PATH="$MAVEN_HOME/bin:${PATH}"

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

## Copy the repackaged Spring Boot jar
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# NOTE: stray shell commands were removed. If you intended to build the TransactionService
# image, run that from a terminal in the TransactionService directory, not inside this Dockerfile.
