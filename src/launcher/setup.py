import json
import os
import re

import requests

from launch_platform import validate_linux_system
from utils import critical_error


def setup_execute(config):
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

    ## Prompt can be added back for 1.20.5+ versions
    minecraft_version = "1.20.4"
    # while True:
    #     print("Select a Minecraft version: (1/2)")
    #     print("1. 1.20.1")
    #     print("2. 1.20.4")
    #     i1 = input("> ")
    #     if i1 == "1":
    #         minecraft_version = "1.20.1"
    #         break
    #     elif i1 == "2":
    #         minecraft_version = "1.20.4"
    #         break
    #     else:
    #         print("Invalid input. Enter 1 or 2")

    config.auto_update = True
    config.auto_update_launcher = True
    config.release_channel = release_channel + "." + minecraft_version
    config.version = "0.0.0"
    config.local_version = "0.0.0"
    config.repo_owner = "rfresh2"
    config.repo_name = "ZenithProxy"
    config.write_launch_config()

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
        print("Input port the proxy will use.")
        print("If you are unsure, leave this blank; port 25565 will be selected.")
        port = input("> ")
        if port == "":
            port = 25565
            break
        try:
            port = int(port)
            if port < 1 or port > 65535:
                raise ValueError
            break
        except ValueError:
            print("Invalid port number. Must be between 1 and 65535")

    while True:
        print("Input the IP address players should connect to. This can be a domain name or an IP address.")
        print(
            "If you are unsure, leave this blank and the proxy will use the IP address of the machine it is running on."
        )
        print("If you are using a domain name, make sure you have DNS records set up (see README.md)")
        ip = input("> ")
        if ip == "":
            response = requests.get("https://api.ipify.org")
            if response.status_code == 200:
                ip = response.content.decode()
                break
            else:
                print("Failed to get IP address:", response.status_code, response.reason)
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
        print("See README.md for Discord bot setup instructions")
        while True:
            print("Enter Discord bot token:")
            discord_bot_token = input("> ")
            if len(discord_bot_token) < 50:
                print("Invalid token")
                continue
            if verify_discord_bot_token(discord_bot_token):
                break
        while True:
            print("Enter a Discord channel ID to manage ZenithProxy in:")
            discord_channel_id = input("> ")
            try:
                discord_channel_id = int(discord_channel_id)
                if discord_channel_id < 1000000000 or discord_channel_id > 9999999999999999999:
                    raise ValueError
                # todo: verify the bot is in this channel
                break
            except ValueError:
                print("Invalid ID")
        while True:
            print("Enter a Discord Role ID to grant management permissions like the whitelist to:")
            discord_admin_role_id = input("> ")
            try:
                discord_admin_role_id = int(discord_admin_role_id)
                if discord_admin_role_id < 1000000000 or discord_admin_role_id > 9999999999999999999:
                    raise ValueError
                # todo: verify the bot has this role
                break
            except ValueError:
                print("Invalid ID")
        while True:
            print("Enable Discord Chat Relay? (y/n)")
            i3 = input("> ")
            if i3 == "y":
                chat_relay = True
                break
            elif i3 == "n":
                chat_relay = False
                break
            else:
                print("Invalid input. Enter y or n")
        if chat_relay:
            while True:
                print("Enter a Discord channel ID for the Chat Relay:")
                discord_chat_relay_channel = input("> ")
                try:
                    discord_chat_relay_channel = int(discord_chat_relay_channel)
                    if discord_chat_relay_channel < 1000000000 or discord_chat_relay_channel > 9999999999999999999:
                        raise ValueError
                    if discord_chat_relay_channel == discord_channel_id:
                        print("Chat Relay and Management cannot have the same channel")
                        continue
                    # todo: verify the bot is in this channel
                    break
                except ValueError:
                    print("Invalid ID")

    # Write config.json
    config = {}

    if auth_method == "msa":
        config["authentication"] = {"accountType": "msa", "email": username, "password": password}

    ip_pattern = re.compile(r"^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$")
    proxy_address = ip + ":" + str(port) if ip_pattern.match(ip) else ip

    config["server"] = {
        "bind": {
            "port": port,
        },
        "proxyIP": proxy_address,
    }

    if discord_bot:
        config["discord"] = {
            "enable": True,
            "token": discord_bot_token,
            "channelId": discord_channel_id,
            "accountOwnerRoleId": discord_admin_role_id,
        }
        if chat_relay:
            config["discord"]["chatRelay"] = {
                "enable": True,
                "channelId": discord_chat_relay_channel
            }

    with open("config.json", "w") as f:
        f.write(json.dumps(config, indent=2))
        print("config.json written successfully!")

    print("Setup complete!")


def rescue_invalid_system(config):
    print("CRITICAL: Invalid system for release channel: " + config.release_channel)
    while True:
        print("Run setup? (y/n)")
        i1 = input("> ")
        if i1 == "y":
            setup_execute(config)
            return
        elif i1 == "n":
            critical_error("Invalid system for release channel:", config.release_channel)


def verify_discord_bot_token(token):
    headers = {
        "Authorization": "Bot " + token
    }
    try:
        response = requests.get("https://discord.com/api/applications/@me", headers=headers)
        if response.status_code != 200:
            print("Invalid token. Discord API response code:", response.status_code)
            return False
        response_json = response.json()
        flags = response_json["flags"]
        message_content_intent = flags >> 19
        if message_content_intent != 1:
            print("ERROR: Message content intent is not enabled.")
            print("Enable 'Message Content Intent' in the discord bot settings")
            return False
        return True
    except Exception as e:
        print("ERROR: Verifying discord bot", e)
        return False


