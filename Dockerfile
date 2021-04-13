FROM ubuntu:20.04

LABEL maintainer="Berend Weel <b.weel@esciencecenter.nl>"

RUN apt-get update && \
	apt-get install -y --no-install-recommends build-essential openjdk-11-jdk python3 python3-dev python3-pip && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN mkdir /home/xenon
RUN pip3 install --upgrade pip && \
    pip3 install setuptools && \
    pip3 install cwltool

RUN cwltool --version

COPY ./src /app/src
COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle /app/
COPY ./settings.gradle /app/
COPY ./cwl /app/cwl

WORKDIR /app
RUN ./gradlew build -x test

COPY ./config/docker-config.yml /app/config/config.yml
COPY ./config/application.properties /app/config/application.properties

RUN mkdir /running-jobs
RUN mkdir /output

EXPOSE 8080

WORKDIR /app
CMD ./gradlew bootRun
