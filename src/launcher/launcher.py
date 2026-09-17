import platform
import shlex
import subprocess
import time
from collections import deque
from pathlib import Path

import launch_platform
from jdk_install import get_java_instance
from launcher_paths import APP_ROOT, LAUNCH_DIR
from log import info, warn, critical_error, critical_exception

default_java_xmx = 300

default_java_args = """\
-XX:+IgnoreUnrecognizedVMOptions -XX:+UseG1GC -Xms32m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:G1PeriodicGCInterval=30000 -XX:TrimNativeHeapInterval=30000 -XX:+UnlockExperimentalVMOptions -XX:+PerfDisableSharedMem"""

java24_addnl_args = """\
-XX:+UseCompactObjectHeaders --sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED"""

java26_addnl_args = """\
--enable-final-field-mutation=ALL-UNNAMED"""

default_linux_xmx = 225

default_linux_args = ""

bootloop_threshold = 10
bootloop_window = 60
launch_history = deque()


def git_build():
    wrapper = APP_ROOT / ("gradlew.bat" if platform.system() == "Windows" else "gradlew")
    command = f"{_quote_path(wrapper)} build --no-daemon"
    info(f"> {command}")
    try:
        subprocess.run(command, shell=True, cwd=APP_ROOT, check=True)
    except subprocess.CalledProcessError as e:
        critical_exception("Error building application")


def launch_linux(config):
    if not (LAUNCH_DIR / "ZenithProxy").is_file():
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
    command = f"{_quote_path(LAUNCH_DIR / 'ZenithProxy')} {jvm_args}"
    info(f"> {command}")
    _record_launch()
    before = time.time()
    try:
        subprocess.run(command, shell=True, cwd=APP_ROOT, check=True)
    except subprocess.CalledProcessError as e:
        if e.returncode == 69:
            critical_error("Shutdown requested by user.")
        if config.custom_jvm_args is not None:
            after = time.time()
            if after - before <= 1:
                config.custom_jvm_args = None
                config.write_launch_config()
                warn("Resetting custom JVM args and retrying.")
                return
        critical_exception("Error launching application")


def launch_java(config):
    java_instance = get_java_instance(launch_platform.min_java_version(config))
    if java_instance is None or java_instance.path is None or java_instance.version is None:
        critical_error("Java not found")
    java_executable = java_instance.path
    info(f"Using Java installation: {java_instance}")
    java_version = int(java_instance.version.major)
    if not (LAUNCH_DIR / "ZenithProxy.jar").is_file():
        critical_error("ZenithProxy.jar not found")
    if config.custom_jvm_args is not None and config.custom_jvm_args != "":
        jvm_args = config.custom_jvm_args
        # if jvm args only contain -Xmx<int><unit>, add default args
        if jvm_args.startswith("-Xmx") and len(jvm_args.split(" ")) == 1:
            jvm_args += " " + default_java_args
            if java_version in (24, 25, 26, 27):
                jvm_args += " " + java24_addnl_args
            if java_version in (26, 27):
                jvm_args += " " + java26_addnl_args
    else:
        jvm_args = default_java_args
        if java_version in (24, 25, 26, 27):
            jvm_args += " " + java24_addnl_args
        if java_version in (26, 27):
            jvm_args += " " + java26_addnl_args
    if "-Xmx" not in jvm_args:
        jvm_args += f" -Xmx{default_java_xmx}M"
    command = f"{_quote_path(java_executable)} {jvm_args} -jar {_quote_path(LAUNCH_DIR / 'ZenithProxy.jar')}"
    info(f"> {command}")
    _record_launch()
    before = time.time()
    try:
        subprocess.run(command, shell=True, cwd=APP_ROOT, check=True)
    except subprocess.CalledProcessError as e:
        if e.returncode == 69:
            critical_error("Shutdown requested by user.")
        if config.custom_jvm_args is not None:
            after = time.time()
            if after - before <= 1:
                config.custom_jvm_args = None
                config.write_launch_config()
                warn("Resetting custom JVM args and retrying.")
                return
        critical_exception("Error launching application")


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
    toolchain = APP_ROOT / "build" / ("java_toolchain.bat" if platform.system() == "Windows" else "java_toolchain")
    jar = APP_ROOT / "build" / "libs" / "ZenithProxy.jar"
    command = f"{_quote_path(toolchain)} {jvm_args} -jar {_quote_path(jar)}"
    info(f"> {command}")
    _record_launch()
    before = time.time()
    try:
        subprocess.run(command, shell=True, cwd=APP_ROOT, check=True)
    except subprocess.CalledProcessError as e:
        if e.returncode == 69:
            critical_error("Shutdown requested by user.")
        if config.custom_jvm_args is not None:
            after = time.time()
            if after - before <= 1:
                config.custom_jvm_args = None
                config.write_launch_config()
                warn("Resetting custom JVM args and retrying.")
                return
        critical_exception("Error launching application: %s")


def launcher_exec(config):
    info("Launching ZenithProxy...")
    check_bootloop()
    if config.release_channel == "git":
        launch_git(config)
    elif config.release_channel.startswith("java"):
        launch_java(config)
    elif config.release_channel.startswith("linux"):
        launch_linux(config)
    else:
        critical_error(f"Invalid release channel: {config.release_channel}")


def _record_launch():
    now = time.time()
    launch_history.append(now)


def _launch_count_in_window():
    now = time.time()
    cutoff = now - bootloop_window
    return sum(1 for t in launch_history if t >= cutoff)


def check_bootloop():
    count = _launch_count_in_window()
    if count > bootloop_threshold:
        critical_error(f"Possible bootloop detected {count} launches within {bootloop_window} seconds. ")


def _quote_path(path: Path) -> str:
    """Quote a native filesystem path without changing the JVM argument string."""
    if platform.system() == "Windows":
        return f'"{path}"'
    return shlex.quote(str(path))
