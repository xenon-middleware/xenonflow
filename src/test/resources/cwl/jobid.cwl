cwlVersion: v1.0
class: CommandLineTool
inputs:
  - id: xenonflow_jobid
    type: string
    inputBinding:
      position: 1
  - id: xenonflow_jobname
    type: string
    inputBinding:
      position: 2

outputs:
  - id: out
    type: string
    outputBinding:
      glob: out.txt
      loadContents: true
      outputEval: $(self[0].contents)

baseCommand: echo
stdout: out.txt