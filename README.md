# zapperfly-asm [![Build Status](https://travis-ci.org/codingchili/zapperfly-asm.svg?branch=master)](https://travis-ci.org/codingchili/zapperfly-asm)
Extra-simple clustered build servers for continuous integration. Minimal configuration required, installation is as simple as starting a Java application. [Demo video!](https://www.youtube.com/watch?v=t4fKgGerj8I)

![alt text](https://raw.githubusercontent.com/codingchili/zapperfly-asm/master/preview.png "Current snapshot version")

# Installing

Download the latest release from [releases](https://github.com/codingchili/zapperfly-asm/releases), unpack where you want to run it.

To start it all up, run the following on each host.
```
./run start --group <groupName> --name <instanceName> --website <port, default 443>
```

If you want to build it yourself, clone this repository with git and run:
```
./gradlew archiveZip
```

Optional commandline arguments:

--group: specifies the virtual group for the instances, different groups will not cluster together.

--name: the name of the instance as displayed in the website or when listing available executors.

--website: starts the graphical user interface on port 443, or the specified port.

# Features 😎
- build scheduling over multiple hosts
- real time log monitoring
- running builds in docker containers.
- tune workload per instance based on number of parallel builds.
- downloading of build artifacts (TBD)
- support for simple authentication (TBD)

Features not implemented 😐
- enterprise-xml-annotations-headache-complexity generator.

# Background
Build servers are often hard to setup and involves some serious configuration overhead. The purpose of the zapperfly assembly server is 
to provide the simplest build server experience there is, with clustering. To simplify installation, minimal environmental dependencies 
are required. An optimal installation experience should include no more than installing Java. As its 2018 already, the application will
be packaged with a web interface. A very snazzy web interface, 2018-style web-components in action. 🐇

# Makes use of
This project makes use of chili-core, which means that vertx and hazelcast is on your classpath. What a boon!😵🌟

# License
The MIT License (MIT) Copyright (c) 2017 Robin Duda

See: LICENSE.md

# Contributing
yes, please. :smile_cat:  :cherry_blossom:
