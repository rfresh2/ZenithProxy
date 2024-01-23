import os
import platform
import subprocess

from jdk_install import get_java_executable


def validate_linux_cpu_flags():
    x86_64_v3_flags = ["avx", "avx2", "bmi1", "bmi2", "fma", "sse4_1", "sse4_2", "ssse3"]
    try:
        output = subprocess.check_output(['cat', '/proc/cpuinfo'], stderr=subprocess.STDOUT, text=True)
        flags = []
        for line in output.splitlines():
            if line.startswith("flags"):
                flags = line.split(": ")[1].split(" ")
                break
        for flag in x86_64_v3_flags:
            if flag not in flags:
                print("Unsupported CPU. "
                      + "Use the Java release channel instead. Re-run setup.py to change the release channel. "
                      + "\nFlag not found: " + flag)
                return False
        return True
    except Exception as e:
        print("Error checking CPU flags:", e)
        return False


def validate_linux_glibc_version():
    try:
        output = subprocess.check_output(['ldd', '--version'], stderr=subprocess.STDOUT, text=True)
        # ldd (Ubuntu GLIBC 2.35-0ubuntu3.4) 2.35
        # get the version from the last word of the first line
        version = output.splitlines()[0].split(" ")[-1]
        version = version.split(".")
        if int(version[0]) != 2:
            print("Unsupported OS for linux release channel. "
                  + "\nglibc version too low: " + ".".join(version))
            return False
        if int(version[1]) < 31:
            print("Unsupported OS for linux release channel. "
                  + "\nglibc version too low: " + ".".join(version))
            return False
        return True
    except Exception as e:
        print("Error checking GLIBC version.")
        return False


def validate_linux_system():
    return platform.system() == "Linux" and validate_linux_cpu_flags() and validate_linux_glibc_version()


def validate_system_with_config(config):
    if config.release_channel == "git":
        # check if we have a .git directory
        if not os.path.isdir(".git"):
            print("No .git directory found. Please clone the repository.")
            return False
        return True
    elif config.release_channel.startswith("java"):
        min_java_version = 21 if config.version.startswith("2") else 17
        java_executable = get_java_executable()
        if java_executable is None:
            print(f"Java >={min_java_version} not found.")
            return False
        return True
    elif config.release_channel.startswith("linux"):
        # ignoring this for now
        valid_flags = validate_linux_cpu_flags()
        valid_glibc = validate_linux_glibc_version()
        return platform.system() == "Linux"  # and valid_flags and valid_glibc
    else:
        return False
