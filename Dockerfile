FROM alpine:3.7

MAINTAINER codingchili@github

# make sure to run 'gradlew build' before building the container.
# example: 'gradlew build && docker build build/distributions'
# set environment ZAPPER_PWD when running the container to set a password for the root user.
# default login is root and empty password.

COPY ./ ~/
RUN unzip ~/*

ENTRYPOINT ["/bin/sh -c", "cd ~/ && ./zapperfly.sh --user --name root --password $ZAPPER_PWD && ./zapperfly.sh"]