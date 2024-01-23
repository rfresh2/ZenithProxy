import hashlib
import jdk
import json
import os
import platform
import re
import requests
import subprocess
import sys
import time
import zipfile

auto_update = True
auto_update_launcher = True
release_channel = "java.1.20.1"
version = "0.0.0"
local_version = "0.0.0"
repo_owner = "rfresh2"
repo_name = "ZenithProxy"
launch_dir = "launcher/"
custom_jvm_args = None
system = platform.system()
launcher_tag = "launcher"

default_java_args = """\
-Xmx300m \
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch \
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dio.netty.allocator.maxOrder=9 \
-Dio.netty.leakDetection.level=disabled"""

default_linux_args = """\
-Xmx200m \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=2 \
-Dio.netty.allocator.maxOrder=9 \
-Dio.netty.leakDetection.level=disabled"""


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
        return None
    except json.decoder.JSONDecodeError:
        print("launch_config.json is invalid")
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


def get_github_api_base_url():
    if repo_owner == "rfresh2" and repo_name == "ZenithProxy":
        return "github.2b2t.vc"
    else:
        return "api.github.com"


def get_github_base_headers():
    return {
        "User-Agent": "ZenithProxy/" + version,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "Connection": "close"
    }


def get_latest_release_and_ver(channel):
    latest_release = None
    url = f"https://{get_github_api_base_url()}/repos/{repo_owner}/{repo_name}/releases"
    params = {'per_page': 100}
    try:
        response = requests.get(url, headers=get_github_base_headers(), params=params)
        if response.status_code == 200:
            releases = response.json()
            for release in releases:
                if release["draft"]:
                    continue
                if release["tag_name"].endswith("+" + channel):
                    if latest_release is None or release["published_at"] > latest_release["published_at"]:
                        latest_release = release
        else:
            print("Failed to get releases:", response.status_code, response.reason)
    except Exception as e:
        print("Failed to get releases:", e)
    return (latest_release["id"], latest_release["tag_name"]) if latest_release else None


def get_release_for_ver(target_version):
    found_version = False
    page = 1
    url = f"https://{get_github_api_base_url()}/repos/{repo_owner}/{repo_name}/releases"
    try:
        while not found_version and page < 10:
            response = requests.get(url, headers=get_github_base_headers(), params={'per_page': 100, 'page': page})
            if response.status_code == 200:
                releases = response.json()
                for release in releases:
                    if release["draft"]:
                        continue
                    if release["tag_name"] == target_version:
                        return release["id"], release["tag_name"]
            else:
                print("Failed to get releases:", response.status_code, response.reason)
                break
            page += 1
    except Exception as e:
        print("Failed to get release for version:", target_version, e)


