# docker-jart-test
Installs jart test source and script on top of the [docker-netmap-java11](https://github.com/scott-jart-io/docker-netmap-java11) image

Build:

Make sure you've built [docker-netmap-java11](https://github.com/scott-jart-io/docker-netmap-java11)

```
git clone https://github.com/scott-jart-io/docker-jart-test.git
cd docker-jart-test
make
```

Run:

```
docker run --rm -it docker-jart-test
```

In container, build and run test code:

```
cd jart-test
./jart-test.sh
```
