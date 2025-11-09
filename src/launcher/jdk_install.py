import os
import re
import subprocess
from enum import Enum

import jdk

import launch_platform
from utils import critical_error

_USER_DIR = os.path.expanduser("~")
_JDK_DIR = os.path.join(_USER_DIR, ".jdk")
_JDKS_DIR = os.path.join(_USER_DIR, ".jdks")
_JRE_DIR = os.path.join(_USER_DIR, ".jre")


class JavaInstallType(Enum):
    USER_PROMPT = 1
    AUTO_INSTALL = 2
    NO_INSTALL = 3


def get_java_executable(min_version=21, install_type=JavaInstallType.AUTO_INSTALL):
    java_path = _locate_java(min_version)
    if not java_path:
        if install_type == JavaInstallType.USER_PROMPT:
            _java_install_prompt()
            java_path = _locate_java(min_version)
        elif install_type == JavaInstallType.AUTO_INSTALL:
            _install_java()
            java_path = _locate_java(min_version)
        elif install_type == JavaInstallType.NO_INSTALL:
            critical_error("Java not found and both auto install and user prompt disabled.")
        if not java_path:
            print("Failed to install Java.")
            return None
    return java_path


def get_java_version_from_subprocess(java_path):
    try:
        output = subprocess.check_output([java_path, "-version"], stderr=subprocess.STDOUT, text=True)
        version_line = [line for line in output.split("\n") if "version" in line][0]
        version_match = re.search(r'"(\d+(\.\d+)?)', version_line)
        if version_match:
            version = version_match.group(1)
            return float(version) if "." in version else int(version)
    except Exception:
        return None


def _get_path_java_version():
    return get_java_version_from_subprocess("java")


def _get_java_home_version():
    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        return None
    java_path = os.path.join(java_home, "bin", "java" + _java_exe_extension())
    return get_java_version_from_subprocess(java_path)


def _locate_java_from_env(env_var, min_version):
    java_home = os.environ.get(env_var)
    if not java_home:
        return None
    java_path = os.path.join(java_home, "bin", "java" + _java_exe_extension())
    version = get_java_version_from_subprocess(java_path)
    if version and version >= min_version:
        return java_path
    return None


def _install_java():
    print("Installing Java to:", _JDK_DIR)
    install_dir = jdk.install("25", path=_JDK_DIR)
    print("Java installed successfully to:", install_dir)


def _java_exe_extension():
    return ".exe" if launch_platform.get_platform_os() == launch_platform.OperatingSystem.WINDOWS else ""


def _search_for_java_in_dir(search_path):
    output = []
    if not os.path.exists(search_path) or not os.path.isdir(search_path):
        return output
    # check if this has bin/java(.exe)
    for folder in os.listdir(search_path):
        java_path = os.path.join(search_path, folder, "bin", "java" + _java_exe_extension())
        if os.path.exists(java_path):
            output.append(java_path)
    return output


def _find_first_java_in_dir(java_path_list, min_version):
    for java_path in java_path_list:
        version = get_java_version_from_subprocess(java_path)
        if version and version >= min_version:
            return java_path
    return None


def _locate_in_dir(search_dir, min_version):
    return _find_first_java_in_dir(_search_for_java_in_dir(search_dir), min_version)


def _locate_java(min_version):
    path_java_version = _get_path_java_version()
    if path_java_version and path_java_version >= min_version:
        return "java"

    java_home_version = _locate_java_from_env("JAVA_HOME", min_version)
    if java_home_version:
        return java_home_version

    jdk_dir_java = _locate_in_dir(_JDK_DIR, min_version)
    if jdk_dir_java:
        return jdk_dir_java

    jdks_dir_java = _locate_in_dir(_JDKS_DIR, min_version)
    if jdks_dir_java:
        return jdks_dir_java

    jre_dir_java = _locate_in_dir(_JRE_DIR, min_version)
    if jre_dir_java:
        return jre_dir_java
    return None


def _java_install_prompt():
    while True:
        print("Automatically install Java? (y/n)")
        i1 = input("> ")
        if i1 == "y":
            _install_java()
            break
        elif i1 == "n":
            print("Please install Java 21+ and try again.")
            break
        else:
            print("Invalid input. Enter y or n")
