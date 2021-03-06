#!/bin/bash
VER="4.0.0-SNAPSHOT"
echo "Version is $VER"
TARGET=installable
SERVER=../../../csp-apps/configuration/conf-server/target/conf-server-$VER-exec.jar
CLIENT=../../../csp-apps/configuration/conf-client-cspapp/target/conf-client-cspapp-$VER.jar
mkdir -p $TARGET
echo "Saving csp-docker-java8..."
docker save csp-docker-java8 |bzip2 - > $TARGET/image-dj8.tbz2
echo "Saving csp-java8..."
docker save csp-java8 | bzip2 - > $TARGET/image-j8.tbz2
cp docker-compose.yml $TARGET
cp $SERVER $TARGET
cp $CLIENT $TARGET



