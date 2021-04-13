[![Build and Test Xenonflow](https://github.com/xenon-middleware/xenon-flow/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/xenon-middleware/xenon-flow/actions/workflows/build.yml)
[![DOI](https://zenodo.org/badge/63334137.svg)](https://zenodo.org/badge/latestdoi/63334137)


# Xenon-flow

Run CWL workflows using Xenon. Possibly through a REST api.

# Usage:
The following diagram shows a rough overview of the interaction when using xenonflow

![Xenon-flow Usage Pattern](docs/architecture_diagram.png "Xenon-flow Usage")

# Quick-start
## 1. Install the dependencies:
 - Java 11
 - a cwl runner

For the cwl runner you can use the reference implementation called cwltool.
It can be installed by
```bash
pip install cwltool
```
You may need to use pip3 on some systems.
For a full list of available cwl runners check https://www.commonwl.org/#Implementations

After installing the cwl runner it is a good idea to double check that your workflow can be run
using the runner on the command line

## 2. Download Xenon-flow
```
wget https://github.com/xenon-middleware/xenon-flow/releases/V1.0
unzip xenonflow-1.0.zip
```

## 3. Configure the server
Configuration of the server is done by editing the `XENONFLOW_HOME/config/config.yml` file.
As well as the `XENONFLOW_HOME/config/application.properties`.

By default it is set the use the local file system as the source and the local
computer to run workflows.

For information on which filesystems and schedulers can be used refer to the xenon documentation: https://xenon-middleware.github.io/xenon/versions/3.1.0/javadoc/.


### config.yml
Xenon-flow configuration consists of
1. `sourceFileSystem`: Any filesystem supported by Xenon can be used here
2. `targetFileSystem`: Any filesystem supported by Xenon can be used here
3. `cwlFileSystem`: Any filesystem supported by Xenon can be used here
4. `ComputeResources`: A map of compute resource descriptions for Xenon
5. Each resource has the following settings:
    1. `cwlCommand`: A script to run the cwl runner, allowing for python environments to be started first.
    	* Default:
    		```
    		#!/usr/bin/env bash

    		cwltool $@
    		```
    2. `scheduler`: A Xenon scheduler
    3. `filesystem` A Xenon filesystem
    4. Both the scheduler and filesystem have to following format:
        1. `adaptor`: The name of the Xenon adaptor (for instance slurm for scheduler or sftp for filesystem)
        2. `location`: The URI for the resource
        3. `credential`: Optional credentials (if not supplied the current user and ~/.ssh/id_rsa is used)
        	1. `user`: Username
        	2. `password`: Password in base64 encoded
        4. `properties`: Optional properties (usually not needed)

### application.properties
The application.properties needs configuration for the following things:
1. api-key
	1. `xenonflow.http.auth-token-header-name` controls the name of the header that holds the api key
	2. `xenonflow.http.auth-token` the value of the api key. IMPORTANT you should really change this one
2. The Database Configuration.
	* These settings should be changed!
    	1. `spring.datasource.username` The database username
    	2. `spring.datasource.password` The database password3.
	* The following settings can be left as is.
    	1. `server.port` The port for the server to run on.
    	2. `local.server.address=localhost` The servername.
    	3. `server.http.interface` Set up the server to be publicaly available by setting this to 0.0.0.0


## 4. Start the server
The following command will run the server.
```
./bin/xenonflow
```

## 5. Run a workflow
Put the workflow and any input files and directories to into the location as defined by the `sourceFileSystem` in the config. For instance when using a webdav server, upload the files there.

Send a POST http request with a job description to the server.

### Example:

Given the echo command-line-tool (in yaml notation):

```yaml
cwlVersion: v1.0
class: CommandLineTool
inputs:
  - id: inp
    type: string
    inputBinding: {}

outputs:
  - id: out
    type: string
    outputBinding:
      glob: out.txt
      loadContents: true
      outputEval: $(self[0].contents)

baseCommand: echo
stdout: out.txt
```

The job description looks something like the following.

Note that the input map contains a key `inp` which refers to the corresponding input of the echo command-line-tool.

```json
{
    "name": "My First Workflow",
    "workflow": "cwl/echo.cwl",
    "input": {
        "inp": "Hello CWL Server!"
    }
}
```

```bash
curl -X POST -H "Content-Type: application/json" -H "api-key: <insert api key here>" -d '{"name": "My First Workflow","workflow": "$PWD/cwl/echo.cwl","input": {"inp": "Hello CWL Server!"}}' https://localhost:8443/jobs
```



### Running Xenonflow behind a proxy server
We recommend running xenonflow behind a proxy server. Both nginx and apache httpd are good candidates for this. In addition both nginx and apache httpd can act as webdav servers which xenonflow can use as a sourceFileSystem.

Doing this requires no changes to the configuration of xenonflow as long as the correct X-forwarded-* headers are set in the proxy server.

To ensure that xenonflow returns the correct uri's for the jobs you should set the following headers:
* X-Forwarded-Host
* X-Forwarded-Server
* X-Forwarded-Proto
* X-Forwarded-Port
* X-Forwarded-Prefix

Below is an example location from a nginx config that correctly proxies a xenonflow instance running at localhost:8080
```nginx
...
location /api/ {

    include cors;
    proxy_pass http://localhost:8080/;
    proxy_redirect off;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-Proto http;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_set_header X-Forwarded-Prefix /api/;
}
...
```

### Running Xenonflow in SSL mode
To run xenonflow in ssl (https) mode you can follow the following steps:
1. Please read https://dzone.com/articles/spring-boot-secured-by-lets-encrypt for setup using Letsencrypt.
2. You should now have a certificate with a private key store
3. You should now set the following properties in the application.properties file:
   1. `server.ssl.enabled=true` Enable ssl encryption in the server
   2. `server.ssl.key-store-type` The keystore type (spring boot only supports PKCS12).
   3. `server.ssl.key-store` The store for the certificate files.
   4. `server.ssl.key-store-password` The password to the key store.
   5. `server.ssl.key-alias` The alias as given to the keystore.