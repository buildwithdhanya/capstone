#!/usr/bin/env bash
# DhanyaMart container entrypoint.
# Render injects a $PORT (dynamic). Tomcat listens on 8080 by default, so we
# rewrite the connector before boot. The 8005 SHUTDOWN port is disabled because
# it is unsafe to leave open inside a container.
set -e

if [ -n "$PORT" ] && [ "$PORT" != "8080" ]; then
    sed -i "s/port=\"8080\"/port=\"$PORT\"/" /usr/local/tomcat/conf/server.xml
fi
# Disable the unsafe 8005 SHUTDOWN port inside the container
sed -i "s|port=\"8005\"|port=\"-1\"|" /usr/local/tomcat/conf/server.xml

exec /usr/local/tomcat/bin/catalina.sh run