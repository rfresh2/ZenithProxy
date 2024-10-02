import os
import platform
import subprocess

from jdk_install import get_java_executable
from utils import critical_error

default_java_args = """\
-Xmx300m \
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch \
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1"""

default_linux_args = """\
-Xmx200m"""


def git_build():
    if platform.system() == "Windows":
        os.system(".\\gradlew jarBuild --no-daemon")
    else:
        os.system("./gradlew jarBuild --no-daemon")


def launch_linux(config):
    if not os.path.isfile(config.launch_dir + "ZenithProxy"):
        critical_error("ZenithProxy executable not found")
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
    else:
        jvm_args = default_linux_args
    run_script = f"./{config.launch_dir}ZenithProxy {jvm_args}"
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:", e)


def launch_java(config):
    java_executable = get_java_executable()
    print("Using Java installation:", java_executable)
    if platform.system() == "Windows":
        java_executable = java_executable.replace("/", "\\")
    if not os.path.isfile(config.launch_dir + "ZenithProxy.jar"):
        critical_error("ZenithProxy.jar not found")
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
    else:
        jvm_args = default_java_args
    if platform.system() == "Windows":
        jar_command = "-jar " + config.launch_dir.replace("/", "\\") + "ZenithProxy.jar"
    else:
        jar_command = "-jar " + config.launch_dir + "ZenithProxy.jar"
    run_script = f"\"{java_executable}\" {jvm_args} {jar_command}"
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:", str(e))


def launch_git(config):
    git_build()
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
    else:
        jvm_args = default_java_args
    if platform.system() == "Windows":
        toolchain_command = ".\\build\\java_toolchain.bat"
        jar_command = "-jar build\\libs\\ZenithProxy.jar"
    else:
        toolchain_command = "./build/java_toolchain"
        jar_command = "-jar build/libs/ZenithProxy.jar"
    run_script = f"{toolchain_command} {jvm_args} {jar_command}"
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:", e)


def launcher_exec(config):
    print("Launching ZenithProxy...")
    if config.release_channel == "git":
        launch_git(config)
    elif config.release_channel.startswith("java"):
        launch_java(config)
    elif config.release_channel.startswith("linux"):
        launch_linux(config)
    else:
        critical_error("Invalid release channel:", config.release_channel)
