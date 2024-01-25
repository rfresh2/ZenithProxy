import sys
import time

import github_api
import launcher
from launch_config import LaunchConfig, read_launch_config_file
from setup import setup_execute
from update_launcher import update_launcher_exec
from update_zenith import update_zenith_exec

config = LaunchConfig()
api = github_api.GitHubAPI(config)

# for use with relaunches just so we don't get stuck in an infinite update loop if something goes wrong
no_launcher_update = False
# to go straight to setup and exit
setup_only = False
for arg in sys.argv:
    if arg == "--no-launcher-update":
        no_launcher_update = True
    if arg == "--setup":
        setup_only = True

if setup_only:
    setup_execute()
    sys.exit(0)

try:
    while True:
        json_data = read_launch_config_file()
        if json_data is None:
            print("launch_config.json not found, running setup.")
            setup_execute()
            json_data = read_launch_config_file()
        config.load_launch_config_data(json_data)
        config.validate_launch_config()
        if no_launcher_update:
            print("Skipping launcher update check")
            no_launcher_update = False
        else:
            update_launcher_exec(config, api)
        update_zenith_exec(config, api)
        launcher.launcher_exec(config)
        print("Restarting in 3 seconds...")
        time.sleep(3)
except KeyboardInterrupt:
    sys.exit(0)
