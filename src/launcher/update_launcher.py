import hashlib
import io
import os
import subprocess
import sys
import tempfile
import zipfile

import launch_platform

launcher_tag = "launcher-v3"


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
        return "__main__.py"
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
    launcher_asset_id = api.get_release_tag_asset_id(launcher_tag, launcher_asset_file_name)
    if launcher_asset_id is None:
        print("Failed to get launcher asset ID:", launcher_asset_file_name)
        return
    launcher_asset_bytes = api.download_release_asset(launcher_asset_id)
    if launcher_asset_bytes is None:
        print("Failed to download launcher.py asset:", launcher_asset_file_name)
        return
    with zipfile.ZipFile(io.BytesIO(launcher_asset_bytes)) as zip_file:
        zip_file.extractall("launcher")
    tmp_executable_path = "launcher/" + executable_name
    do_update = False
    if os.path.exists(tmp_executable_path):
        tmp_sha1 = hashlib.sha1()
        cur_sha1 = hashlib.sha1()
        with open(tmp_executable_path, "rb") as f:
            while True:
                data = f.read(65536)
                if not data:
                    break
                tmp_sha1.update(data)
        with open(executable_name, "rb") as f:
            while True:
                data = f.read(65536)
                if not data:
                    break
                cur_sha1.update(data)
        if cur_sha1.hexdigest() != tmp_sha1.hexdigest():
            print("Current launcher:", cur_sha1.hexdigest())
            print("New launcher:", tmp_sha1.hexdigest())
            print("Updating launcher to:", tmp_sha1.hexdigest())
            do_update = True

    if do_update:
        print("Relaunching...")
        if os_platform == "windows":
            # on windows, we can't replace the executable while it's running
            # so we're moving the files around and then launching a subprocess
            # not ideal as we don't clean this process until everything gets closed, but it seems to work
            os.rename(executable_name, tempfile.gettempdir() + "/launcher-" + cur_sha1.hexdigest() + ".old")
            os.rename(tmp_executable_path, executable_name)
            subprocess.run([executable_name])
        else:
            os.replace(tmp_executable_path, executable_name)
        if sys.argv[0].endswith(".py"):
            os.execl(sys.executable, sys.executable, *sys.argv)
        else:
            os.execl(sys.argv[0], *sys.argv)
