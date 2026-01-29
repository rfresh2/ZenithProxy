import os
import ssl
import sys
import time

if sys.version_info[0] < 3 or sys.version_info[1] < 6:
    # f strings added in 3.6
    # it's possible other things will break pre 3.10, not sure tho
    print("CRITICAL: Python 3.10 or higher is required. Current version: " + str(sys.version_info[0]) + "." + str(sys.version_info[1]))
    sys.exit(1)

if sys.version_info[1] < 10:
    print("WARNING: Python 3.10 or higher is required. Current version: " + str(sys.version_info[0]) + "." + str(sys.version_info[1]))

from log import info, critical_error

info("ZenithProxy Launcher Initializing...")
info(f"Python Version: {sys.version_info[0]}.{sys.version_info[1]}.{sys.version_info[2]}")

import certifi
import github_api
import launch_platform
from launch_config import LaunchConfig, read_launch_config_file
from launcher import launcher_exec
from setup import setup_execute, rescue_invalid_system, setup_unattended
from update_launcher import update_launcher_exec
from update_zenith import update_zenith_exec

ssl._create_default_https_context = lambda: ssl.create_default_context(cafile=certifi.where())

config = LaunchConfig()
api = github_api.GitHubAPI(config)

# Certain platforms like mac seem to not have the correct cwd set correctly when double clicking the executable
if launch_platform.is_pyinstaller_bundle():
    current_cwd = os.getcwd()
    executable_path = launch_platform.executable_path()
    expected_cwd = os.path.dirname(executable_path)
    if current_cwd != expected_cwd:
        os.chdir(expected_cwd)

# for use with relaunches just so we don't get stuck in an infinite update loop if something goes wrong
no_launcher_update = False
# to go straight to setup and exit
setup_only = False
# Initialize configs from env variables
unattended = False
for arg in sys.argv:
    if arg == "--no-launcher-update":
        no_launcher_update = True
    if arg == "--setup":
        setup_only = True
    if arg == "--unattended":
        unattended = True

if setup_only:
    setup_execute(config)
    sys.exit(0)

if unattended:
    setup_unattended(config)

try:
    while True:
        json_data = read_launch_config_file()
        if json_data is None:
            if unattended:
                critical_error("launch_config.json not found and unattended setup is enabled")
            info("Running setup...")
            setup_execute(config)
            continue
        config.load_launch_config_data(json_data)
        if not config.validate_launch_config():
            if unattended:
                critical_error("launch_config.json has invalid values and unattended setup is enabled")
            info("launch_config.json has invalid values, running setup...")
            setup_execute(config)
            continue
        info("Loaded launch_config.json successfully")
        if not launch_platform.validate_system_with_config(config):
            if unattended:
                critical_error("System validation failed and unattended setup is enabled")
            rescue_invalid_system(config)
            continue
        if no_launcher_update:
            no_launcher_update = False
        else:
            update_launcher_exec(config, api)
        update_zenith_exec(config, api)
        launcher_exec(config)
        info("Restarting...")
        time.sleep(3)
except KeyboardInterrupt:
    sys.exit(0)
