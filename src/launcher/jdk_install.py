import os
import re
import subprocess
from dataclasses import dataclass
from enum import Enum
from typing import Optional, List

import jdk

import launch_platform
from log import info, error, warn, critical_error, debug
from version import Version

_USER_DIR = os.path.expanduser("~")
_JDK_DIR = os.path.join(_USER_DIR, ".jdk")
_JDKS_DIR = os.path.join(_USER_DIR, ".jdks")
_JRE_DIR = os.path.join(_USER_DIR, ".jre")

class JavaInstallType(Enum):
    USER_PROMPT = 1
    AUTO_INSTALL = 2
    NO_INSTALL = 3


@dataclass
class JavaInstance:
    path: str
    version: Version


def get_java_instance(min_version: Version, install_type: JavaInstallType = JavaInstallType.AUTO_INSTALL) -> Optional[JavaInstance]:
    java_instance = _locate_java(min_version)
    if not java_instance:
        if install_type == JavaInstallType.USER_PROMPT:
            _java_install_prompt()
            java_instance = _locate_java(min_version)
        elif install_type == JavaInstallType.AUTO_INSTALL:
            _install_java()
            java_instance = _locate_java(min_version)
        elif install_type == JavaInstallType.NO_INSTALL:
            critical_error("Java not found and both auto install and user prompt disabled.")
        if not java_instance:
            warn("Failed to install Java.")
            return None
    return java_instance


def _get_java_version_from_subprocess(java_path: str) -> Optional[Version]:
    try:
        output = subprocess.check_output([java_path, "-version"], stderr=subprocess.STDOUT, text=True)
        version_line = [line for line in output.split("\n") if "version" in line][0]
        version_match = re.search(r'"(\d+(\.\d+){0,2})(\S)*"', version_line)
        if version_match:
            version_str = version_match.group(1)
            version = Version.from_short_str(version_str)
            return version
    except:
        return None


def _locate_path_java(min_version: Version) -> Optional[JavaInstance]:
    version = _get_java_version_from_subprocess("java")
    if version and version >= min_version:
        return JavaInstance("java", version)
    return None


def _locate_java_from_env(env_var: str, min_version: Version) -> Optional[JavaInstance]:
    java_home = os.environ.get(env_var)
    if not java_home:
        return None
    java_path = os.path.join(java_home, "bin", "java" + _java_exe_extension())
    version = _get_java_version_from_subprocess(java_path)
    if version and version >= min_version:
        return JavaInstance(java_path, version)
    return None


def _install_java():
    info(f"Installing Java to: {_JDK_DIR}")

    install_os = jdk.OS
    # default detector doesn't even attempt to detect alpine
    if launch_platform.get_platform_os() == launch_platform.OperatingSystem.ALPINE:
        debug("Installing java for alpine")
        install_os = jdk.OperatingSystem.ALPINE_LINUX

    install_dir = jdk.install("25", path=_JDK_DIR, vendor="Adoptium", operating_system=install_os)
    info(f"Java installed successfully to: {install_dir}")


def _java_exe_extension() -> str:
    return ".exe" if launch_platform.get_platform_os() == launch_platform.OperatingSystem.WINDOWS else ""


def _search_for_java_in_dir(search_path: str) -> List[str]:
    output = []
    if not os.path.exists(search_path) or not os.path.isdir(search_path):
        return output
    # check if this has bin/java(.exe)
    for folder in os.listdir(search_path):
        java_path = os.path.join(search_path, folder, "bin", "java" + _java_exe_extension())
        if os.path.exists(java_path):
            output.append(java_path)
    return output


def _find_latest_java_in_dir(java_path_list: List[str], min_version: Version) -> Optional[JavaInstance]:
    path_result = None
    latest = Version("0.0.0")
    for java_path in java_path_list:
        version = _get_java_version_from_subprocess(java_path)
        if version and version >= min_version and version > latest:
            path_result = java_path
            latest = version
    return JavaInstance(path_result, latest) if path_result else None


def _locate_in_dir(search_dir, min_version) -> Optional[JavaInstance]:
    return _find_latest_java_in_dir(_search_for_java_in_dir(search_dir), min_version)


def _locate_java(min_version: Version) -> Optional[JavaInstance]:
    # prefer path and env
    path_java_instance = _locate_path_java(min_version)
    if path_java_instance:
        return path_java_instance

    java_home_instance = _locate_java_from_env("JAVA_HOME", min_version)
    if java_home_instance:
        return java_home_instance

    # otherwise, search possible directories for highest java version
    jdk_dir_java = _locate_in_dir(_JDK_DIR, min_version)
    jdks_dir_java = _locate_in_dir(_JDKS_DIR, min_version)
    jre_dir_java = _locate_in_dir(_JRE_DIR, min_version)

    if not jdk_dir_java and not jdks_dir_java and not jre_dir_java:
        return None

    # return highest version instance of dir instances (if one exists for each)
    return max(jdk_dir_java, jdks_dir_java, jre_dir_java, key=lambda x: x.version if x else Version("0.0.0"))


def _java_install_prompt():
    while True:
        info("Automatically install Java? (y/n)")
        i1 = input("> ")
        if i1 == "y":
            _install_java()
            break
        elif i1 == "n":
            error("Please install Java 21+ and try again.")
            break
        else:
            error("Invalid input. Enter y or n")
