# Un seul service sert le site et l'API.
#
# Le frontend compile est embarque dans les ressources statiques du jar : une
# seule origine, donc aucune question d'origine croisee, et un seul deploiement
# a surveiller. C'est aussi ce qui permet au frontend d'appeler l'API en chemin
# relatif, et donc de fonctionner derriere n'importe quel domaine sans etre
# recompile.

# ---------------------------------------------------------------- site
FROM node:22-alpine AS front
WORKDIR /front

# Les dependances d'abord : cette couche est reutilisee tant que package-lock
# ne change pas, ce qui evite de retelecharger a chaque commit.
COPY Front/package.json Front/package-lock.json ./
RUN npm ci

COPY Front/ ./
RUN npm run build


# ---------------------------------------------------------------- api
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY back-spring/pom.xml ./pom.xml
COPY back-spring/.mvn .mvn
COPY back-spring/mvnw ./mvnw
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY back-spring/src src

# Le site rejoint les ressources statiques avant l'empaquetage. Il cohabite
# avec swagger.html et openapi.yaml, deja presents.
COPY --from=front /front/dist/ecommerce-front/browser/ src/main/resources/static/

RUN ./mvnw clean package -DskipTests


# ---------------------------------------------------------------- execution
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Emplacement par defaut des visuels. En production il doit pointer vers un
# volume persistant : le systeme de fichiers d'un conteneur est efface a chaque
# deploiement, et les visuels des vendeurs avec lui.
ENV APP_UPLOADS_DIR=/var/data/uploads/products
RUN mkdir -p /var/data/uploads/products

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
