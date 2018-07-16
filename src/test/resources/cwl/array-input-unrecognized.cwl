cwlVersion: v1.0
class: CommandLineTool

inputs:
  - id: testArray
    type:
        type: array
        items:
          - double
          - int

outputs:
  - id: out
    type: File
    outputBinding:
      glob: $(inputs.newname)

baseCommand: cp