def get_release_asset_id(release_id, asset_name):
    url = f"https://{get_github_api_base_url()}/repos/{repo_owner}/{repo_name}/releases/{release_id}"
    try:
        response = requests.get(url, headers=get_github_base_headers())
        if response.status_code == 200:
            release_data = response.json()
            asset_id = next(
                (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
                None
            )
            return asset_id
        else:
            print("Failed to get release asset ID:", response.status_code, response.reason)
            return None
    except Exception as e:
        print("Failed to get release asset ID:", e)
        return None


def get_release_tag_asset_id(release_id, asset_name):
    url = f"https://{get_github_api_base_url()}/repos/{repo_owner}/{repo_name}/releases/tags/{release_id}"
    try:
        response = requests.get(url, headers=get_github_base_headers())
        if response.status_code == 200:
            release_data = response.json()
            asset_id = next(
                (asset["id"] for asset in release_data["assets"] if asset["name"] == asset_name),
                None
            )
            return asset_id
        else:
            print("Failed to get release asset ID:", response.status_code, response.reason)
            return None
    except Exception as e:
        print("Failed to get release asset ID:", e)
        return None


def download_release_asset(asset_id):
    url = f"https://{get_github_api_base_url()}/repos/{repo_owner}/{repo_name}/releases/assets/{asset_id}"
    download_headers = get_github_base_headers()
    download_headers["Accept"] = "application/octet-stream"
    try:
        response = requests.get(url, headers=download_headers, allow_redirects=True)
        if response.status_code == 200:
            asset_data = response.content
            return asset_data
        else:
            print("Failed to download asset:", response.status_code, response.reason)
            return None
    except Exception as e:
        print("Failed to download asset:", e)
        return None


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
        version_match = re.search(r'"(\d+(\.\d+)?)', version_line)
        if version_match:
            version = version_match.group(1)
            return float(version) if '.' in version else int(version)
    except (subprocess.CalledProcessError, OSError) as e:
        critical_error("Error checking Java version, do you have Java installed?\n" + str(e))
    return None


def validate_linux_cpu_flags():
    x86_64_v3_flags = ["avx", "avx2", "bmi1", "bmi2", "fma", "sse4_1", "sse4_2", "ssse3"]
    try:
        output = subprocess.check_output(['cat', '/proc/cpuinfo'], stderr=subprocess.STDOUT, text=True)
        flags = []
        for line in output.splitlines():
            if line.startswith("flags"):
                flags = line.split(": ")[1].split(" ")
                break
        for flag in x86_64_v3_flags:
            if flag not in flags:
                print("Unsupported CPU. "
                      + "Use the Java release channel instead. Re-run setup.py to change the release channel. "
                      + "\nFlag not found: " + flag)
                return False
        return True
    except Exception as e:
        print("Error checking CPU flags:", e)
        return False


def validate_linux_glibc_version():
    try:
        output = subprocess.check_output(['ldd', '--version'], stderr=subprocess.STDOUT, text=True)
        # ldd (Ubuntu GLIBC 2.35-0ubuntu3.4) 2.35
        # get the version from the last word of the first line
        version = output.splitlines()[0].split(" ")[-1]
        version = version.split(".")
        if int(version[0]) != 2:
            print("Unsupported OS for linux release channel. "
                  + "\nglibc version too low: " + ".".join(version))
            return False
        if int(version[1]) < 31:
            print("Unsupported OS for linux release channel. "
                  + "\nglibc version too low: " + ".".join(version))
            return False
        return True
    except Exception as e:
        print("Error checking GLIBC version.")
        return False


def validate_system_with_config():
    if release_channel == "git":
        # check if we have a .git directory
        if not os.path.isdir(".git"):
            print("No .git directory found. Please clone the repository.")
            return False
        return True
    elif release_channel.startswith("java"):
        java_version = get_java_version()
        min_java_version = 21 if version.startswith("2") else 17
        if java_version is None or java_version < min_java_version:
            print("Invalid Java version on PATH. Found: '" + str(java_version) + "' Java " + str(
                min_java_version) + " or higher required.")
            return False
        return True
    elif release_channel.startswith("linux"):
        # ignoring this for now
        valid_flags = validate_linux_cpu_flags()
        valid_glibc = validate_linux_glibc_version()
        return system == "Linux"  # and valid_flags and valid_glibc
    else:
        return False


def install_java():
    print("Installing Java...")
    if system == "Windows":
        jdk.install('21')
    else:
        jdk.install('21')
    print("Java installed successfully!")


def validate_linux_system():
    return system == "Linux" and validate_linux_cpu_flags() and validate_linux_glibc_version()


def critical_error(message):
    print("CRITICAL: " + message)
    sys.exit(69)


def setup_exec():
    if validate_linux_system():  # otherwise we will always select java
        while True:
            print("Select a ZenithProxy platform: (1/2)")
            print("1. java")
            print("2. linux")
            i1 = input("> ")
            if i1 == "1":
                release_channel = "java"
                break
            elif i1 == "2":
                release_channel = "linux"
                break
            else:
                print("Invalid input. Enter 1 or 2")
    else:
        print("Auto-selecting the java release channel based on current system")
        release_channel = "java"

    while True:
        print("Select a Minecraft version: (1/2)")
        print("1. 1.20.1")
        print("2. 1.20.4")
        i1 = input("> ")
        if i1 == "1":
            minecraft_version = "1.20.1"
            break
        elif i1 == "2":
            minecraft_version = "1.20.4"
            break
        else:
            print("Invalid input. Enter 1 or 2")

    launch_config = {
        "auto_update": True,
        "auto_update_launcher": True,
        "release_channel": release_channel + "." + minecraft_version,
        "version": "0.0.0",
        "local_version": "0.0.0",
        "repo_owner": "rfresh2",
        "repo_name": "ZenithProxy"
    }

    with open("launch_config.json", "w") as f:
        f.write(json.dumps(launch_config, indent=2))
        print("launch_config written successfully")

    if os.path.exists("config.json"):
        while True:
            print("config.json already exists, overwrite and continue anyway? (y/n)")
            i1 = input("> ").lower()
            if i1 == "n":
                print("Setup complete!")
                print("Run './launch.sh' (Unix) or '.\\launch.bat` (Windows) to start ZenithProxy!")
                exit(0)
            elif i1 == "y":
                break
            else:
                print("Invalid input. Enter y or n")

    while True:
        print("Select authentication method: (1/2)")
        print("1. Device Code (Recommended)")
        print("2. Username and Password")

        i1 = input("> ")
        if i1 == "1":
            auth_method = "device_code"
            break
        elif i1 == "2":
            auth_method = "msa"
            break
        else:
            print("Invalid input. Enter 1 or 2")

    if auth_method == "msa":
        while True:
            print("Enter your Microsoft account email/username:")
            username = input("> ")
            if "@" in username:
                break
        while True:
            print("Enter your Microsoft account password:")
            password = input("> ")
            if len(password) > 0:
                break

    while True:
        print("Input port the proxy will listen on (e.g. 25565):")
        port = input("> ")
        try:
            port = int(port)
            if port < 1 or port > 65535:
                raise ValueError
            break
        except ValueError:
            print("Invalid port number. Must be between 1 and 65535")

    while True:
        print("Input the IP address players should connect to. This can be a domain name or an IP address.")
        print(
            "If you are unsure, leave this blank and the proxy will use the IP address of the machine it is running on.")
        print("If you are using a domain name, make sure you have DNS records set up (see README.md)")
        ip = input("> ")
        if ip == "":
            response = requests.get("https://api.ipify.org")
            if response.status_code == 200:
                ip = response.content.decode()
                break
            else:
                print("Failed to get IP address:", response.status_code, response.reason)
        else:
            break

    while True:
        print("Enable Discord bot? (y/n)")
        i2 = input("> ")
        if i2 == "y":
            discord_bot = True
            break
        elif i2 == "n":
            discord_bot = False
            break
        else:
            print("Invalid input. Enter y or n")

    if discord_bot:
        while True:
            print("See README.md for instructions on how to set up a Discord bot")
            print("Enter your Discord bot token:")
            discord_bot_token = input("> ")
            if len(discord_bot_token) > 50:
                break
            else:
                print("Invalid token")
        while True:
            print("Enter the channel ID the proxy will be managed in:")
            discord_channel_id = input("> ")
            try:
                discord_channel_id = int(discord_channel_id)
                if discord_channel_id < 1000000000 or discord_channel_id > 9999999999999999999:
                    raise ValueError
                break
            except ValueError:
                print("Invalid ID")
        while True:
            print("Enter the role ID of the role that will be able to manage the proxy's whitelist:")
            discord_admin_role_id = input("> ")
            try:
                discord_admin_role_id = int(discord_admin_role_id)
                if discord_admin_role_id < 1000000000 or discord_admin_role_id > 9999999999999999999:
                    raise ValueError
                break
            except ValueError:
                print("Invalid ID")
        while True:
            print("Enter the channel ID the proxy will relay chat messages to:")
            discord_chat_relay_channel = input("> ")
            try:
                discord_chat_relay_channel = int(discord_chat_relay_channel)
                if discord_chat_relay_channel < 1000000000 or discord_chat_relay_channel > 9999999999999999999:
                    raise ValueError
                if discord_chat_relay_channel == discord_channel_id:
                    print("Chat relay channel cannot be the same as the management channel")
                    continue
                break
            except ValueError:
                print("Invalid ID")

    # Write config.json
    config = {}

    if auth_method == "msa":
        config["authentication"] = {
            "accountType": "msa",
            "email": username,
            "password": password
        }

    ipPattern = re.compile(r"^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$")
    proxyAddress = ip + ":" + str(port) if ipPattern.match(ip) else ip

    config["server"] = {
        "bind": {
            "port": port,
        },
        "proxyIP": proxyAddress
    }

    if discord_bot:
        config["discord"] = {
            "enable": True,
            "token": discord_bot_token,
            "channelId": discord_channel_id,
            "accountOwnerRoleId": discord_admin_role_id,
            "chatRelay": {
                "enable": True,
                "channelId": discord_chat_relay_channel
            }
        }

    with open("config.json", "w") as f:
        f.write(json.dumps(config, indent=2))
        print("config.json written successfully!")

    print("Setup complete!")
    # print("Run './launch.sh' (Unix) or '.\\launch.bat` (Windows) to start ZenithProxy!")


def update_launcher_exec():
    if not auto_update_launcher:
        return
    print("Checking for launcher update...")
    launcherAssetID = get_release_tag_asset_id(launcher_tag, "launcher.py")
    if launcherAssetID is None:
        exit(1)
    launcherAssetBytes = download_release_asset(launcherAssetID)
    if launcherAssetBytes is None:
        exit(1)
    contents = launcherAssetBytes.decode()
    if len(contents) < 100:
        print("Failed to download launcher.py")
        exit(1)

    if os.path.exists("launcher.py"):
        with open("launcher.py", "r", newline='') as f:
            current = f.read()
            currentHash = hashlib.sha1(current.encode()).hexdigest()
            newHash = hashlib.sha1(contents.encode()).hexdigest()
            if currentHash != newHash:
                print("Updating launcher.py to " + newHash)
                do_update = True
    else:
        print("No launcher.py found. Downloading from GitHub.")

    if do_update:
        with open("launcher.py.tmp", "w", newline='') as f:
            f.write(contents)
        os.replace("launcher.py.tmp", "launcher.py")
        print("Updated launcher.py successfully!")
        print("Relaunching launcher...")
        # todo: handle native launcher executable update downloads and stuff above
        if sys.argv[0].endswith(".py"):
            os.execl(sys.executable, sys.executable, *sys.argv)
        else:
            os.execl(sys.argv[0], *sys.argv)


def launcher_exec():
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
        if release_channel == "git":
            critical_error("Invalid version found for git release channel:" + version
                           + "\nRe-run setup.py and select another release channel.")
        critical_error("Invalid version found:'" + version + "'"
                       + "\nEnable `auto_updater` or specify a valid version in launch_config.json.")

    if not validate_system_with_config():
        critical_error("Invalid system for release channel: " + release_channel)

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


def main():
    while True:
        json_data = init_launch_config()
        if json_data is None:
            print("launch_config.json not found, running setup.")
            setup_exec()
            json_data = init_launch_config()
        read_launch_config(json_data)
        validate_launch_config()
        update_launcher_exec()
        launcher_exec()
        print("Restarting in 3 seconds...")
        time.sleep(3)


if __name__ == "__main__":
    main()

# todo: We could still split out our py source files and compile them with PyInstaller into a single executable
#   Would probably reduce the length and complexity of this script by a lot
#   This is more possible with PyInstaller as we don't have to depend on the user not updating all the launcher files
#   as we had before
