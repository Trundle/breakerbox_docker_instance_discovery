# Docker instance discovery for breakerbox #

Like synapse, but for [breakerbox](https://github.com/yammer/breakerbox).


## Installation ##

1. `./gradlew fatJar`

2. Add the following line to your breakerbox's properties file:
   `InstanceDiscovery.impl=de.hammerhartes.andy.breakerbox.DockerDiscovery`

3. Add your docker hosts to your breakerbox's properties file:
   `docker.hosts=http://localhost:2375`

3. Add `build/libs/docker_discovery.jar` to breakerbox's classpath. 
