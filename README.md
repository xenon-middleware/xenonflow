# xenon-cwl-runner

Run CWL workflows using Xenon

# Building

Install Java first, then run:
```
./gradlew shadowJar
```

# Run

To echo something on the DAS-5 cluster (set the `das5` alias in `.ssh/config`), run:
```
java -jar build/libs/xenon-cwl-runner-all.jar --xenon-host slurm://das5 echo.cwl --inp "Hello World"
```

