cwlVersion: v1.0
class: Workflow

requirements:
    - class: ScatterFeatureRequirement

inputs:
  - id: inp_dirs
    type: Directory[]
  - id: newnames
    type: string[]

steps:
    copydirs:
        in:
            inp: inp_dirs
            newname: newnames
        out:
            - out
        run: copy-dir.cwl 
        scatter:
            - inp
            - newname
        scatterMethod: dotproduct

outputs:
  - id: out
    type: Directory[]
    outputSource:
      copydirs/out