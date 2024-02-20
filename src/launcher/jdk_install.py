import os
import re
import subprocess

import jdk

import launch_platform

_USER_DIR = os.path.expanduser("~")
_JDK_DIR = os.path.join(_USER_DIR, ".jdk")


def get_java_version_from_subprocess(java_path):
    try:
        output = subprocess.check_output([java_path, "-version"], stderr=subprocess.STDOUT, text=True)
        version_line = [line for line in output.split("\n") if "version" in line][0]
        version_match = re.search(r'"(\d+(\.\d+)?)', version_line)
        if version_match:
            version = version_match.group(1)
            return float(version) if "." in version else int(version)
    except Exception:
        # No Java installation found on PATH
        # fall through
        return None


def get_path_java_version():
    return get_java_version_from_subprocess("java")


def get_java_home_version():
    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        return None
    return get_java_version_from_subprocess(os.path.join(java_home, "bin", "java"))


def install_java():
    print("Installing Java to:", _JDK_DIR)
    install_dir = jdk.install("21", path=_JDK_DIR)
    print("Java installed successfully to:", install_dir)


def search_for_java_in_dir(search_path, min_version=21):
    output = []
    java_file_extension = ".exe" if launch_platform.get_platform_os() == launch_platform.OperatingSystem.WINDOWS else ""
    if not os.path.exists(search_path) or not os.path.isdir(search_path):
        return output
    for folder in os.listdir(search_path):
        if folder.__contains__(str(min_version)):
            # check if this has bin/java(.exe)
            # its optional on windows to include the .exe in the subprocess call, but we need to check it if its present
            java_path = os.path.join(search_path, folder, "bin", "java" + java_file_extension)
            if os.path.exists(java_path):
                output.append(java_path)
    return output


def find_first_java_in_dir(java_path_list, min_version):
    for java_path in java_path_list:
        version = get_java_version_from_subprocess(java_path)
        if version and version >= min_version:
            return java_path
    return None


def locate_java(min_version=21):
    path_java_version = get_path_java_version()
    if path_java_version and path_java_version >= min_version:
        return "java"
    java_home_version = get_java_home_version()
    if java_home_version and java_home_version >= min_version:
        return os.path.join(os.environ.get("JAVA_HOME"), "bin", "java")
    jdk_dir_java = find_first_java_in_dir(search_for_java_in_dir(_JDK_DIR, min_version), min_version)
    if jdk_dir_java:
        return jdk_dir_java
    jdk_dir_java = find_first_java_in_dir(search_for_java_in_dir(os.path.join(_USER_DIR, ".jdks"), min_version), min_version)
    if jdk_dir_java:
        return jdk_dir_java
    return None


def java_install_prompt():
    while True:
        print("Automatically install Java? (y/n)")
        i1 = input("> ")
        if i1 == "y":
            install_java()
            break
        elif i1 == "n":
            print("Please install Java 21+ and try again.")
            break
        else:
            print("Invalid input. Enter y or n")


def get_java_executable(min_version=21):
    java_path = locate_java(min_version)
    if not java_path:
        java_install_prompt()
        java_path = locate_java(min_version)
        if not java_path:
            print("Failed to install Java.")
            return None
    return java_path
