cwlVersion: v1.0
class: CommandLineTool
inputs:
  - id: inp
    type: File
    inputBinding: {}

outputs:
  - id: out
    type: string
    outputBinding:
      glob: out.txt
      loadContents: true
      outputEval: $(self[0].contents)

baseCommand: cat
stdout: out.txt