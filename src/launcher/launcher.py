import os
import platform
import subprocess

from jdk_install import get_java_executable
from launch_platform import validate_system_with_config
from utils import critical_error

default_java_args = """\
-Xmx300m \
-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch \
-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 \
-XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 \
-XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=2 -Dio.netty.allocator.maxOrder=9 \
-Dio.netty.leakDetection.level=disabled"""

default_linux_args = """\
-Xmx200m \
-Djava.util.concurrent.ForkJoinPool.common.parallelism=2 \
-Dio.netty.allocator.maxOrder=9 \
-Dio.netty.leakDetection.level=disabled"""


def git_read_version(config):
    try:
        output = subprocess.check_output(['git', 'rev-parse', '--short=8', 'HEAD'], stderr=subprocess.STDOUT, text=True)
        v = str(output).splitlines()[0].strip()
        if len(v) == 8:
            config.version = v
            config.local_version = v
            print("Git commit:", config.version)
        else:
            print("Invalid version string found from git:", output)
    except subprocess.CalledProcessError as e:
        print("Error reading local git version:")
        print(e.stderr)


def git_build():
    if platform.system() == "Windows":
        os.system(".\\gradlew jarBuild --no-daemon")
    else:
        os.system("./gradlew jarBuild --no-daemon")


def launcher_exec(config):
    if config.release_channel == "git":
        git_read_version(config)

    config.write_launch_config()

    if config.version == "0.0.0" or config.local_version == "0.0.0":
        if config.release_channel == "git":
            critical_error("Invalid version found for git release channel:" + config.version
                           + "\nRe-run setup.py and select another release channel.")
        critical_error("Invalid version found:'" + config.version + "'"
                       + "\nEnable `auto_updater` or specify a valid version in launch_config.json.")

    if not validate_system_with_config(config):
        critical_error("Invalid system for release channel: " + config.release_channel)

    # Launch application

    if config.release_channel == "git":
        git_build()
        if config.custom_jvm_args is not None and config.custom_jvm_args != "":
            jvm_args = config.custom_jvm_args
        else:
            jvm_args = default_java_args
        if platform.system() == 'Windows':
            toolchain_command = ".\\build\\java_toolchain.bat"
            jar_command = "-jar build\\libs\\ZenithProxy.jar"
        else:
            toolchain_command = "./build/java_toolchain"
            jar_command = "-jar build/libs/ZenithProxy.jar"
        run_script = f"{toolchain_command} {jvm_args} {jar_command}"
        try:
            subprocess.run(run_script, shell=True, check=True)
        except subprocess.CalledProcessError as e:
            print("Error launching application:", e)
    elif config.release_channel.startswith("java"):
        java_executable = get_java_executable().replace("/", "\\")
        if not os.path.isfile(config.launch_dir + "ZenithProxy.jar"):
            critical_error("ZenithProxy.jar not found")
        if config.custom_jvm_args is not None and config.custom_jvm_args != "":
            jvm_args = config.custom_jvm_args
        else:
            jvm_args = default_java_args
        if platform.system() == 'Windows':
            toolchain_command = "call " + java_executable
            jar_command = "-jar " + config.launch_dir.replace("/", "\\") + "ZenithProxy.jar"
        else:
            toolchain_command = java_executable
            jar_command = "-jar " + config.launch_dir + "ZenithProxy.jar"
        run_script = f"{toolchain_command} {jvm_args} {jar_command}"
        try:
            subprocess.run(run_script, shell=True, check=True)
        except subprocess.CalledProcessError as e:
            critical_error("Error launching application:" + str(e))
    elif config.release_channel.startswith("linux"):
        if platform.system() != "Linux":
            critical_error("Linux release channel is not supported on current system: " + platform.system())
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
            critical_error("Error launching application:" + str(e))
    else:
        critical_error("Invalid release channel:" + config.release_channel)
