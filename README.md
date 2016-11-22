# xenon-cwl-runner

#Note: This runner is both a prototype and incomplete. We do plan to finish it at some point in the near future.

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

