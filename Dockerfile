FROM ubuntu:16.04

RUN apt-get update && apt-get install -y openjdk-8-jdk

COPY ./src /app/src
COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle /app/
COPY ./settings.gradle /app/
COPY ./docker-config.yml /app/

WORKDIR /app
RUN ./gradlew build

ENTRYPOINT /bin/bash