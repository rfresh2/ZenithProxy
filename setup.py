import http.client
import json
import os
import platform
import re
import subprocess

system = platform.system()

if os.path.exists("launch_config.json"):
    while True:
        print("launch_config.json already exists, overwrite and continue anyway? (y/n)")
        i1 = input("> ").lower()
        if i1 == "n":
            print("Setup complete!")
            print("Run './launch.sh' (Unix) or '.\\launch.bat` (Windows) to start ZenithProxy!")
            exit(0)
        elif i1 == "y":
            break
        else:
            print("Invalid input. Enter y or n")


def validate_linux_cpu_flags():
    x86_64_v3_flags = ["avx", "avx2", "bmi1", "bmi2", "fma", "sse4_1", "sse4_2", "ssse3"]
    try:
        output = subprocess.check_output(['lscpu'], stderr=subprocess.STDOUT, text=True)
        flags_line = [line for line in output.split('\n') if "Flags" in line][0]
        flags = flags_line.split(":")[1].strip().split(" ")
        for flag in x86_64_v3_flags:
            if flag not in flags:
                print("Unsupported CPU for linux release channel. "
                      + "\nFlag not found: " + flag)
                return False
        return True
    except Exception as e:
        print("Error checking CPU flags.")
        return False


def validate_linux_system():
    return system == "Linux" and validate_linux_cpu_flags()


if validate_linux_system():  # otherwise we will always select java
    while True:
        print("Select a ZenithProxy platform: (1/2)")
        print("1. java")
        print("2. linux")
        i1 = input("> ")
        if i1 == "1":
            release_channel = "java"
            break
        elif i1 == "2":
            release_channel = "linux"
            break
        else:
            print("Invalid input. Enter 1 or 2")
else:
    print("Auto-selecting the java release channel based on current system")
    release_channel = "java"

# Auto-selecting 1.20.1 for now
# 1.12.2 is deprecated
# may return this option we support future versions
minecraft_version = "1.20.1"

launch_config = {
    "auto_update": True,
    "auto_update_launcher": True,
    "release_channel": release_channel + "." + minecraft_version,
    "version": "0.0.0",
    "local_version": "0.0.0",
    "repo_owner": "rfresh2",
    "repo_name": "ZenithProxy"
}

with open("launch_config.json", "w") as f:
    f.write(json.dumps(launch_config, indent=2))
    print("launch_config written successfully")

if os.path.exists("config.json"):
    while True:
        print("config.json already exists, overwrite and continue anyway? (y/n)")
        i1 = input("> ").lower()
        if i1 == "n":
            print("Setup complete!")
            print("Run './launch.sh' (Unix) or '.\\launch.bat` (Windows) to start ZenithProxy!")
            exit(0)
        elif i1 == "y":
            break
        else:
            print("Invalid input. Enter y or n")

while True:
    print("Select authentication method: (1/2)")
    print("1. Device Code (Recommended)")
    print("2. Username and Password")

    i1 = input("> ")
    if i1 == "1":
        auth_method = "device_code"
        break
    elif i1 == "2":
        auth_method = "msa"
        break
    else:
        print("Invalid input. Enter 1 or 2")

if auth_method == "msa":
    while True:
        print("Enter your Microsoft account email/username:")
        username = input("> ")
        if "@" in username:
            break
    while True:
        print("Enter your Microsoft account password:")
        password = input("> ")
        if len(password) > 0:
            break

while True:
    print("Input port the proxy will listen on (e.g. 25565):")
    port = input("> ")
    try:
        port = int(port)
        if port < 1 or port > 65535:
            raise ValueError
        break
    except ValueError:
        print("Invalid port number. Must be between 1 and 65535")

while True:
    print("Input the IP address players should connect to. This can be a domain name or an IP address.")
    print("If you are unsure, leave this blank and the proxy will use the IP address of the machine it is running on.")
    print("If you are using a domain name, make sure you have DNS records set up (see README.md)")
    ip = input("> ")
    if ip == "":
        connection = http.client.HTTPSConnection("api.ipify.org")
        connection.request("GET", "/")
        response = connection.getresponse()
        if response.status == 200:
            ip = response.read().decode()
            break
        else:
            print("Failed to get IP address:", response.status, response.reason)
    else:
        break

while True:
    print("Enable Discord bot? (y/n)")
    i2 = input("> ")
    if i2 == "y":
        discord_bot = True
        break
    elif i2 == "n":
        discord_bot = False
        break
    else:
        print("Invalid input. Enter y or n")

if discord_bot:
    while True:
        print("See README.md for instructions on how to set up a Discord bot")
        print("Enter your Discord bot token:")
        discord_bot_token = input("> ")
        if len(discord_bot_token) > 50:
            break
        else:
            print("Invalid token")
    while True:
        print("Enter the channel ID the proxy will be managed in:")
        discord_channel_id = input("> ")
        try:
            discord_channel_id = int(discord_channel_id)
            if discord_channel_id < 1000000000 or discord_channel_id > 9999999999999999999:
                raise ValueError
            break
        except ValueError:
            print("Invalid ID")
    while True:
        print("Enter the role ID of the role that will be able to manage the proxy's whitelist:")
        discord_admin_role_id = input("> ")
        try:
            discord_admin_role_id = int(discord_admin_role_id)
            if discord_admin_role_id < 1000000000 or discord_admin_role_id > 9999999999999999999:
                raise ValueError
            break
        except ValueError:
            print("Invalid ID")
    while True:
        print("Enter the channel ID the proxy will relay chat messages to:")
        discord_chat_relay_channel = input("> ")
        try:
            discord_chat_relay_channel = int(discord_chat_relay_channel)
            if discord_chat_relay_channel < 1000000000 or discord_chat_relay_channel > 9999999999999999999:
                raise ValueError
            if discord_chat_relay_channel == discord_channel_id:
                print("Chat relay channel cannot be the same as the management channel")
                continue
            break
        except ValueError:
            print("Invalid ID")

# Write config.json
config = {}

if auth_method == "msa":
    config["authentication"] = {
        "accountType": "msa",
        "email": username,
        "password": password
    }

ipPattern = re.compile(r"^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$")
proxyAddress = ip + ":" + str(port) if ipPattern.match(ip) else ip

config["server"] = {
    "bind": {
        "port": port,
    },
    "proxyIP": proxyAddress
}

if discord_bot:
    config["discord"] = {
        "enable": True,
        "token": discord_bot_token,
        "channelId": discord_channel_id,
        "accountOwnerRoleId": discord_admin_role_id,
        "chatRelay": {
            "enable": True,
            "channelId": discord_chat_relay_channel
        }
    }

with open("config.json", "w") as f:
    f.write(json.dumps(config, indent=2))
    print("config.json written successfully!")

print("Setup complete!")
print("Run './launch.sh' (Unix) or '.\\launch.bat` (Windows) to start ZenithProxy!")
