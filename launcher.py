import http.client
import json
import os
import platform
import re
import subprocess
import sys
import urllib.parse
import zipfile

auto_update = True
auto_update_launcher = True
release_channel = "git"
version = "0.0.0"
local_version = "0.0.0"
repo_owner = "rfresh2"
repo_name = "ZenithProxy"
launch_dir = "launcher/"
custom_jvm_args = None
system = platform.system()

default_java_args = """\
-Xmx300m \
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch \
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8 -Dio.netty.allocator.maxOrder=9 -Dio.netty.eventLoopThreads=2"""

default_linux_args = """\
-Xmx150m \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8 \
-Dio.netty.allocator.maxOrder=9 -Dio.netty.eventLoopThreads=2"""

github_headers = {
    "User-Agent": "ZenithProxy/1.0",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "Connection": "close"
}

if os.getenv('GITHUB_TOKEN') is not None:
    github_headers["Authorization"] = f"Bearer {os.getenv('GITHUB_TOKEN')}"


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
    except json.decoder.JSONDecodeError:
        print("launch_config.json is invalid")
        create_default_launch_config()
        return None


def read_launch_config(data):
    global release_channel, version, local_version, repo_owner, repo_name, auto_update, auto_update_launcher, custom_jvm_args
    if data is None:
        print("No data to read from launch_config.json")
        return
    auto_update = data.get('auto_update', auto_update)
    auto_update_launcher = data.get('auto_update_launcher', auto_update_launcher)
    release_channel = data.get('release_channel', release_channel)
    version = data.get('version', version)
    local_version = data.get('local_version', local_version)
    repo_owner = data.get('repo_owner', repo_owner)
    repo_name = data.get('repo_name', repo_name)
    custom_jvm_args = data.get('custom_jvm_args', custom_jvm_args)
    if custom_jvm_args is not None and custom_jvm_args != "":
        print("Using custom JVM args:", custom_jvm_args)


def write_launch_config():
    global release_channel, version, repo_owner, repo_name
    output = {
        "auto_update": auto_update,
        "auto_update_launcher": auto_update_launcher,
        "release_channel": release_channel,
        "version": version,
        "local_version": version,
        "repo_owner": repo_owner,
        "repo_name": repo_name,
    }
    if (custom_jvm_args is not None) and (custom_jvm_args != ""):
        output["custom_jvm_args"] = custom_jvm_args
    with open('launch_config.json.tmp', 'w') as f:
        f.write(json.dumps(output, indent=2))
    os.replace('launch_config.json.tmp', 'launch_config.json')


