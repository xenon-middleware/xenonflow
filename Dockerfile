FROM ubuntu:16.04

RUN apt-get update && apt-get install -y openjdk-8-jdk python python-pip

COPY ./src /app/src
COPY ./gradle /app/gradle
COPY ./gradlew /app/
COPY ./build.gradle /app/
COPY ./settings.gradle /app/

WORKDIR /app
RUN ./gradlew build

COPY ./config/docker-config.yml /app/config/config.yml
COPY ./config/application.properties /app/config/application.propertiess

RUN mkdir /home/xenon
RUN pip install --upgrade pip && pip install cwltool

ENTRYPOINT cd /app && ./gradlew bootRun
