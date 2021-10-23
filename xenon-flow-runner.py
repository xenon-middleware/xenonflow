import argparse
import json
import os
import pathlib
import subprocess
import sys
import time

import requests
import yaml

SUCCESS_STATES = ['Success']
WAITING_STATES = ['Waiting', 'Running']
ERROR_STATES = [
    'Cancelled', 'SystemError', 'TemporaryFailure', 'PermanentFailure'
]


def get_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument('--version',
                        action='store_true',
                        help='get the cwltool version')

    options, remainder = parser.parse_known_args()

    if not options.version:
        parser.add_argument('--outdir')
        parser.add_argument('--quiet', action='store_true')
        parser.add_argument("cwlfile")
        parser.add_argument("jobinput", nargs='?')
        return parser.parse_args(remainder)
    else:
        return options


def wait_until_finished(created, headers, quiet):
    created_json = created.json()
    joburl = created_json['uri']

    while True:
        time.sleep(1)
        response = requests.get(joburl, headers=headers)
        if response.status_code == 200:
            response_json = response.json()
            status = response_json['state']
            if status in SUCCESS_STATES:
                output = response_json['output']
                # cwltest probably does not want this extra output
                # that gets added by xenonflow
                del output['stderr.txt']
                print(json.dumps(output, indent=4))
                return 0
            elif status in ERROR_STATES:
                if not quiet:
                    print("received status: ", status, " not supported ")
                return 33
            elif status not in WAITING_STATES:
                print("Unsupported state received from xenonflow: ", status)
                return -1
        else:
            if not quiet:
                print("Received a non 200 reply from xenonflow: ",
                      response.text)
            return -1


def main():
    args = get_arguments()

    if args.version:
        output = subprocess.run("cwltool --version",
                                shell="true",
                                capture_output="true")
        print(output.stdout.decode('utf-8'))
        return output.returncode
    else:
        quiet = args.quiet
        filespath = pathlib.Path(os.getenv('XENONFLOW_FILES', "."))
        url = os.getenv('XENONFLOW_URL', 'http://localhost:8080/jobs')
        xenonflow_api_key_name = os.getenv('XENONFLOW_API_KEY_NAME', 'api-key')
        xenonflow_api_key = os.getenv('XENONFLOW_API_KEY')
        jobpath = None
        if args.jobinput:
            jobpath = filespath / pathlib.Path(args.jobinput)

        if not xenonflow_api_key:
            raise Exception(
                "XENONFLOW_API_KEY environment variable must be set")

        cwlpath = pathlib.Path(args.cwlfile)
        jobinput = None
        if jobpath:
            with open(jobpath) as f:
                if jobpath.suffix == 'json':
                    jobinput = json.load(f)
                else:
                    jobinput = yaml.safe_load(f)

        joborder = {
            'name': cwlpath.name,
            'workflow': args.cwlfile,
        }

        if jobinput:
            joborder['input'] = jobinput

        if not quiet:
            print("Posting to xenonflow: ", joborder)

        headers = {xenonflow_api_key_name: xenonflow_api_key}
        created = requests.post(url, json=joborder, headers=headers)
        if created.status_code == 201:
            return wait_until_finished(created, headers, quiet)
        else:
            if not quiet:
                print("Could not create job: ", created.text)
            return -1


if __name__ == "__main__":
    sys.exit(main())
