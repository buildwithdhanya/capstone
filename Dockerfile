# ---------------------------------------------------------------------------
# DhanyaMart - production image for Render.com (and any Docker host)
#
#   Stage 1 : build the WAR with Maven + JDK 17
#   Stage 2 : run it on Tomcat 10.1 (Jakarta) as ROOT so the app serves "/"
# ---------------------------------------------------------------------------

# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies first (cached unless pom.xml changes)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline || true

# Compile + package
COPY src ./src
RUN mvn -q -B clean package

# ---------- runtime stage ----------
FROM tomcat:10.1-jdk17-temurin

# Serve the app at the root path so the public URL is https://<app>.onrender.com/
COPY --from=build /build/target/DhanyaMart.war /usr/local/tomcat/webapps/ROOT.war

# Entrypoint swaps Tomcat's connector port to $PORT (set by Render) at boot
COPY docker-entrypoint.sh /usr/local/tomcat/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/tomcat/bin/docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/usr/local/tomcat/bin/docker-entrypoint.sh"]