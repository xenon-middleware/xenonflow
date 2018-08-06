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
    type: File
    outputBinding:
      glob: $(inputs.newname)

baseCommand: cp