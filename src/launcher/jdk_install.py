import os
import re
import subprocess

import jdk

_USER_DIR = os.path.expanduser("~")
_JDK_DIR = os.path.join(_USER_DIR, ".jdk")


def get_path_java_version():
    try:
        output = subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT, text=True)
        version_line = [line for line in output.split('\n') if "version" in line][0]
        version_match = re.search(r'"(\d+(\.\d+)?)', version_line)
        if version_match:
            version = version_match.group(1)
            return float(version) if '.' in version else int(version)
    except (subprocess.CalledProcessError, OSError) as e:
        # No Java installation found on PATH
        # fall through
        return None


def install_java():
    print("Installing Java to:", _JDK_DIR)
    install_dir = jdk.install('21', path=_JDK_DIR)
    print("Java installed successfully to:", install_dir)


def locate_java(min_version=21):
    # first check if the java on the path is java 21+
    # if so, use that
    path_java_version = get_path_java_version()
    if path_java_version and path_java_version >= min_version:
        return 'java'

    if not os.path.exists(_JDK_DIR):
        return None
    # find the jdk/bin/java executable for java 21+ including java 21, 22, 23, etc.
    # the next folder down will be named like 'jdk-21.0.2+13'
    # match on anything that starts with jdk-2
    for folder in os.listdir(_JDK_DIR):
        if folder.startswith('jdk-2'):
            return os.path.join(_JDK_DIR, folder, 'bin', 'java')
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

