import json
import os
import re
from typing import Optional

from log import info, error, critical_error


def version_looks_valid(ver: str) -> bool:
    return re.match(r"[0-9]+\.[0-9]+\.[0-9]+", ver) or (len(ver) == 8 and re.match(r"[0-9a-f]+", ver))


def valid_release_channel(channel: str) -> bool:
    return channel.startswith("git") or channel.startswith("java") or channel.startswith("linux")


def read_launch_config_file() -> Optional[dict]:
    try:
        with open("launch_config.json") as f:
            data = json.load(f)
            return data
    except FileNotFoundError:
        info("launch_config.json not found")
        return None
    except json.decoder.JSONDecodeError:
        error("launch_config.json is invalid")
        return None


class LaunchConfig:

    def __init__(self):
        self.auto_update = True
        self.auto_update_launcher = True
        self.release_channel = "java.1.21.4"
        self.version = "0.0.0"
        self.local_version = "0.0.0"
        self.repo_owner = "rfresh2"
        self.repo_name = "ZenithProxy"
        self.custom_jvm_args = None
        self.launch_dir = "launcher/"

    def load_launch_config_data(self, data):
        if data is None:
            critical_error("No data to read from launch_config.json")
        self.auto_update = data.get("auto_update", self.auto_update)
        self.auto_update_launcher = data.get("auto_update_launcher", self.auto_update_launcher)
        self.release_channel = data.get("release_channel", self.release_channel)
        self.version = data.get("version", self.version)
        self.local_version = data.get("local_version", self.local_version)
        self.repo_owner = data.get("repo_owner", self.repo_owner)
        self.repo_name = data.get("repo_name", self.repo_name)
        self.custom_jvm_args = data.get("custom_jvm_args", self.custom_jvm_args)
        if self.custom_jvm_args is not None and self.custom_jvm_args != "":
            info(f"Using custom JVM args: {self.custom_jvm_args}")

    def write_launch_config(self):
        output = {
            "auto_update": self.auto_update,
            "auto_update_launcher": self.auto_update_launcher,
            "release_channel": self.release_channel,
            "version": self.version,
            "local_version": self.version,
            "repo_owner": self.repo_owner,
            "repo_name": self.repo_name,
        }
        if (self.custom_jvm_args is not None) and (self.custom_jvm_args != ""):
            output["custom_jvm_args"] = self.custom_jvm_args
        with open("launch_config.json.tmp", "w") as f:
            f.write(json.dumps(output, indent=2))
        os.replace("launch_config.json.tmp", "launch_config.json")

    def create_default_launch_config(self):
        info("Creating default launch_config.json")
        self.write_launch_config()

    def validate_launch_config(self):
        if not valid_release_channel(self.release_channel):
            error(f"Invalid release channel: {self.release_channel}")
            return False
        if not version_looks_valid(self.version):
            error(f"Invalid version string: {self.version}")
            return False
        if self.repo_name == "":
            error(f"Invalid repo name: {self.repo_name}")
            return False
        if self.repo_owner == "":
            error(f"Invalid repo owner: {self.repo_owner}")
            return False
        return True

    def get_mc_version(self) -> str:
        # extract mc version from release channel
        # e.g. java.1.20.1 -> 1.20.1 or linux.1.20.1 -> 1.20.1
        channel = self.release_channel
        if channel.endswith(".pre"):
            channel = channel[:-4]
        java = channel.startswith("java")
        linux = channel.startswith("linux")
        if channel.find(".") == -1 or (not java and not linux):
            return "1.21.4"
        return channel[channel.find(".") + 1 :]
