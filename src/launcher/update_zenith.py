import os
import subprocess
import zipfile


class UpdateError(Exception):
    pass


class RestUpdateError(UpdateError):
    pass


def git_update_check():
    try:
        print("Running git pull...")
        subprocess.run(['git', 'pull'], check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as e:
        print("Error pulling from git:")
        print(e.stderr)
    return None


def rest_update_check(config, api, asset_name, executable_name):
    latest_release_and_ver = api.get_latest_release_and_ver(config.get_mc_version())
    if not latest_release_and_ver:
        raise RestUpdateError("Failed to get latest release for channel: " + config.release_channel)
    if latest_release_and_ver[1] == config.version and os.path.isfile(config.launch_dir + executable_name):
        print("Already up to date")
        return
    rest_get_assets(config, api, asset_name, executable_name, latest_release_and_ver)


def rest_get_version(config, api, asset_name, executable_name, target_version):
    release_and_version = api.get_release_for_ver(target_version)
    if not release_and_version:
        raise RestUpdateError("Failed to get release for version: " + target_version
                              + " and channel: " + config.release_channel)
    rest_get_assets(config, api, asset_name, executable_name, release_and_version)


def rest_get_assets(config, api, asset_name, executable_name, release_and_version):
    print("Downloading version:", release_and_version[1])
    asset_id = api.get_release_asset_id(release_and_version[0], asset_name)
    if not asset_id:
        raise RestUpdateError("Failed to get executable asset ID")
    asset_data = api.download_release_asset(asset_id)
    if not asset_data:
        raise RestUpdateError("Failed to download executable asset")
    try:
        if not os.path.exists(config.launch_dir):
            os.makedirs(config.launch_dir)
        existing_files = os.listdir(config.launch_dir)
        for existing_file in existing_files:
            if existing_file == ".gitkeep":
                continue
            print("Removing existing file:", existing_file)
            os.remove(config.launch_dir + existing_file)
        with open(config.launch_dir + asset_name, "wb") as f:
            f.write(asset_data)
        if asset_name.endswith(".zip"):
            with zipfile.ZipFile(config.launch_dir + asset_name, "r") as zip_ref:
                zip_ref.extractall(config.launch_dir)
                subprocess.run(["chmod", "+x", config.launch_dir + executable_name])
            os.remove(config.launch_dir + asset_name)
        config.local_version = config.version = release_and_version[1]
    except IOError as e:
        raise RestUpdateError("Failed to write executable asset: " + str(e))


def java_update_check(config, api):
    rest_update_check(config, api, "ZenithProxy.jar", "ZenithProxy.jar")


def java_get_version(config, api, target_version):
    print("Getting version: " + target_version)
    rest_get_version(config, api, "ZenithProxy.jar", "ZenithProxy.jar", target_version)


def linux_native_update_check(config, api):
    rest_update_check(config, api, "ZenithProxy-linux-amd64.zip", "ZenithProxy")


def linux_native_get_version(config, api, target_version):
    print("Getting version: " + target_version)
    rest_get_version(config, api, "ZenithProxy-linux-amd64.zip", "ZenithProxy", target_version)


def update_zenith_exec(config, api):
    # Determine if there's a new update
    # Install new update if available
    if config.auto_update:
        try:
            if config.release_channel == "git":
                git_update_check()
            elif config.release_channel.startswith("java"):
                java_update_check(config, api)
            elif config.release_channel.startswith("linux"):
                linux_native_update_check(config, api)
        except UpdateError as e:
            print("Error performing update check:", e)
    elif config.release_channel != "git" and config.version != config.local_version:
        print("Desired version is different from local version, attempting to download version:", config.version)
        try:
            if config.release_channel.startswith("java"):
                java_get_version(config, api, config.version)
            elif config.release_channel.startswith("linux"):
                linux_native_get_version(config, api, config.version)
        except UpdateError as e:
            print("Error performing update check:", e)
