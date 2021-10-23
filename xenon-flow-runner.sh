#!/usr/bin/env bash
SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

. $SCRIPT_DIR/cwltest-venv/bin/activate && python3 $SCRIPT_DIR/xenon-flow-runner.py $@