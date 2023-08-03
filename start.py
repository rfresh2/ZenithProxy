import json
import os
import platform
import re
import subprocess
import sys
import zipfile

import requests

release_channel = "git"
version = "0"
repo_owner = "rfresh2"
repo_name = "ZenithProxy"
system = platform.system()

github_headers = {
    "User-Agent": "ZenithProxy/1.0",
    "Accept": "application/vnd.github+json",
    "Authorization": f"Bearer {os.getenv('GITHUB_TOKEN')}",
    "X-GitHub-Api-Version": "2022-11-28",
    "Connection": "close"
}


def create_default_launch_config():
    print("Creating default launch_config.json")
    write_launch_config()


def init_launch_config():
    try:
        with open('launch_config.json') as f:
            data = json.load(f)
            return data
    except FileNotFoundError:
        print("launch_config.json not found")
        create_default_launch_config()
        return None


def read_launch_config(data):
    global release_channel, version, repo_owner, repo_name
    try:
        t_channel = data['release_channel']
        t_version = data['version']
        t_repo_owner = data['repo_owner']
        t_repo_name = data['repo_name']
        release_channel = t_channel
        version = t_version
        repo_owner = t_repo_owner
        repo_name = t_repo_name
    except KeyError:
        print("Error reading launch_config.json")
        create_default_launch_config()


def write_launch_config():
    global release_channel, version, repo_owner, repo_name
    with open('launch_config.json', 'w') as f:
        f.write('''{
    "release_channel": "''' + release_channel + '''",
    "version": "''' + version + '''",
    "repo_owner": "''' + repo_owner + '''",
    "repo_name": "''' + repo_name + '''"
}''')


def git_update_check():
    global version
    os.system("git pull")
    os.system("gradlew jarBuild --no-daemon")
    try:
        output = subprocess.check_output(['git', 'rev-parse', '--short=8', 'HEAD'], stderr=subprocess.STDOUT, text=True)
        v = str(output).splitlines()[0].strip()
        if version_looks_valid(v):
            version = v
            print("Git updated to version: ", version)
        else:
            print("Invalid version string found from git:", output)
    except subprocess.CalledProcessError as e:
        print("Error:", e.output)
    return None


def version_looks_valid(ver):
    return ver == '0' or (len(ver) == 8 and re.match(r"[0-9a-f]+", ver))


def validate_launch_config():
    if not valid_release_channel(release_channel):
        print("Invalid release channel:", release_channel)
        return False
    if not version_looks_valid(version):
        print("Invalid version string:", version)
        return False
    if repo_name == "":
        print("Invalid repo name:", repo_name)
        return False
    if repo_owner == "":
        print("Invalid repo owner:", repo_owner)
        return False
    return True


def valid_release_channel(channel):
    return channel == "git" \
        or channel == "java" \
        or channel == "linux-native" \
        or channel == "prerelease-linux-native"


def get_latest_release_id(channel):
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/releases"
    response = requests.get(url, headers=github_headers)

    if response.status_code != 200:
        print("Failed to get releases")
        return None

    releases = response.json()
    latest_release = next(
        (release for release in releases if release["tag_name"].startswith(channel)),
        None
    )
    print("Latest release:", str(latest_release))
    return latest_release["id"] if latest_release else None


