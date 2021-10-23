#!/usr/bin/env bash
cloneorpull() {
        if test -d $1 ; then
                (cd $1 && git pull)
        else
                git clone $2
        fi
}
venv() {
        if ! test -d $1 ; then
                virtualenv $1
        fi
        . $1/bin/activate
}

cloneorpull common-workflow-language https://github.com/common-workflow-language/common-workflow-language.git
venv cwltest-venv
(. cwltest-venv/bin/activate && PIP_DOWNLOAD_CACHE=/var/lib/jenkins/pypi-cache/ pip3 install -U pip wheel)
(. cwltest-venv/bin/activate && PIP_DOWNLOAD_CACHE=/var/lib/jenkins/pypi-cache/ pip3 install setuptools==18.5 "cwltest>=1.0.20160825151655")
(. cwltest-venv/bin/activate && PIP_DOWNLOAD_CACHE=/var/lib/jenkins/pypi-cache/ pip3 install pyyaml requests docker node)

#export XENONFLOW_FILES=common-workflow-language/v1.0
#./gradlew bootRun > xenonflow-output.log 2>&1 &

(. cwltest-venv/bin/activate && cd common-workflow-language && ./run_test.sh -j10 --junit-xml=../../result.xml --junit-verbose RUNNER=../xenon-flow-runner.sh)
