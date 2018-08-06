cwlVersion: v1.0
class: CommandLineTool

inputs:
  - id: inp
    type: Directory
    inputBinding: {}
  - id: newname
    type: string
    inputBinding: {}

outputs:
  - id: out
    type: Directory
    outputBinding:
      glob: $(inputs.newname)

baseCommand: cp
arguments: [-R]