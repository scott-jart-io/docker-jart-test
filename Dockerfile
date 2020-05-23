FROM io.jart.focal-netmap-java11:1.0

RUN mkdir -p jart-test/src

COPY Main.java jart-test/src/.
COPY pom.xml jart-test/.
COPY jart-test.sh jart-test/.
RUN chmod +x jart-test/jart-test.sh

