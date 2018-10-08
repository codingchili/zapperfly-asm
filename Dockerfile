FROM anapsix/alpine-java:latest

MAINTAINER codingchili@github

# make sure to run 'gradlew archiveZip' before building the container.
# example: 'gradlew archiveZip && docker build -f Dockerfile build/distributions'
# set environment ZAPPER_PWD when running the container to set a password for the root user.
# default login is root and empty password.

RUN mkdir -p /opt/zapper/
COPY ./ /opt/zapper/

RUN apk add git && \
    cd /opt/zapper && \
    unzip ./* && \
    rm *.zip

EXPOSE 443/tcp

# if you need hazelcast clustering set -p 5701:5701 to enable
# multicast discovery. For additional containers increase external port.
# example, 5702:5701, 5703:5701 etc.

ENTRYPOINT ["/bin/sh", "-c", "cd /opt/zapper && ./zapperfly.sh --user --role admin --name root --pass $ZAPPER_PWD && ./zapperfly.sh --start --website"]
