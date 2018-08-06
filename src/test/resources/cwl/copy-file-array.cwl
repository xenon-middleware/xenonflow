cwlVersion: v1.0
class: Workflow

requirements:
    - class: ScatterFeatureRequirement

inputs:
  - id: inp_files
    type: File[]
    inputBinding: {}
  - id: newnames
    type: string[]
    inputBinding: {}

steps:
    copyfiles:
        in:
            inp: inp_files
            newname: newnames
        out:
            - out
        run: copy.cwl 
        scatter: ["inp", "newname"]
        scatterMethod: dotproduct

outputs:
  - id: out
    type: File[]
    outputSource:
      copyfiles/out