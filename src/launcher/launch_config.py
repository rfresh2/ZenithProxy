import json
import os
import re

from utils import critical_error


def version_looks_valid(ver):
    return re.match(r"[0-9]+\.[0-9]+\.[0-9]+", ver) or (len(ver) == 8 and re.match(r"[0-9a-f]+", ver))


def valid_release_channel(channel):
    return channel.startswith("git") or channel.startswith("java") or channel.startswith("linux")


def read_launch_config_file():
    try:
        with open("launch_config.json") as f:
            data = json.load(f)
            return data
    except FileNotFoundError:
        print("launch_config.json not found")
        return None
    except json.decoder.JSONDecodeError:
        print("launch_config.json is invalid")
        return None


class LaunchConfig:
    auto_update = True
    auto_update_launcher = True
    release_channel = "java.1.20.1"
    version = "0.0.0"
    local_version = "0.0.0"
    repo_owner = "rfresh2"
    repo_name = "ZenithProxy"
    custom_jvm_args = None
    launch_dir = "launcher/"

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
            print("Using custom JVM args:", self.custom_jvm_args)

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
        print("Creating default launch_config.json")
        self.write_launch_config()

    def validate_launch_config(self):
        if not valid_release_channel(self.release_channel):
            print("Invalid release channel:", self.release_channel)
            return False
        if not version_looks_valid(self.version):
            print("Invalid version string:", self.version)
            return False
        if self.repo_name == "":
            print("Invalid repo name:", self.repo_name)
            return False
        if self.repo_owner == "":
            print("Invalid repo owner:", self.repo_owner)
            return False
        return True

    def get_mc_version(self):
        # extract mc version from release channel
        # e.g. java.1.20.1 -> 1.20.1
        return self.release_channel.split(".")[1]
