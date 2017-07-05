# xenon-cwl

#Note: This runner is both a prototype and incomplete.

Run CWL workflows using Xenon. Possibly through a REST api.

# Building

This repo needs the as-of-yet-unreleased Xenon 2.0. Quick way to get Xenon:

    git clone https://github.com/NLeSC/Xenon.git
    cd Xenon
    git checkout APIchange
    //FIX JAVADOC BUILD IN GRADLE
    ./gradlew publishToMavenLocal

Install Java first, then run:
```
./gradlew shadowJar
```
