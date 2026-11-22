# syntax=docker/dockerfile:1
#
# Billing-Dienst (witch-billing-server) als Image.
# Build:  docker build --secret id=github_token,src=.github_token -t witch-billing:latest .
# Das GitHub-Token (classic, Scope read:packages) wird fuer witch-parent, witch-ui und
# witch-auth aus GitHub Packages gebraucht. Es wird als BuildKit-Secret eingebunden und
# landet weder im Image noch im Build-Cache. witch-billing-client baut Maven im selben Lauf mit.

# ---- Build-Stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY witch-billing-client ./witch-billing-client
COPY witch-billing-server ./witch-billing-server
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=github_token,required=true \
    printf '<settings><servers><server><id>github</id><username>token</username><password>%s</password></server></servers></settings>' \
         "$(tr -d '\r\n' < /run/secrets/github_token)" > /tmp/settings.xml \
    && mvn -s /tmp/settings.xml -pl witch-billing-server -am package -DskipTests -B -q \
    && rm /tmp/settings.xml

# ---- Runtime-Stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/witch-billing-server/target/witch-billing-server-*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
