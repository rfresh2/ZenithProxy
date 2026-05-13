import json
import os
import time

from log import error, info

env_prefix = "ZENITH_CONFIG_"

def apply():
    info("Applying environment variables to config.json...")
    config = read_config()

    valid_env_entries = []
    for env_entry in os.environ.items():
        if env_entry[0].startswith(env_prefix):
            valid_env_entries.append(env_entry)
    if len(valid_env_entries) == 0:
        info(f"No environment variables found with the config prefix: {env_prefix}")
        return

    for env_entry in valid_env_entries:
        env_key, env_value = env_entry
        config_key = env_key.replace(env_prefix, "", count=1).replace("_", ".")
        value = os.getenv(env_key)
        if value is not None:
            config_key_split = config_key.split(".")
            config_dict = config
            for key in config_key_split[:-1]:
                config_dict = config_dict.setdefault(key, {})
            config_dict[config_key_split[-1]] = value
            info(f"Set {config_key} to {value}")

    write_config(config)

def read_config() -> dict:
    try:
        with open("config.json") as f:
            data = json.load(f)
            return data
    except FileNotFoundError:
        error("config.json not found")
        return {}
    except json.decoder.JSONDecodeError:
        error("config.json is invalid")
        os.replace("config.json", f"config.backup-{time.time_ns()}.json")
        return {}

def write_config(data: dict):
    with open("config.json.tmp", "w") as f:
        json.dump(data, f, indent=2)
    os.replace("config.json.tmp", "config.json")
    info("config.json written successfully!")