def git_update_check():
    try:
        print("Running git pull...")
        subprocess.run(['git', 'pull'], check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as e:
        print("Error pulling from git:")
        print(e.stderr)
    return None


def git_read_version():
    global version, local_version
    try:
        output = subprocess.check_output(['git', 'rev-parse', '--short=8', 'HEAD'], stderr=subprocess.STDOUT, text=True)
        v = str(output).splitlines()[0].strip()
        if len(v) == 8:
            version = v
            local_version = v
            print("Git commit:", version)
        else:
            print("Invalid version string found from git:", output)
    except subprocess.CalledProcessError as e:
        print("Error reading local git version:")
        print(e.stderr)


def git_build():
    if system == "Windows":
        os.system(".\\gradlew jarBuild --no-daemon")
    else:
        os.system("./gradlew jarBuild --no-daemon")


def version_looks_valid(ver):
    return re.match(r"[0-9]+\.[0-9]+\.[0-9]+", ver) or (len(ver) == 8 and re.match(r"[0-9a-f]+", ver))


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
    return channel.startswith("git") \
        or channel.startswith("java") \
        or channel.startswith("linux")


def get_latest_release_and_ver(channel):
    latest_release = None
    url = f"/repos/{repo_owner}/{repo_name}/releases?{urllib.parse.urlencode({'per_page': 100})}"
    try:
        connection = http.client.HTTPSConnection("api.github.com")
        connection.request("GET", url, headers=github_headers)
        response = connection.getresponse()
        if response.status == 200:
            releases = json.loads(response.read())
            for release in releases:
                if release["draft"]:
                    continue
                if release["tag_name"].endswith("+" + channel):
                    if latest_release is None or release["published_at"] > latest_release["published_at"]:
                        latest_release = release
        else:
            print("Failed to get releases:", response.status, response.reason)
    except Exception as e:
        print("Failed to get releases:", e)
    finally:
        connection.close()
    return (latest_release["id"], latest_release["tag_name"]) if latest_release else None


def get_release_for_ver(target_version):
    found_version = False
    page = 1
    url = f"/repos/{repo_owner}/{repo_name}/releases?{urllib.parse.urlencode({'per_page': 100, 'page': page})}"
    try:
        while not found_version and page < 10:
            connection = http.client.HTTPSConnection("api.github.com")
            connection.request("GET", url, headers=github_headers)
            response = connection.getresponse()
            if response.status == 200:
                releases = json.loads(response.read())
                for release in releases:
                    if release["draft"]:
                        continue
                    if release["tag_name"] == target_version:
                        return release["id"], release["tag_name"]
            else:
                print("Failed to get releases:", response.status, response.reason)
                break
            connection.close()
            page += 1
    except Exception as e:
        print("Failed to get release for version:", target_version, e)
    finally:
        connection.close()
    return None


def get_release_asset_id(release_id, asset_name):
    url = f"/repos/{repo_owner}/{repo_name}/releases/{release_id}"
    try:
        connection = http.client.HTTPSConnection("api.github.com")
        connection.request("GET", url, headers=github_headers)
        response = connection.getresponse()
        if response.status == 200:
            release_data = json.loads(response.read())
            asset_id = next(
                (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
                None
            )
            return asset_id
        else:
            print("Failed to get release asset ID:", response.status, response.reason)
            return None
    except Exception as e:
        print("Failed to get release asset ID:", e)
        return None
    finally:
        connection.close()


def download_release_asset(asset_id):
    url = f"/repos/{repo_owner}/{repo_name}/releases/assets/{asset_id}"
    try:
        connection = http.client.HTTPSConnection("api.github.com")
        download_headers = github_headers.copy()
        download_headers["Accept"] = "application/octet-stream"
        connection.request("GET", url, headers=download_headers)
        response = connection.getresponse()

        # Follow redirects
        while response.status // 100 == 3:
            redirect_location = response.getheader('Location')
            connection.close()

            # Parse the redirect URL to extract the new host
            redirect_url = urllib.parse.urlparse(redirect_location)
            redirect_host = redirect_url.netloc

            # Reopen connection to the new host
            connection = http.client.HTTPSConnection(redirect_host)
            connection.request("GET", redirect_location, headers=download_headers)
            response = connection.getresponse()

        if response.status == 200:
            asset_data = response.read()
            return asset_data
        else:
            print("Failed to download asset:", response.status, response.reason)
            return None
    except Exception as e:
        print("Failed to download asset:", e)
        return None
    finally:
        connection.close()


class UpdateError(Exception):
    pass


class RestUpdateError(UpdateError):
    pass


def rest_update_check(asset_name, executable_name):
    global version
    latest_release_and_ver = get_latest_release_and_ver(release_channel)
    if not latest_release_and_ver:
        raise RestUpdateError("Failed to get latest release for channel: " + release_channel)
    if latest_release_and_ver[1] == version and os.path.isfile(launch_dir + executable_name):
        print("Already up to date")
        return
    rest_get_assets(asset_name, executable_name, latest_release_and_ver)


def rest_get_version(asset_name, executable_name, target_version):
    release_and_version = get_release_for_ver(target_version)
    if not release_and_version:
        raise RestUpdateError("Failed to get release for version: " + target_version
                              + " and channel: " + release_channel)
    rest_get_assets(asset_name, executable_name, release_and_version)


def rest_get_assets(asset_name, executable_name, release_and_version):
    global version, local_version
    print("Downloading version:", release_and_version[1])
    asset_id = get_release_asset_id(release_and_version[0], asset_name)
    if not asset_id:
        raise RestUpdateError("Failed to get executable asset ID")
    asset_data = download_release_asset(asset_id)
    if not asset_data:
        raise RestUpdateError("Failed to download executable asset")
    try:
        if not os.path.exists(launch_dir):
            os.makedirs(launch_dir)
        existing_files = os.listdir(launch_dir)
        for existing_file in existing_files:
            if existing_file == ".gitkeep":
                continue
            print("Removing existing file:", existing_file)
            os.remove(launch_dir + existing_file)
        with open(launch_dir + asset_name, "wb") as f:
            f.write(asset_data)
        if asset_name.endswith(".zip"):
            with zipfile.ZipFile(launch_dir + asset_name, "r") as zip_ref:
                zip_ref.extractall(launch_dir)
                subprocess.run(["chmod", "+x", launch_dir + executable_name])
            os.remove(launch_dir + asset_name)
        local_version = version = release_and_version[1]
    except IOError as e:
        raise RestUpdateError("Failed to write executable asset: " + str(e))


def java_update_check():
    rest_update_check("ZenithProxy.jar", "ZenithProxy.jar")


def java_get_version(target_version):
    print("Getting version: " + target_version)
    rest_get_version("ZenithProxy.jar", "ZenithProxy.jar", target_version)


def linux_native_update_check():
    rest_update_check("ZenithProxy.zip", "ZenithProxy")


def linux_native_get_version(target_version):
    print("Getting version: " + target_version)
    rest_get_version("ZenithProxy.zip", "ZenithProxy", target_version)


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
    elif release_channel.startswith("java"):
        java_version = get_java_version()
        if java_version is None or java_version < 17:
            print("Invalid Java version on PATH. Found: '" + java_version + "'Please install Java 17 or higher.")
            return False
        return True
    elif release_channel.startswith("linux"):
        return system == "Linux"
    else:
        return False


def critical_error(message):
    print("CRITICAL: " + message)
    sys.exit(69)


# Check our release channel and version

json_data = init_launch_config()
if json_data is None:
    json_data = init_launch_config()
read_launch_config(json_data)
validate_launch_config()
if not validate_system_with_config():
    critical_error("Invalid system for release channel: " + release_channel)

# Determine if there's a new update
# Install new update if available
if auto_update:
    try:
        if release_channel == "git":
            git_update_check()
        elif release_channel.startswith("java"):
            java_update_check()
        elif release_channel.startswith("linux"):
            linux_native_update_check()
    except UpdateError as e:
        print("Error performing update check:", e)
elif release_channel != "git" and version != local_version:
    print("Desired version is different from local version, attempting to download version:", version)
    try:
        if release_channel.startswith("java"):
            java_get_version(version)
        elif release_channel.startswith("linux"):
            linux_native_get_version(version)
    except UpdateError as e:
        print("Error performing update check:", e)

if release_channel == "git":
    git_read_version()

write_launch_config()

if version == "0.0.0" or local_version == "0.0.0":
    print("CRITICAL: Invalid version found:'", version, "'")
    print("Enable `auto_updater` or specify a valid version in launch_config.json.")
    exit(69)

# Launch application

if release_channel == "git":
    git_build()
    toolchain_command = ""
    jar_command = ""
    if custom_jvm_args is not None and custom_jvm_args != "":
        jvm_args = custom_jvm_args
    else:
        jvm_args = default_java_args
    if system == 'Windows':
        toolchain_command = ".\\build\\java_toolchain.bat"
        jar_command = "-jar build\\libs\\ZenithProxy.jar"
    else:
        toolchain_command = "./build/java_toolchain"
        jar_command = "-jar build/libs/ZenithProxy.jar"
    run_script = f"{toolchain_command} {jvm_args} {jar_command}"
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        print("Error launching application:", e)
elif release_channel.startswith("java"):
    if not os.path.isfile(launch_dir + "ZenithProxy.jar"):
        critical_error("ZenithProxy.jar not found")
    toolchain_command = ""
    jar_command = ""
    if custom_jvm_args is not None and custom_jvm_args != "":
        jvm_args = custom_jvm_args
    else:
        jvm_args = default_java_args
    if system == 'Windows':
        toolchain_command = "call java"
        jar_command = "-jar " + launch_dir.replace("/", "\\") + "ZenithProxy.jar"
    else:
        toolchain_command = "java"
        jar_command = "-jar " + launch_dir + "ZenithProxy.jar"
    run_script = f"{toolchain_command} {jvm_args} {jar_command}"
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:" + str(e))
elif release_channel.startswith("linux"):
    if system != "Linux":
        critical_error("Linux release channel is not supported on current system: " + system)
    if not os.path.isfile(launch_dir + "ZenithProxy"):
        critical_error("ZenithProxy executable not found")
    if custom_jvm_args is not None and custom_jvm_args != "":
        jvm_args = custom_jvm_args
    else:
        jvm_args = default_linux_args
    run_script = f"./{launch_dir}ZenithProxy {jvm_args}"
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:" + str(e))
else:
    critical_error("Invalid release channel:" + release_channel)
