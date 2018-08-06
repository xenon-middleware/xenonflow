cwlVersion: v1.0
class: CommandLineTool

inputs:
  - id: inp
    type: File
    inputBinding: {}
  - id: newname
    type: string
    inputBinding: {}

outputs:
  - id: out
    type: string

baseCommand: this_command_does_not_exist