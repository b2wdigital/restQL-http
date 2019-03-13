FROM openjdk:11-jdk

RUN mkdir -p /usr/src/restql-http
ADD ./dist /usr/src/restql-http

WORKDIR /usr/src/restql-http

CMD ./bin/run.sh
