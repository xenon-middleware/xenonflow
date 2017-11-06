# Xenon-flow

# Note: This runner is a prototype.
Run CWL workflows using Xenon. Possibly through a REST api.

# Usage:
To run a workflow through Xenon-flow is quite simple:

![Xenon-flow Usage Pattern](docs/architecture_diagram.png "Xenon-flow Usage")

# Quick-start
## 1. Install the dependencies:
 - Java 8

## 2. Download Xenon-flow
```
git clone https://github.com/NLeSC/xenon-flow.git
```

## 3. Configure the server
Configuration of the server is done by editing the `config/config.yml` file.
By default it is set the use the local file system as the source and the local
computer to run workflows.


1. Xenon-flow configuration consists of 
    1. `sourceFileSystem`: Any filesystem supported by Xenon can be used here
    2. `ComputeResources`: A map of compute resource descriptions for Xenon
    3. Each resource has the following settings:
        1. `cwlCommand`: The command to run cwl workflows, usually cwltool
        2. `scheduler`: A Xenon scheduler
        3. `filesystem` A Xenon filesystem
        4. Both the scheduler and filesystem have to following format:
            1. `adaptor`: The name of the Xenon adaptor (for instance slurm for scheduler or sftp for filesystem)
            2. `location`: The URI for the resource
            3. `credential`: Optional credentials (if not supplied the current user and ~/.ssh/id_rsa is used)
            4. `properties`: Optional properties (usually not needed)

## 4. Start the server
The following command will install all necessary dependencies, compile Xenon-flow and run the server.
```
./gradlew bootRun
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
curl -X POST -H "Content-Type: application/json" -d '{"name": "My First Workflow","workflow": "$PWD/cwl/echo.cwl","input": {"inp": "Hello CWL Server!"}}' http://localhost:8080/jobs
```

