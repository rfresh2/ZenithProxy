import hashlib
import io
import os
import subprocess
import tempfile
import zipfile

import launch_platform

launcher_tag = "launcher-v3"
hashes_file_name = "hashes.txt"


def get_launcher_asset_file_name(is_pyinstaller, os_platform, os_arch):
    if not is_pyinstaller:
        return "ZenithProxy-launcher-python.zip"
    if os_platform == "windows":
        return "ZenithProxy-launcher-windows-amd64.zip"
    if os_platform == "linux":
        if os_arch == "amd64":
            return "ZenithProxy-launcher-linux-amd64.zip"
        elif os_arch == "aarch64":
            return "ZenithProxy-launcher-linux-aarch64.zip"
    if os_platform == "macos":
        return "ZenithProxy-launcher-macos-amd64.zip"
    return None


def get_launcher_executable_name(is_pyinstaller, os_platform, os_arch):
    if not is_pyinstaller:
        return "launcher-py.zip"
    if os_platform == "windows":
        return "launcher.exe"
    if os_platform == "linux":
        if os_arch == "amd64":
            return "launcher"
        elif os_arch == "aarch64":
            return "launcher"
    if os_platform == "macos":
        return "launcher"
    return None


def compute_sha1(file_path):
    sha1 = hashlib.sha1()
    with open(file_path, "rb") as f:
        while True:
            data = f.read(65536)
            if not data:
                break
            sha1.update(data)
    return sha1.hexdigest()


def get_launcher_hashes(api):
    hashes_asset_id = api.get_release_tag_asset_id(launcher_tag, hashes_file_name)
    if hashes_asset_id is None:
        print("Failed to get launcher hashes asset ID:", hashes_file_name)
        return None
    hashes_asset_bytes = api.download_release_asset(hashes_asset_id)
    if hashes_asset_bytes is None:
        print("Failed to download launcher hashes asset:", hashes_file_name)
        return None
    hashes_file_string = hashes_asset_bytes.decode("utf-8")
    hashes_file_lines = hashes_file_string.splitlines()
    hashes_list = []
    for line in hashes_file_lines:
        if line.startswith("#"):
            continue
        if line.strip() == "":
            continue
        hashes_list.append(line)
    if len(hashes_list) == 0:
        print("Failed to parse launcher hashes file:", hashes_file_name)
        return None
    return hashes_list


def relaunch(is_pyinstaller, os_platform, executable_name, new_executable_path, current_launcher_sha1):
    print("Relaunching...")
    if os_platform == "windows":
        # on windows, we can't replace the executable while it's running
        # so we're moving the files around and then launching a subprocess
        # not ideal as we don't clean this process until everything gets closed, but it seems to work
        os.rename(executable_name, tempfile.gettempdir() + "/launcher-" + current_launcher_sha1 + ".old")
        os.rename(new_executable_path, executable_name)
        if not is_pyinstaller:
            os.rename("launcher/launcher-python.sh", "launcher-python.sh")
            os.rename("launcher-python.bat", tempfile.gettempdir() + "/launcher-python-" + current_launcher_sha1 + ".bat.old")
            os.rename("launcher/launcher-python.bat", "launcher-python.bat")
            subprocess.run(["launcher-python.bat", "--no-launcher-update"])
        else:
            subprocess.run([executable_name, "--no-launcher-update"])
    else:
        os.replace(new_executable_path, executable_name)
        if not is_pyinstaller:
            os.replace("launcher/launcher-python.sh", "launcher-python.sh")
            os.replace("launcher/launcher-python.bat", "launcher-python.bat")
    if not is_pyinstaller:
        os.execl("launcher-python.sh", "--no-launcher-update")
    else:
        os.execl(executable_name, "--no-launcher-update")


def update_launcher_exec(config, api):
    if not config.auto_update_launcher:
        return
    print("Checking for launcher update...")
    is_pyinstaller = launch_platform.is_pyinstaller_bundle()
    os_platform = launch_platform.get_platform_os()
    os_arch = launch_platform.get_platform_arch()
    launcher_asset_file_name = get_launcher_asset_file_name(is_pyinstaller, os_platform, os_arch)
    executable_name = get_launcher_executable_name(is_pyinstaller, os_platform, os_arch)
    if executable_name is None:
        print("Unable to identify which launcher executable to update, skipping launcher update.")
        return
    if launcher_asset_file_name is None:
        print("Unable to identify which launcher asset to download, skipping launcher update.")
        return
    hashes_list = get_launcher_hashes(api)
    if hashes_list is None:
        print("Failed to get launcher hashes, skipping launcher update.")
        return
    if not os.path.isfile(executable_name):
        print("Launcher executable not found, skipping launcher update:", executable_name)
        return
    current_launcher_sha1 = compute_sha1(executable_name)
    if current_launcher_sha1 in hashes_list:
        print("Launcher is up to date:", current_launcher_sha1)
        return
    print("Found new launcher, current version:", current_launcher_sha1)
    launcher_asset_id = api.get_release_tag_asset_id(launcher_tag, launcher_asset_file_name)
    if launcher_asset_id is None:
        print("Failed to get launcher asset ID:", launcher_asset_file_name)
        return
    launcher_asset_bytes = api.download_release_asset(launcher_asset_id)
    if launcher_asset_bytes is None:
        print("Failed to download launcher asset:", launcher_asset_file_name)
        return
    for file_name in os.listdir("launcher"):
        if file_name.startswith("launcher"):
            os.remove("launcher/" + file_name)
    with zipfile.ZipFile(io.BytesIO(launcher_asset_bytes)) as zip_file:
        zip_file.extractall("launcher")
    new_executable_path = "launcher/" + executable_name
    if not os.path.isfile(new_executable_path):
        print("Failed to extract launcher executable:", executable_name)
        return
    new_launcher_sha1 = compute_sha1(new_executable_path)
    print("New launcher version:", new_launcher_sha1)
    if not is_pyinstaller:
        os.replace("launcher/requirements.txt", "requirements.txt")
    relaunch(is_pyinstaller, os_platform, executable_name, new_executable_path, current_launcher_sha1)
