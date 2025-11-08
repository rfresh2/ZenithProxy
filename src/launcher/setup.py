import json
import os
import random
import re

import requests

import launch_platform
from jdk_install import get_java_executable, JavaInstallType
from launch_config import read_launch_config_file
from launch_platform import get_public_ip, check_port_in_use
from launch_platform import validate_linux_system
from utils import critical_error


def setup_execute(config):
    if validate_linux_system(config):  # otherwise we will always select java
        while True:
            print("Select a ZenithProxy platform: (1/2)")
            print("1. java")
            print("2. linux (Recommended)")
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
        release_channel = "java"
    print("")

    if release_channel == "java":
        if not launch_platform.validate_java_system(config, JavaInstallType.USER_PROMPT):
            critical_error("Setup cancelled")
        print("")

    # while True:
    #     print("Select a Minecraft version: (1/2)")
    #     print("1. 1.21.0")
    #     print("2. 1.21.4")
    #     print("3. 1.21.5")
    #     i1 = input("> ")
    #     if i1 == "1":
    #         minecraft_version = "1.21.0"
    #         break
    #     elif i1 == "2":
    #         minecraft_version = "1.21.4"
    #         break
    #     elif i1 == "3":
    #         minecraft_version = "1.21.5"
    #         break
    #     else:
    #         print("Invalid input. Enter 1, 2, or 3")
    # print("")

    minecraft_version = "1.21.4"

    config.auto_update = True
    config.auto_update_launcher = True
    config.release_channel = release_channel + "." + minecraft_version
    config.version = "0.0.0"
    config.local_version = "0.0.0"
    config.repo_owner = "rfresh2"
    config.repo_name = "ZenithProxy"
    config.write_launch_config()
    print("launch_config.json written successfully!")
    print("")

    if os.path.exists("config.json"):
        while True:
            print("config.json already exists, overwrite and continue anyway? (y/n)")
            i1 = input("> ").lower()
            if i1 == "n":
                return
            elif i1 == "y":
                break
            else:
                print("Invalid input. Enter y or n")
        print("")

    while True:
        print("Select the type of environment you are running ZenithProxy on.")
        print("1. PC or other computer in your home")
        print("2. VPS or server outside your home")
        i1 = input("> ")
        if i1 == "1":
            ip = "localhost"
            break
        elif i1 == "2":
            print("Attempting to determine IP for players to connect to...")
            ip = get_public_ip()
            if ip is not None:
                print("Found IP:", ip)
            else:
                ip = "localhost"
            break
        else:
            print("Invalid input. Enter 1 or 2.")
    print("")

    while True:
        print("Select the port ZenithProxy will be hosted on.")
        print("If you are unsure, press enter to select a random port.")
        port = input("> ")
        if port == "":
            port = int(random.uniform(35000, 65000))
            attempts = 0
            while check_port_in_use(port) and attempts < 10:
                port = int(random.uniform(35000, 65000))
                attempts += 1
            if attempts == 10:
                port = 25565 # just fall back to 25565 if we can't find a random port
            break
        try:
            port = int(port)
            if port < 1 or port > 65535:
                raise ValueError
            break
        except ValueError:
            print("Invalid port number. Must be between 1 and 65535")
    print("Using port:", port)
    print("")

    upnp = False
    if ip == "localhost":
        while True:
            print("Enable automatic port forwarding with UPnP (https://w.wiki/Ebjt)? (y/n)")
            i1 = input("> ")
            if i1 == "y":
                upnp = True
                print("Attempting to determine IP for players to connect to...")
                public_ip = get_public_ip()
                if public_ip is not None:
                    ip = public_ip
                    print("Found IP:", ip)
                break
            elif i1 == "n":
                upnp = False
                break
            else:
                print("Invalid input. Enter y or n")
        print("")

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
    print("")

    if discord_bot:
        print("See Discord bot setup instructions:")
        print("https://wiki.2b2t.vc/Discord-Bot-Guide")
        discord_verify_verbose = False
        while True:
            print("Enter Discord bot token:")
            discord_bot_token = input("> ")
            if len(discord_bot_token) < 50:
                print("Invalid token")
                continue
            if verify_discord_bot_token(discord_bot_token, discord_verify_verbose):
                break
            discord_verify_verbose = True # verbose on second attempt
        print("")
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
        print("")
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
        print("")
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
        print("")
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
            print("")

    # Write config.json
    config = {}

    # if auth_method == "msa":
    #     config["authentication"] = {"accountType": "msa", "email": username, "password": password}

    ip_pattern = re.compile(r"^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$")
    if ip == "localhost" or ip_pattern.match(ip):
        proxy_address = ip + ":" + str(port)
    else:
        proxy_address = ip

    config["server"] = {
        "bind": {
            "port": port,
        },
        "proxyIP": proxy_address,
        "upnp": upnp
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
    print("")
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


def verify_discord_bot_token(token, verbose=False):
    headers = {
        "Authorization": "Bot " + token,
        "User-Agent": "DiscordBot (https://github.com/rfresh2/ZenithProxy, 1.0)"
    }
    try:
        response = requests.get("https://discord.com/api/applications/@me", headers=headers, timeout=10)
        if response.status_code != 200:
            print("Invalid token. Discord API response code:", response.status_code)
            if verbose:
                print("Full Discord Response:", response.text)
            return False
        response_json = response.json()
        flags = response_json["flags"]
        gateway_message_content = (flags & (1 << 18) == (1 << 18))
        gateway_message_content_limited = (flags & (1 << 19) == (1 << 19))
        if not (gateway_message_content or gateway_message_content_limited):
            print("ERROR: Message content intent is not enabled, flags: " + str(flags))
            print("Enable 'Message Content Intent' in the discord bot settings")
            return False
        return True
    except Exception as e:
        print("ERROR: Verifying discord bot", e)
        return False


def setup_unattended(config):
    # check if launch_config.json exists
    if read_launch_config_file() is None:
        print("Creating unattended launch_config.json")
        mc_version = os.getenv("ZENITH_MC_VERSION", "1.21.4")
        mc_ver_pattern = re.compile(r"(\d+)\.(\d+)\.(\d+)$")
        if not mc_ver_pattern.match(mc_version):
            critical_error("Invalid ZENITH_MC_VERSION.")
        if os.getenv("ZENITH_PLATFORM") is not None:
            platform = os.getenv("ZENITH_PLATFORM").lower()
            if platform not in ["java", "linux"]:
                critical_error("Invalid ZENITH_PLATFORM. Must be one of: java, linux")
            config.release_channel = platform + "." + mc_version
            if platform == "linux":
                if not validate_linux_system(config):
                    critical_error("Cannot use linux on current system")
            elif platform == "java":
                java_exec = get_java_executable(install_type=JavaInstallType.AUTO_INSTALL)
                if java_exec is None:
                    critical_error("Java not found and auto install failed")
            else:
                critical_error("Invalid ZENITH_PLATFORM. Must be one of: java, linux")
        else:
            config.release_channel = "java." + mc_version
            if validate_linux_system(config):
                config.release_channel = "linux." + mc_version
            else:
                config.release_channel = "java." + mc_version
                java_exec = get_java_executable(install_type=JavaInstallType.AUTO_INSTALL)
                if java_exec is None:
                    critical_error("Java not found and auto install failed")
        config.write_launch_config()
    if not os.path.exists("config.json"):
        print("Creating unattended config.json")
        config = {}
        # some env vars have default values
        port = os.getenv("ZENITH_PORT", 25565)
        ip = os.getenv("ZENITH_IP", "localhost")
        discord_disabled_env = os.getenv("ZENITH_DISCORD_DISABLED")

        # idk how exactly you plan to do anything after this but ok
        discord_bot = discord_disabled_env is None or discord_disabled_env.lower() in ['false', '0', 'no']
        if discord_bot:
            discord_bot_token = os.getenv("ZENITH_DISCORD_TOKEN")
            if discord_bot_token is None:
                critical_error("ZENITH_DISCORD_TOKEN env variable must be set in unattended mode")
            if not verify_discord_bot_token(discord_bot_token, verbose=True):
                critical_error("Invalid Discord bot token")
            discord_channel_id = os.getenv("ZENITH_DISCORD_CHANNEL_ID")
            if discord_channel_id is None:
                critical_error("ZENITH_DISCORD_CHANNEL_ID env variable must be set in unattended mode")
            discord_channel_id = int(discord_channel_id)
            if discord_channel_id < 1000000000 or discord_channel_id > 9999999999999999999:
                critical_error("Invalid Discord channel ID")
            discord_admin_role_id = os.getenv("ZENITH_DISCORD_ROLE_ID")
            if discord_admin_role_id is None:
                critical_error("ZENITH_DISCORD_ROLE_ID env variable must be set in unattended mode")
            discord_admin_role_id = int(discord_admin_role_id)
            if discord_admin_role_id < 1000000000 or discord_admin_role_id > 9999999999999999999:
                critical_error("Invalid Discord role ID")
            if os.getenv("ZENITH_DISCORD_CHAT_RELAY_CHANNEL") is None:
                chat_relay = False
            else:
                chat_relay = True
                discord_chat_relay_channel = os.getenv("ZENITH_DISCORD_CHAT_RELAY_CHANNEL")
                if discord_chat_relay_channel is None:
                    critical_error("ZENITH_DISCORD_CHAT_RELAY_CHANNEL env variable must be set in unattended mode")
                discord_chat_relay_channel = int(discord_chat_relay_channel)
                if discord_chat_relay_channel < 1000000000 or discord_chat_relay_channel > 9999999999999999999:
                    critical_error("Invalid Discord chat relay channel ID")

        ip_pattern = re.compile(r"^(?:[0-9]{1,3}\.){3}[0-9]{1,3}$")
        if ip == "localhost" or ip_pattern.match(ip):
            proxy_address = ip + ":" + str(port)
        else:
            proxy_address = ip

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
        print("")
        print("Unattended Setup complete!")
