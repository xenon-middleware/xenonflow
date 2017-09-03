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
git clone https://github.com/NLeSC/xenon-cwl.git
```

## 3. Configure the server
Configuration of the server is done by editing the `config/config.yml` file.
By default it is set the use the local file system as the source and the local
computer to run workflows.


1. Xenon-flow configuration consists of 
    1. sourceFileSystem: Any filesystem supported by Xenon can be used here
    2. 
Upload the workflow and any input files and directories to a webdav server

## Starting the server
The following command will install all necessary dependencies, compile Xenon-flow and run the server.
```
./gradlew bootRun
```

