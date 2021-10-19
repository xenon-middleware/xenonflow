[![Build and Test Xenonflow](https://github.com/xenon-middleware/xenon-flow/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/xenon-middleware/xenon-flow/actions/workflows/build.yml)
[![DOI](https://zenodo.org/badge/63334137.svg)](https://zenodo.org/badge/latestdoi/63334137)


# Xenonflow
Run CWL workflows using Xenon through a REST api.

# Usage:
The following diagram shows a rough overview of the interaction when using xenonflow.
The overview shows 3 file systems, all of which can be configured for Xenonflow (See the quick-start guide below).
1. The input filesystem, this should contain all the input files needed for running the cwl workflow
2. The cwl filesystem, this filesystem should contain the cwl workflows you want to run with Xenonflow.
3. The output filesystem, this is where xenonflow will put the output of the workflows.

On the right you can see a compute resource: Xenonflow can be configured to run on a number of computing backends, including the local machine, to actually execute the cwl workflow.

Before making a call to the Xenonflow REST API make sure the data is available on the input filesystem and the workflow is available on the cwl filesystems.
The rest call will return a JSON object which contains some information on the job you just submitted. Such as its current state, what input was provided, a uri to the job (for instance to poll the server for new states) and a uri to the log of the job.

After the workflow is completed the results will be available in the target filesystem

![Xenonflow Usage Pattern](https://user-images.githubusercontent.com/16776108/116083884-718bdd80-a69d-11eb-982a-7351b1e586f3.png)


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
    		```![xenonflow (3)](https://user-images.githubusercontent.com/16776108/116083865-6d5fc000-a69d-11eb-81ca-5e82fa2c6727.png)

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

#### Environment Variables
There are two environment variables that can be set in your environement which can then be
used in the config.yml file: `XENONFLOW_FILES` and `XENONFLOW_HOME`.

 

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
curl -X POST -H "Content-Type: application/json" -H "api-key: <insert api key here>" -d '{"name": "My First Workflow","workflow": "$PWD/cwl/echo.cwl","input": {"inp": "Hello CWL Server!"}}' http://localhost:8080/jobs
```

### Using the jobid or jobname in a workflow
If you need access to the jobid generated by xenonflow, or the jobname that was used to submit the workflow
then you can add them as inputs to your cwl file as parameters with the ids `xenonflow_jobid` and `xenonflow_jobname` respectively.
Xenonflow will then automatically inject the values into the job-order.json as input to the cwl file.

For example the following cwl file will echo the xenonflow_jobid and xenonflow_jobname:
```yaml
cwlVersion: v1.0
class: CommandLineTool
inputs:
  - id: xenonflow_jobid
    type: string
    inputBinding:
      position: 1
  - id: xenonflow_jobname
    type: string
    inputBinding:
      position: 2

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

