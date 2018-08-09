# Xenon-flow

Run CWL workflows using Xenon. Possibly through a REST api.

# Usage:
To run a workflow through Xenon-flow is quite simple:

![Xenon-flow Usage Pattern](docs/architecture_diagram.png "Xenon-flow Usage")

# Quick-start
## 1. Install the dependencies:
 - Java 8

## 2. Download Xenon-flow
```
wget https://github.com/NLeSC/xenon-flow/releases/V1.0
unzip xenonflow-1.0.zip
```

## 3. Configure the server
Configuration of the server is done by editing the `XENONFLOW_HOME/config/config.yml` file.
As well as the `XENONFLOW_HOME/config/application.properties`.

By default it is set the use the local file system as the source and the local
computer to run workflows.


### config.yml
Xenon-flow configuration consists of 
1. `sourceFileSystem`: Any filesystem supported by Xenon can be used here
2. `targetFileSystem`: Any filesystem supported by Xenon can be used here
3. `ComputeResources`: A map of compute resource descriptions for Xenon
4. Each resource has the following settings:
    1. `cwlCommand`: The command to run cwl workflows, usually cwltool
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
The application.properties needs configuration for two things:
1. api-key
	1. `xenonflow.http.auth-token-header-name` controls the name of the header that holds the api key
	2. `xenonflow.http.auth-token` the value of the api key. IMPORTANT you should really change this one
2. The SSL Configuration
	* Please read https://dzone.com/articles/spring-boot-secured-by-lets-encrypt for setup using Letsencrypt
	* The following settings can be left as is. 
	1. `server.port` The port for the server to run on.
	2. `local.server.address=localhost` The servername.
	3. `server.ssl.key-store-type` The keystore type (spring boot only supports PKCS12).
	* The following really need to be changed
	4. `server.ssl.key-store` The store for the certificate files. 
	5. `server.ssl.key-store-password` The password to the key store.
	6. `server.ssl.key-alias` The alias as given to the keystore.
	7. `server.http.interface` Set up the server to be publicaly available by setting this to 0.0.0.0


## 4. Start the server
The following command will run the server.
```
./xenonflow
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

