FROM ubuntu:16.04

MAINTAINER Berend Weel "b.weel@esciencecenter.nl"

RUN apt-get update && \
    apt-get install -y --no-install-recommends build-essential openjdk-8-jdk python python-pip python-dev && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

COPY ./src /app/src
COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle /app/
COPY ./settings.gradle /app/

WORKDIR /app
RUN ./gradlew build -x test

RUN mkdir /home/xenon
RUN pip install --upgrade pip==9.0.3 && \
    pip install setuptools && \
    pip install cwltool

COPY ./config/docker-config.yml /app/config/config.yml

RUN mkdir /running-jobs
RUN mkdir /output

EXPOSE 8080

ENTRYPOINT cd /app && ./gradlew bootRun
