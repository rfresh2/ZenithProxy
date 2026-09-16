"""Filesystem locations selected once, before logging or configuration is opened."""

import sys
from pathlib import Path

# need to duplicate these from launch_platform to avoid circular imports

def is_pyinstaller_bundle() -> bool:
    return getattr(sys, "frozen", False) and hasattr(sys, "_MEIPASS")


def is_nuitka_bundle() -> bool:
    return "__compiled__" in globals()


def executable_path() -> Path:
    return Path(sys.argv[0] if is_nuitka_bundle() else sys.executable).absolute()


# Source and ZIP launches deliberately use the caller's working directory.
# Bundled executables keep their data beside the executable.
APP_ROOT = executable_path().parent if is_pyinstaller_bundle() or is_nuitka_bundle() else Path.cwd()
LAUNCH_DIR = APP_ROOT / "launcher"
CONFIG_FILE = APP_ROOT / "config.json"
LAUNCH_CONFIG_FILE = APP_ROOT / "launch_config.json"
LOG_DIR = APP_ROOT / "log"
