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

try:
    while True:
        json_data = read_launch_config_file()
        if json_data is None:
            print("launch_config.json not found, running setup.")
            setup_execute()
            json_data = read_launch_config_file()
        config.load_launch_config_data(json_data)
        config.validate_launch_config()
        update_launcher_exec(config, api)
        update_zenith_exec(config, api)
        launcher.launcher_exec(config)
        print("Restarting in 3 seconds...")
        time.sleep(3)
except KeyboardInterrupt:
    sys.exit(0)
