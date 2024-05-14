import hashlib
import io
import os
import subprocess
import sys
import tempfile

import launch_platform
import zip_fixed
from launch_platform import OperatingSystem

launcher_tag = "launcher-v3"
hashes_file_name = "hashes.txt"


def update_launcher_exec(config, api):
    if not config.auto_update_launcher:
        return
    print("Checking for launcher update...")
    try:
        is_pyinstaller = launch_platform.is_pyinstaller_bundle()
        os_platform = launch_platform.get_platform_os()
        os_arch = launch_platform.get_platform_arch()
        launcher_asset_file_name = get_launcher_asset_zip_file_name(is_pyinstaller, os_platform, os_arch)
        current_executable_name = get_current_launcher_executable_name(is_pyinstaller)
        expected_executable_name = get_expected_launcher_executable_name(is_pyinstaller)
        hashes_list = get_launcher_hashes(api)
        if not os.path.isfile(current_executable_name):
            raise LauncherUpdateError("Launcher executable not found, skipping launcher update:", current_executable_name)
        current_launcher_sha1 = compute_sha1(current_executable_name)
        if current_launcher_sha1 in hashes_list:
            print("Already on the latest launcher")
            return
        print("Found new launcher, current version:", current_launcher_sha1)
        launcher_asset_id = api.get_release_tag_asset_id(launcher_tag, launcher_asset_file_name)
        if launcher_asset_id is None:
            raise LauncherUpdateError("Failed to get launcher asset ID:", launcher_asset_file_name)
        launcher_asset_bytes = api.download_asset(launcher_asset_id)
        if launcher_asset_bytes is None:
            raise LauncherUpdateError("Failed to download launcher asset:", launcher_asset_file_name)
        if not os.path.exists("launcher"):
            os.makedirs("launcher")
        for file_name in os.listdir("launcher"):
            if file_name.startswith("launch"):
                os.remove("launcher/" + file_name)
        with zip_fixed.ZipFileWithPermissions(io.BytesIO(launcher_asset_bytes)) as zip_file:
            zip_file.extractall("launcher")
        new_executable_path = "launcher/" + expected_executable_name
        if not os.path.isfile(new_executable_path):
            raise LauncherUpdateError("Failed to extract launcher executable:", new_executable_path)
        new_launcher_sha1 = compute_sha1(new_executable_path)
        print("New launcher version:", new_launcher_sha1)
        # Preserve current launcher executable name if its changed
        replace_launcher_executable(os_platform, current_executable_name, new_executable_path, current_launcher_sha1)
        if is_pyinstaller:
            relaunch_executable(os_platform, current_executable_name)
        else:
            replace_extra_python_launcher_files(os_platform, current_launcher_sha1)
            relaunch_python(os_platform, current_executable_name)
    except Exception as e:
        print("Error during launcher updater check, skipping update:", e)


def get_launcher_asset_zip_file_name(is_pyinstaller, os_platform, os_arch):
    if is_pyinstaller:
        return f"ZenithProxy-launcher-{os_platform.value}-{os_arch.value}.zip"
    else:
        return "ZenithProxy-launcher-python.zip"


# The executable name we're currently running
def get_current_launcher_executable_name(is_pyinstaller):
    if is_pyinstaller:
        return os.path.basename(sys.executable)
    else:
        return "launcher-py.zip"  # could be anything really, we don't have a way to determine this correctly


# The executable name we're expecting from github
def get_expected_launcher_executable_name(is_pyinstaller):
    if is_pyinstaller:
        if launch_platform.get_platform_os() == OperatingSystem.WINDOWS:
            return "launch.exe"
        return "launch"
    else:
        return "launcher-py.zip"


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
        raise LauncherUpdateError("Failed to get launcher hashes asset ID:", hashes_file_name)
    hashes_asset_bytes = api.download_asset(hashes_asset_id)
    if hashes_asset_bytes is None:
        raise LauncherUpdateError("Failed to download launcher hashes asset:", hashes_file_name)
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
        raise LauncherUpdateError("Failed to parse launcher hashes file:", hashes_file_name)
    return hashes_list


def relaunch_executable(os_platform, executable_name):
    print("Relaunching...")
    if os_platform == OperatingSystem.WINDOWS:
        subprocess.run([executable_name, "--no-launcher-update"])
    else:
        os.execl(executable_name, "--no-launcher-update")


def relaunch_python(os_platform, executable_name):
    if os_platform == OperatingSystem.WINDOWS:
        relaunch_executable(os_platform, "launch.bat")
    else:
        relaunch_executable(executable_name, "launch.sh")


def replace_launcher_executable(os_platform, exec_name, new_exec_name, current_sha1):
    print("Replacing launcher files...")
    if os_platform == OperatingSystem.WINDOWS:
        # on windows, we can't replace the executable while it's running
        # so we're moving the files around and then launching a subprocess
        # not ideal as we don't clean this process until everything gets closed, but it seems to work
        os.rename(exec_name, tempfile.gettempdir() + "/launcher-exec-" + current_sha1 + ".old")
        os.rename(new_exec_name, exec_name)
    else:
        os.replace(new_exec_name, exec_name)


def replace_extra_python_launcher_files(os_platform, current_sha1):
    os.replace("launcher/requirements.txt", "requirements.txt")
    # todo: handle the case where users change the script's name
    os.replace("launcher/launch.sh", "launch.sh")
    if os_platform == OperatingSystem.WINDOWS:
        os.rename("launch.bat", tempfile.gettempdir() + "/launch-" + current_sha1 + ".bat.old")
        os.rename("launcher/launch.bat", "launch.bat")
    else:
        os.replace("launcher/launch.bat", "launch.bat")


class LauncherUpdateError(Exception):
    pass
