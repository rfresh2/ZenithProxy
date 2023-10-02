# ZenithProxy

<p align="center">
  <img alt="Discord" src="https://dcbadge.vercel.app/api/server/nJZrSaRKtb"
</p>

<p align="center">
  <img alt="Downloads" src="https://img.shields.io/github/downloads/rfresh2/ZenithProxy/total">
  <img src="https://img.shields.io/badge/MC-1.12.2-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.1-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/github/languages/code-size/rfresh2/ZenithProxy.svg" alt="Code size"/>
  <img src="https://img.shields.io/github/repo-size/rfresh2/ZenithProxy.svg" alt="GitHub repo size"/>
  <img src="https://tokei.rs/b1/github/rfresh2/ZenithProxy?category=code&style=flat" alt="Lines of Code"/>
</p>


Minecraft proxy/bot intended for use on 2b2t.org.

Currently supports 1.12.2 and 1.20.1

The primary purpose is to have Minecraft accounts always online and securely shared by multiple people.

This project is also used to support the [2b2t.vc API](https://api.2b2t.vc) and [Discord Bot](https://bot.2b2t.vc).

<details>
    <summary>What is a proxy?</summary>

    This proxy itself consists of two components:
    1. A Minecraft Server ("Proxy Server")
    2. A Minecraft Client ("Proxy Client")

    Players use a Minecraft client to connect to the Proxy Server as you would a normal MC server.
    The Proxy Client connects to a destination MC server (i.e. 2b2t.org).
    The Player's packets to the Proxy Server get forwarded to the Proxy Client which forwards them to the destination
    MC server.
    
    Player MC Client -> Proxy Server -> Proxy Client -> MC Server
    
    When no Player Client is connected the Proxy Client can still act as a bot: moving around, chatting, etc.
</details>

<details>
    <summary>How does it work?</summary>

    The Proxy caches the client's world state including chunks, entities, other players, etc. to allow Player Clients to connect at any time.

    The Proxy is also able to read/modify/cancel/send arbitrary packets in either direction at any time. This is used to simulate
    player movements, spectator mode, discord chat relay, and more.
</details>

# Features

* High performance and efficiency on minimal hardware, <300MB RAM per java instance or <150MB on linux.
* Secure Whitelist system - share MC accounts without sharing passwords
* Extensive discord integration
    * Chat relay
    * Player in visual range alerts
    * 25+ commands and modules to configure every feature in the proxy
    * Without discord, all commands are still supported in the interactive terminal
* Spectator mode
  * Multiple players can connect to the proxy and spectate the player as entities in-game or playercam
* Advanced AntiAFK with full player movement simulation
* Modules including AutoEat, AutoDisconnect, AutoReconnect, AutoRespawn, AutoTotem, KillAura, Spammer, AutoReply
* Many, many, more features.

# Getting Started

## Prerequisites

1. Linux or Windows computer. I recommend DigitalOcean's `1 GB Memory / 1 vCPU` VPS in NYC-1 for
   minimal ping to 2b2t. 2b2t allows a maximum of 3 accounts concurrently connected per IP address.
   [Free DigitalOcean $200 credit for new accounts](https://m.do.co/c/3a3a226e4936).
2. [Python 3](https://www.python.org/downloads/) installed.

## Setup

1. Download `ZenithProxyLauncher.zip` from the [releases page](https://github.com/rfresh2/ZenithProxy/releases/launcher) and unzip to a new folder. 
OR clone the repository `git clone git@github.com:rfresh2/ZenithProxy.git`
2. Open a terminal in the directory, run `python3 setup.py`, and follow the prompts. (if `python3` is not recognized, try `python` or `py`)

### Release Channels

ZenithProxy has 2 system platforms:

* (Default) `java` - Supports all operating systems
  * Requires Java 21+. [Java Downloads](https://adoptium.net/)
* (Recommended) `linux` - Linux native x86_64 executable. ~50% reduced memory usage and instant startup

You also need to select a minecraft version. There are currently 2 options:
* `1.20.1`
* `1.12.2`

The release channel is based on a combination of the platform and minecraft version, examples: `linux.1.20.1` or `java.1.12.2`

### DNS Setup

* To use a domain name you need the following DNS records:
  * an `A` record to your IP address
  * an `SRV` record for `_minecraft._tcp` with the port and the `A` record as its target. [Example](https://cdn.discordapp.com/attachments/971140948593635335/1139099459431698463/firefox_GSnrLzpsR3.png)

### Discord Bot Setup

* Create a discord bot here: [discord.com/developers](https://discord.com/developers/)
* Enable `Message Content Intent` under the "Bot" tab.
* Invite the discord bot to a server.
* Create a role for users to manage the proxy, a channel to manage the 
  proxy in, and a channel for the chat relay. The bot's role must have permissions to send and receive messages in both channels
* Run `setup.py` or edit these`config.json` properties:
  * `enable` -> set to `true`
  * `token` -> the bot's discord token
  * `channelId` -> the channel ID where you will manage the proxy from
  * `accountOwnerRoleId` -> a discord role ID that allows managing sensitive configuration like the whitelist
  * `chatRelay.channelId` -> The channel ID where the MC server chat relay will be sent and received from.

## Run

* `./launch.sh` (Linux) or `.\launch.bat` (Windows)
* The discord prefix is `.` by default. e.g. `.connect` or `.disconnect`.
* Type `.help` in discord or `help` in the interactive terminal to get a list of available commands.

To set custom JVM args, (e.g. to change the max heap size) edit `custom_jvm_args` in `launch_config.json`

## AutoUpdater

The AutoUpdater is enabled by default. It updates both ZenithProxy and the launcher script. 

It can be enabled/disabled in `launch_config.json`:
* ZenithProxy AutoUpdater: `auto_update`. Can also be configured with the command `autoUpdate off` (with `.` prefix if in discord).
* Launcher AutoUpdater: `auto_update_launcher`

To use an exact version, in `launch_config.json` set `auto_update` to `false` and `version` to the desired version (e.g. `1.0.0+java`).

# Developers

* [rfresh2](https://github.com/rfresh2)
* [odpay](https://github.com/odpay)

## Special Thanks

* [Pork2b2tBot Contributors](https://github.com/PorkStudios/Pork2b2tBot/graphs/contributors)
* [MCProtocolLib Contributors](https://github.com/GeyserMC/MCProtocolLib/graphs/contributors)
