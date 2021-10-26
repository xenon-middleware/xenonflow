# Version 1.0.1
- Adding cwl compliance check and fixed a bunch of related bugs
- Reduced logging output
- Fixed various bugs
- Fixed a number of crashes related to the tests ran by the compliance test.
- Fixing output bug when parameter has # in its id
- Fixing file and directory array location wrangling
- Fixed directory tests and staging
- Fixing directory path staging for cwl check: nr85 directory_input_docker

# Version 1.0
- Various bugfixes
- Fixed uri's supplied by xenonflow when runing behind a proxy server
- Updated admin interface to connect to the backend using its current location
- You can now add xenonflow_jobid or xenonflow_jobname as input to a workflow to be run
  xenonflow will then supply these values automatically.
- Added XENONFLOW_FILES environment variable for use in the config.yml

# Version 1.0-rc1
- Split SourceFileSystem setting into sourceFileSystem for inputs and cwlFileSystem for workflow storage
- Added check on job submission whether the referenced workflow exists on the cwlFileSystem
- Added /workflows api that supplies a list of available workflows in the cwlFileSystem
- Upgrade admin interface to Angular 11

# Version 0.4-process
- Upgrade to Xenon 3

# Version 0.3-alpha
- Fixed pending jobs throwing an error

# Version 0.2-alpha
Output file serving and cwl parsing updates
- Output files are now served if they are stored on the local filesystem
  * This requires a new config block with a parameter "hosted" set to true:
  targetFileSystem:
   adaptor: file
   location: /home/bweel/Documents/projects/xenon-flow/output/
   hosted: true

- Output file paths are now set to this location if used

- Parsing of cwl has been updated to support Maps in addition to arrays for inputs and outputs

# Version 0.1-alpha
- First pre-release
- Updated to Xenon 2.1.0