def get_release_asset_id(release_id, asset_name):
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/releases/{release_id}"
    response = requests.get(url, headers=github_headers)
    release_data = response.json()
    return next(
        (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
        None
    )


def download_release_asset(asset_id):
    download_headers = github_headers.copy()
    download_headers["Accept"] = "application/octet-stream"
    url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/releases/assets/{asset_id}"
    response = requests.get(url, headers=download_headers)
    asset_data = response.content
    return asset_data


def java_update_check():
    latest_release_id = get_latest_release_id(release_channel)
    version_asset_id = get_release_asset_id(latest_release_id, "version.txt")
    version_data = download_release_asset(version_asset_id)
    version_str = version_data.decode().strip()
    if not version_looks_valid(version_str):
        print("Invalid version string:", version_str)
        return
    if version_str == version:
        print("Already up to date")
        return
    print("Updating to version", version_str)
    jar_asset_id = get_release_asset_id(latest_release_id, "ZenithProxy.jar")
    jar_data = download_release_asset(jar_asset_id)
    with open("ZenithProxy.jar", "wb") as f:
        f.write(jar_data)


def linux_native_update_check():
    latest_release_id = get_latest_release_id(release_channel)
    print("Latest release ID:", str(latest_release_id))
    version_asset_id = get_release_asset_id(latest_release_id, "version.txt")
    print("Version asset ID:", str(version_asset_id))
    version_data = download_release_asset(version_asset_id)
    print(version_data)
    version_str = version_data.decode().strip()
    if not version_looks_valid(version_str):
        print("Invalid version string:", version_str)
        return
    if version_str == version:
        print("Already up to date")
        return
    print("Updating to version", version_str)
    zip_asset_id = get_release_asset_id(latest_release_id, "ZenithProxy.zip")
    zip_data = download_release_asset(zip_asset_id)
    with open("ZenithProxy.zip", "wb") as f:
        f.write(zip_data)
    with zipfile.ZipFile("ZenithProxy.zip", "r") as zip_ref:
        zip_ref.extractall(".")
    os.remove("ZenithProxy.zip")


def get_java_version():
    try:
        output = subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True)
        version_line = [line for line in output.split('\n') if "version" in line][0]
        version_match = re.search(r'"(\d+\.\d+)\.', version_line)
        if version_match:
            return float(version_match.group(1))
    except subprocess.CalledProcessError as e:
        print("Error:", e.output)
    return None


def validate_system_with_config():
    if release_channel == "git":
        # check if we have a .git directory
        if not os.path.isdir(".git"):
            print("No .git directory found. Please clone the repository.")
            return False
        return True
    elif release_channel == "java":
        java_version = get_java_version()
        if java_version is None or java_version < 17:
            print("Invalid Java version on PATH. Please install Java 17 or higher.")
            return False
        return True
    elif release_channel == "linux-native":
        return system == "Linux"
    elif release_channel == "prerelease-linux-native":
        return system == "Linux"
    else:
        return False


# Check our release channel and version

json_data = init_launch_config()
if json_data is None:
    json_data = init_launch_config()
read_launch_config(json_data)
validate_launch_config()

# Determine if there's a new update
# Install new update if available

if release_channel == "git":
    git_update_check()
elif release_channel == "java":
    java_update_check()
elif release_channel == "linux-native":
    linux_native_update_check()
elif release_channel == "prerelease-linux-native":
    linux_native_update_check()

write_launch_config()

# Launch application

if release_channel == "git":
    toolchain_command = ""
    jar_command = ""
    common_script = """\
-server -XX:MaxRAMPercentage=30 -XX:MinRAMPercentage=30 \
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch \
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8 -Dio.netty.allocator.maxOrder=9 -Dio.netty.eventLoopThreads=2 """
    if system == 'Windows':
        toolchain_command = "call build\\java_toolchain.bat"
        jar_command = "-jar build\\libs\\ZenithProxy.jar"
    else:
        toolchain_command = "./build/java_toolchain"
        jar_command = "-jar build/libs/ZenithProxy.jar"
    full_script = f"{toolchain_command} {common_script} {jar_command}"
    try:
        subprocess.run(full_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        print("Error launching application:", e)
elif release_channel == "java":
    toolchain_command = ""
    jar_command = ""
    common_script = """\
-server -Xmx300m \
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch \
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8 -Dio.netty.allocator.maxOrder=9 -Dio.netty.eventLoopThreads=2 """
    if system == 'Windows':
        toolchain_command = "call java"
        jar_command = "-jar ZenithProxy.jar"
    else:
        toolchain_command = "java"
        jar_command = "-jar ZenithProxy.jar"
    full_script = f"{toolchain_command} {common_script} {jar_command}"
    try:
        subprocess.run(full_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        print("Error launching application:", e)
elif release_channel == "linux-native" or release_channel == "prerelease-linux-native":
    try:
        subprocess.run("./ZenithProxy "
                       "-Xmx150m -Djava.util.concurrent.ForkJoinPool.common.parallelism=8 "
                       "-Dio.netty.allocator.maxOrder=9 -Dio.netty.eventLoopThreads=2",
                       shell=True, check=True)
    except subprocess.CalledProcessError as e:
        print("Error launching application:", e)
else:
    print("Invalid release channel:", release_channel)
    sys.exit(1)
