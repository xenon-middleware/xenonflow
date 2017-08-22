cwlVersion: cwl:v1.0
class: Workflow
inputs:
  directory_in:
    type: Directory
  filename:
    type: File
  dirname:
    type: string

outputs:
  output:
     type: string
     outputSource: echo-file/out

steps: 
  copy-dir:
    run: copy-dir.cwl
    in:
      inp: directory_in
      newname: dirname
    out:
      - out

  echo-file:
    run: echo-file.cwl
    in:
      inp: $(copy-dir)
    out:
      - out