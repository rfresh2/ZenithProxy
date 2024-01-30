import os
import platform
import subprocess
import sys

from jdk_install import get_java_executable


def validate_linux_cpu_flags():
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
                print(
                    "Unsupported CPU. "
                    + "Use the Java release channel instead. Re-run setup.py to change the release channel. "
                    + "\nFlag not found: "
                    + flag
                )
                return False
        return True
    except Exception as e:
        print("Error checking CPU flags:", e)
        return False


def validate_linux_glibc_version():
    try:
        output = subprocess.check_output(["ldd", "--version"], stderr=subprocess.STDOUT, text=True)
        # ldd (Ubuntu GLIBC 2.35-0ubuntu3.4) 2.35
        # get the version from the last word of the first line
        version = output.splitlines()[0].split(" ")[-1]
        version = version.split(".")
        if int(version[0]) != 2:
            print("Unsupported OS for linux release channel. " + "\nglibc version too low: " + ".".join(version))
            return False
        if int(version[1]) < 31:
            print("Unsupported OS for linux release channel. " + "\nglibc version too low: " + ".".join(version))
            return False
        return True
    except Exception as e:
        print("Error checking GLIBC version.")
        return False


def validate_linux_system():
    return platform.system() == "Linux" and validate_linux_cpu_flags() and validate_linux_glibc_version()


def validate_java_system(config):
    min_java_version = 17 if config.version.startswith("1") else 21
    java_executable = get_java_executable()
    if java_executable is None:
        print(f"Java >={min_java_version} not found.")
        return False
    return True


def validate_git_system():
    # check if we have a .git directory
    if not os.path.isdir(".git"):
        return False
    return True


def validate_system_with_config(config):
    if config.release_channel == "git":
        return validate_git_system()
    elif config.release_channel.startswith("java"):
        return validate_java_system(config)
    elif config.release_channel.startswith("linux"):
        return validate_linux_system()
    else:
        return False


def is_pyinstaller_bundle():
    return getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS")


def get_platform_os():
    if platform.system() == "Windows":
        return "windows"
    elif platform.system() == "Linux":
        return "linux"
    elif platform.system() == "Darwin":
        return "macos"
    else:
        return "unknown"


def get_platform_arch():
    if platform.machine().lower() == "amd64":
        return "amd64"
    elif platform.machine() == "x86_64":
        return "amd64"
    elif platform.machine().lower() == "arm64":
        return "aarch64"
    else:
        return "unknown"
