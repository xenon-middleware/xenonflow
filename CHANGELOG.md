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