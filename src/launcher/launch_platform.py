import os
import platform
import socket
import subprocess
import sys
from enum import Enum
from typing import Optional

import requests

from launch_config import LaunchConfig
from log import error, warn, debug, exception
from version import Version


def validate_linux_cpu_flags() -> bool:
    x86_64_v3_flags = ["avx", "avx2", "bmi1", "bmi2", "fma", "sse4_1", "sse4_2", "ssse3"]
    try:
        output = subprocess.check_output(["cat", "/proc/cpuinfo"], stderr=subprocess.STDOUT, text=True)
        flags = []
        for line in output.splitlines():
            if line.startswith("flags"):
                flags = line.split(": ")[1].split(" ")
                break
        for flag in x86_64_v3_flags:
            if flag not in flags:
                warn(
                    "Unsupported CPU. "
                    + "Use the Java release channel instead. Re-run setup to change the release channel. "
                    + "\nFlag not found: "
                    + flag
                )
                return False
        return True
    except:
        exception("Error checking CPU flags")
        return False


def validate_linux_glibc_version(config: LaunchConfig) -> bool:
    try:
        mc_version = config.get_mc_version()
        old_versions = ["1.12.2", "1.20.1", "1.20.4", "1.20.6", "1.21.0"]
        if mc_version in old_versions:
            glibc_minor_version_min = 31
        else:
            glibc_minor_version_min = 35
        debug(f"glibc_minor_version_min: {glibc_minor_version_min}")
        output = subprocess.run(["ldd", "--version"], stderr=subprocess.STDOUT, stdout=subprocess.PIPE, text=True)
        output = output.stdout.lower()
        debug(f"ldd output: \n{output}")
        # ldd (Ubuntu GLIBC 2.35-0ubuntu3.4) 2.35
        # get the version from the last word of the first line
        version = output.splitlines()[0].split(" ")[-1]
        version = version.split(".")
        debug(f"detected glibc version: {version}")
        if int(version[0]) != 2 or int(version[1]) < glibc_minor_version_min:
            warn(
                "Unsupported OS for linux release channel.\nglibc version too low: "
                + str(int(version[0])) + "." + str(int(version[1]))
                + "\nMin glibc version needed: 2."
                + str(glibc_minor_version_min)
            )
            return False
        return True
    except:
        exception("Error checking GLIBC version")
        return False


def validate_linux_system(config: LaunchConfig) -> bool:
    try:
        return (
            get_platform_os() == OperatingSystem.LINUX
            and get_platform_arch() == CpuArch.AMD64
            and validate_linux_cpu_flags()
            and validate_linux_glibc_version(config)
        )
    except Exception:
        return False


def validate_java_system(config: LaunchConfig, install_type) -> bool:
    java_version = min_java_version(config)
    import jdk_install
    java_executable = jdk_install.get_java_executable(java_version, install_type=install_type)
    if java_executable is None:
        warn(f"Java >={java_version} not found.")
        return False
    return True


def min_java_version(config: LaunchConfig) -> int:
    if config.version.startswith("1"):
        return 17
    mc_version = Version(config.get_mc_version())
    if mc_version < Version("1.21.11"):
        return 21
    return 25


def validate_git_system() -> bool:
    return os.path.isdir(".git")


def validate_system_with_config(config: LaunchConfig) -> bool:
    if config.release_channel == "git":
        return validate_git_system()
    elif config.release_channel.startswith("java"):
        import jdk_install
        return validate_java_system(config, jdk_install.JavaInstallType.AUTO_INSTALL)
    elif config.release_channel.startswith("linux"):
        return validate_linux_system(config)
    else:
        return False


def is_pyinstaller_bundle() -> bool:
    return getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS")


def is_nuitka_bundle() -> bool:
    return "__compiled__" in globals()


def is_windows_python_bundle() -> bool:
    return os.path.exists("python/python.exe")


def executable_path() -> str:
    if is_nuitka_bundle():
        return sys.argv[0]
    return sys.executable


class PlatformError(Exception):
    pass


class OperatingSystem(Enum):
    WINDOWS = "windows"
    LINUX = "linux"
    ALPINE = "alpine"
    MACOS = "macos"


class LinuxLibC(Enum):
    GLIBC = "glibc"
    MUSL = "musl"


def get_linux_libc() -> Optional[LinuxLibC]:
    try:
        debug(f"> ldd --version")
        output = subprocess.run(["ldd", "--version"], stderr=subprocess.STDOUT, stdout=subprocess.PIPE, text=True)
        output = output.stdout.lower()
        debug(f"ldd output: \n{output}")
        if "glibc" in output or "gnu libc" in output:
            debug("Detected glibc")
            return LinuxLibC.GLIBC
        elif "musl" in output:
            debug("Detected musl")
            return LinuxLibC.MUSL
    except:
        exception("Error checking linux libc type")
    return None


def get_platform_os() -> OperatingSystem:
    if platform.system() == "Windows":
        return OperatingSystem.WINDOWS
    elif platform.system() == "Linux":
        if get_linux_libc() == LinuxLibC.MUSL:
            debug("Detected alpine")
            return OperatingSystem.ALPINE
        debug("Detected linux")
        return OperatingSystem.LINUX
    elif platform.system() == "Darwin":
        return OperatingSystem.MACOS
    else:
        raise PlatformError("Unsupported OS: " + platform.system())


class CpuArch(Enum):
    AMD64 = "amd64"
    AARCH64 = "aarch64"


def get_platform_arch() -> CpuArch:
    uname = platform.machine().lower()
    debug(f"uname: {uname}")
    arm64_names = ["aarch64", "arm64", "aarch64_be", "armv8b", "armv8l"]
    x64_names = ["amd64", "x86_64", "x64"]
    if uname in arm64_names:
        return CpuArch.AARCH64
    elif uname in x64_names:
        return CpuArch.AMD64
    else:
        raise PlatformError("Unsupported CPU architecture: " + uname)


def get_public_ip() -> Optional[str]:
    response = requests.get("https://api.ipify.org", timeout=10)
    if response.status_code == 200:
        return response.content.decode()
    else:
        error(f"Failed to get public IP: {response.status_code} {response.reason}")
        return None


def check_port_in_use(port) -> bool:
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind(("localhost", port))
            return False
    except socket.error as e:
        if "already in use" in e.strerror:
            return True
        return False
