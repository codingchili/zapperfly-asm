
# zapperfly-asm [![Build Status](https://travis-ci.org/codingchili/zapperfly-asm.svg?branch=master)](https://travis-ci.org/codingchili/zapperfly-asm)
Extra-simple clustered build servers for continuous integration. Minimal configuration required, installation is as simple as starting a Java application. 

Status = IN PROGRESS (no beta yet.)

# Building
The zapperfly.jar is created by executing the following command,
```
./gradlew build
```
To start it all up, run the following on each server.
```
java -jar zapperfly-{version}.jar --port 8080 --cluster <name>
```

The web interface is accessible from each node where the --port option is specified. If the --cluster option is unset,
the cluster name will default to zapperfly-1. Build servers with different groups will not share builds. 

# Features
Planned features 😎
- build any project - as long as build tools are installed on the servers.
- build scheduling over multiple hosts
- real time log monitoring
- downloading of build artifacts
- support for simple authentication

Features not implemented 😐
- enterprise-xml-annotations-headache-complexity generator.

Possible features 😰
- Git repository caching
- Host workload tuning

# Background
Build servers are often hard to setup and involves some serious configuration overhead. The purpose of the zapperfly assembly server is 
to provide the simplest build server experience there is, with clustering. To simplify installation, minimal environmental dependencies 
are required. An optimal installation experience should include no more than installing Java. As its 2017 already, the application will
be packaged with a web interface. A very snazzy web interface, 2017-style webcomponents and websocks in action. 🐇


# Makes use of
This project will make use of chili-core, which means that vertx and hazelcast is on your classpath. What a boon!😵🌟

# License
The MIT License (MIT) Copyright (c) 2017 Robin Duda

See: LICENSE.md


