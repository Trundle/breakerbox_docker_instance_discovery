# Docker instance discovery for breakerbox #

Like synapse, but for [breakerbox](https://github.com/yammer/breakerbox).


## Installation ##

1. `./gradlew fatJar`

2. Add the following line to your breakerbox's properties file:
   `InstanceDiscovery.impl=de.hammerhartes.andy.breakerbox.DockerDiscovery`

3. Add your docker hosts to your breakerbox's properties file:
   `docker.hosts=http://localhost:2375`

3. Add `build/libs/docker_discovery.jar` to breakerbox's classpath. 


## Additional properties ##

| Property name              | Default  | Description                                                                                                                                           |
| -------------------------- | -------- | ----------------------------------------------------------------------------------------------------------------- |
| `blacklist.images`         | [empty]  | List of blacklisted containers by image name                                                                      |
| `docker.hosts`             | [empty]  | List of URIs to Docker Remote APIs for querying the containers                                                    |
| `cache.tenacitycheck.size` | `60`     | How long (in minutes) the result of the "does this container expose tenacity properties" check should be cached.  |


## License ##

MIT/Expat. See `LICENSE` for details.
