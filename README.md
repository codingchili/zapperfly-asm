# zapperfly-asm [![Build Status](https://travis-ci.org/codingchili/zapperfly-asm.svg?branch=master)](https://travis-ci.org/codingchili/zapperfly-asm)
Extra-simple clustered build servers for continuous integration. Minimal configuration required, installation is as simple as starting a Java application. [Demo video!](https://www.youtube.com/watch?v=t4fKgGerj8I)

![alt text](https://raw.githubusercontent.com/codingchili/zapperfly-asm/master/preview.webp "Current snapshot version")

# Installing

Download the latest release from [releases](https://github.com/codingchili/zapperfly-asm/releases), unpack where you want to run it.

##### To save the default configuration to file (optional)
```console
./zapperfly --configure
```

##### Add users with the following command.
```console
./zapperfly --user --name <userName> --pass <userPassword> --role <admin|user>
```

--pass: if omitted a random password will be generated.

--role: if omitted the role will default to 'user'.


##### To start it all up, run the following on each host.
```console
./zapperfly --start --group <groupName> --name <instanceName> --website <port, default 443>
```

--group: specifies the virtual group for the instances, different groups will not cluster together.

--name: the name of the instance as displayed in the website or when listing available executors.

--website: starts the graphical user interface on port 443, or the specified port.

##### If you want to build it yourself, clone this repository with git and run:
```console
./gradlew archiveZip
```

# Run with docker

Run the following command to build the docker image

```console
gradlew archiveZip && docker build -f Dockerfile build/distributions
```

Running the container
```console
docker run -it -p 5701:5701 -e ZAPPER_PWD=secret <image-id>
```

-p is optional, required for clustering. increase the first port in the pair
for each container.

-e is optional, ZAPPER_PWD is the password of the 'root' user. if unset
the default password is blank.

Once the container is started the web interface is available on https://<container-ip>/.

# Features 😎
- build scheduling over multiple hosts
- real time log monitoring
- running the build sersver in a docker container.
- running builds in docker containers.
- tune workload per instance based on number of parallel builds.
- support for role-base authentication
- basic plugin system for integrations

There are three levels of authorization,

- admin: may add build configurations and edit existing. (is also an user)
- user: may view build logs and start builds.
- public: may view the build queue and build history - no build logs.

# Background
Build servers are often hard to setup and involves some serious configuration overhead. The purpose of the zapperfly assembly server is 
to provide the simplest build server experience there is, with clustering. To simplify installation, minimal environmental dependencies 
are required. An optimal installation experience should include no more than installing Java. We aim to make the core product small
enough so that you can modify it according to your own needs. 🐇

# Makes use of
This project makes use of chili-core, which means that vertx and hazelcast is on your classpath. What a boon!😵🌟

# License
The MIT License (MIT) Copyright (c) 2018 Robin Duda

See: LICENSE.md

# Contributing
pull requests, code reviews, feature suggestions and more welcome. :smile_cat:  :cherry_blossom:

[![donate](https://img.shields.io/badge/donate-%CE%9ETH%20/%20%C9%83TC-ff00cc.svg?style=flat&logo=ethereum)](https://commerce.coinbase.com/checkout/673e693e-be6d-4583-9791-611da87861e3)
