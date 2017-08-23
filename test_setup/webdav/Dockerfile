FROM httpd:2.4.23-alpine

MAINTAINER Berend Weel "b.weel@esciencecenter.nl"

RUN apk add --no-cache apache2-webdav
RUN mkdir /webdav && mkdir /usr/local/apache2/var

COPY webdav.htpasswd /usr/local/apache2/conf/webdav.htpasswd
COPY httpd.conf /usr/local/apache2/conf/httpd.conf
