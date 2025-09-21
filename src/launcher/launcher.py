import os
import platform
import subprocess

from jdk_install import get_java_executable, get_java_version_from_subprocess
from utils import critical_error

default_java_xmx = 300

default_java_args = """\
-XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+PerfDisableSharedMem"""

java24_addnl_args = """\
-XX:+UseCompactObjectHeaders --sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED"""

default_linux_xmx = 225

default_linux_args = ""

def git_build():
    if platform.system() == "Windows":
        os.system(".\\gradlew build --no-daemon")
    else:
        os.system("./gradlew build --no-daemon")


def launch_linux(config):
    if not os.path.isfile(config.launch_dir + "ZenithProxy"):
        critical_error("ZenithProxy executable not found")
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
        # no default args to set yet
        # if jvm_args.startswith("-Xmx") and len(jvm_args.split(" ")) == 1:
        #     jvm_args += " " + default_linux_args
    else:
        jvm_args = default_linux_args
    if "-Xmx" not in jvm_args:
        jvm_args += f" -Xmx{default_linux_xmx}M"
    run_script = f"./{config.launch_dir}ZenithProxy {jvm_args}"
    print(">", run_script)
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:", e)


def launch_java(config):
    java_executable = get_java_executable()
    print("Using Java installation:", java_executable)
    java_version = int(get_java_version_from_subprocess(java_executable))
    if platform.system() == "Windows":
        java_executable = "\"" + java_executable.replace("/", "\\") + "\""
    if not os.path.isfile(config.launch_dir + "ZenithProxy.jar"):
        critical_error("ZenithProxy.jar not found")
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
        # if jvm args only contain -Xmx<int><unit>, add default args
        if jvm_args.startswith("-Xmx") and len(jvm_args.split(" ")) == 1:
            jvm_args += " " + default_java_args
            if java_version == 24 or java_version == 25:
                jvm_args += " " + java24_addnl_args
    else:
        jvm_args = default_java_args
        if java_version == 24 or java_version == 25:
            jvm_args += " " + java24_addnl_args
    if "-Xmx" not in jvm_args:
        jvm_args += f" -Xmx{default_java_xmx}M"
    if platform.system() == "Windows":
        jar_command = "-jar " + config.launch_dir.replace("/", "\\") + "ZenithProxy.jar"
    else:
        jar_command = "-jar " + config.launch_dir + "ZenithProxy.jar"
    run_script = f"{java_executable} {jvm_args} {jar_command}"
    print(">", run_script)
    try:
        subprocess.run(run_script, shell=True, check=True)
    except subprocess.CalledProcessError as e:
        critical_error("Error launching application:", str(e))


def launch_git(config):
    git_build()
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
        if jvm_args.startswith("-Xmx") and len(jvm_args.split(" ")) == 1:
            jvm_args += " " + default_java_args
    else:
        jvm_args = default_java_args
    if "-Xmx" not in jvm_args:
        jvm_args += f" -Xmx{default_java_xmx}M"
    if platform.system() == "Windows":
        toolchain_command = ".\\build\\java_toolchain.bat"
        jar_command = "-jar build\\libs\\ZenithProxy.jar"
    else:
        toolchain_command = "./build/java_toolchain"
        jar_command = "-jar build/libs/ZenithProxy.jar"
    run_script = f"{toolchain_command} {jvm_args} {jar_command}"
    print(">", run_script)
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
