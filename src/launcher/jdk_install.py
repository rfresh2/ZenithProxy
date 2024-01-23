# TODO: Migrate this into the launcher script

import os
import sys

import jdk

from launch_platform import get_java_version

_USER_DIR = os.path.expanduser("~")
_JDK_DIR = os.path.join(_USER_DIR, ".jdk")


def install_java():
    print("Installing Java...")
    jdk.install('21', path=_JDK_DIR)
    print("Java installed successfully!")


def locate_java():
    # first check if the java on the path is java 21+
    # if so, use that
    path_java_version = get_java_version()
    if path_java_version and path_java_version >= 21:
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


def get_java_executable():
    java_path = locate_java()
    if not java_path:
        install_java()
        java_path = locate_java()
        if not java_path:
            print("Failed to install Java.")
            sys.exit(69)
    print("Java path: " + java_path)
    return java_path

# can be called like this
# java = get_java_executable()
