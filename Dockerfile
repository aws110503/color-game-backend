# Étape 1 : compilation avec Maven (inchangé par rapport à la version actuelle)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Étape 2 : image finale, légère, sans outils de build
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk update && apk upgrade --no-cache
# SECURITY: un conteneur qui tourne en root = accès root à l'hôte en cas de compromission.
# Création d'un utilisateur applicatif dédié, sans privilèges, sans shell de connexion.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
# SECURITY: le jar appartient à l'utilisateur applicatif, pas à root — cohérent avec
# le read_only filesystem + tmpfs /tmp défini dans docker-compose.yml.
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]