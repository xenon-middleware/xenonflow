cwlVersion: v1.0
class: CommandLineTool

inputs:
  - id: testArray
    type:
        type: array
        items: boolean

outputs:
  - id: out
    type: File
    outputBinding:
      glob: $(inputs.newname)

baseCommand: cp