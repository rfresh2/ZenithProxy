# ZenithProxy

<p align="center">
  <a href="https://discord.gg/nJZrSaRKtb">
  <img alt="Discord" src="https://dcbadge.vercel.app/api/server/nJZrSaRKtb">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/MC-1.21-brightgreen.svg" alt="Minecraft"/>
  <img src="https://tokei.rs/b1/github/rfresh2/ZenithProxy?category=code&style=flat" alt="Lines of Code"/>
</p>


Minecraft proxy and bot designed for 2b2t.org but also works on any server. 

ZenithProxy is a MC bot with an integrated MC server that players can log into and control.

Have your accounts always online in-game and securely shared with your friends.

This project is also used for the [2b2t.vc API](https://api.2b2t.vc) and [Discord Bot](https://bot.2b2t.vc).

<details>
    <summary>What is a proxy?</summary>

    This proxy itself consists of two components:
    1. A Minecraft Server ("Proxy Server")
    2. A Minecraft Client ("Proxy Client")

    Players use a Minecraft client to connect to the Proxy Server as you would a normal MC server.
    The Proxy Client connects to a destination MC server (i.e. 2b2t.org).
    The Player's packets to the Proxy Server get forwarded to the Proxy Client which 
    forwards them to the destination MC server.
    
    Player MC Client -> Proxy Server -> Proxy Client -> MC Server
    
    When no Player Client is connected the Proxy Client will act 
    as a bot: moving around, chatting, etc.
</details>

<details>
    <summary>How does it work?</summary>

    ZenithProxy does not use, depend on, or interact with the Minecraft client or server code.
    This means much greater opportunities for optimizing memory usage and performance.
    
    But this also means existing MC mods or plugins cannot be used and must be
    reimplemented specifically for ZenithProxy.

    ZenithProxy acts primarily at the network packet layer. It can read/modify/cancel/send
    arbitrary packets in either direction at any time.

    Using packet interception, the client's session and world state is cached and 
    sent to players when they connect.

    The cached world state is also used to simulate player movements, 
    inventory actions, discord chat relay, and all features.
</details>

# Features

* High performance and efficiency on minimal hardware, <300MB RAM per java instance or <150MB on linux.
* Integrated ViaVersion
  * Can connect to (almost) any MC server and players can connect with (almost) any MC client
* Secure Whitelist system - share MC accounts without sharing passwords
* Extensive Discord Bot integration for management and monitoring
    * Chat relay
    * Customizable pings, alerts, and notifications. e.g. Player in visual range alerts
* Command System - Discord, In-game, and Terminal
  * 25+ commands and modules to configure every feature
* Spectator mode
  * Multiple players can connect to the proxy and spectate the player
* Advanced AntiAFK with full player movement simulation
* Integrated ReplayMod Recording
* Modules including AutoEat, AutoDisconnect, AutoReconnect, AutoRespawn, AutoTotem, KillAura, Spammer, AutoReply
* Many, many, more features.

# Getting Started

## Setup and Download

### System Requirements

1. Linux, Windows, or Mac computer. I recommend using a VPS (droplet) from DigitalOcean:
    * [$200 Free Digital](https://m.do.co/c/f3afffef9a46)[Ocean Credits](https://m.do.co/c/3a3a226e4936).
    * [Guide and automatic setup script](https://github.com/rfresh2/ZenithProxy/wiki/DigitalOcean-Setup-Guide).
2. Java 21+ (Not required for `linux` release channel on supported CPU)
    * The ZenithProxy launcher will install Java 21 automatically if not already installed

### Launcher

1. Download [the launcher zip](https://github.com/rfresh2/ZenithProxy/releases/launcher-v3) for your system
    * Windows Setup Guide: https://github.com/rfresh2/ZenithProxy/wiki/Windows-Python-Launcher-Guide
    * For other systems, download the Python version (Universal). Requires [Python 3.10+](https://www.python.org/downloads/)
2. Unzip the file.
3. Run the launcher executable in a terminal:
   * Linux/Mac: `./launch`
   * Python: `./launch.sh` (Linux/Mac) or `.\launch.bat` (Windows)

<details>
    <summary>How do I download a file from a Linux terminal?</summary>

* Use [wget](https://linuxize.com/post/wget-command-examples/#how-to-download-a-file-with-wget) in the terminal
* Example: `wget https://github.com/rfresh2/ZenithProxy/releases/download/launcher-v3/ZenithProxy-launcher-linux-amd64.zip`
</details>

<details> 
<summary>Recommended unzip tools</summary>

* Windows: [7zip](https://www.7-zip.org/download.html)
* Linux: [unzip](https://linuxize.com/post/how-to-unzip-files-in-linux/)
* Mac: [The Unarchiver](https://theunarchiver.com/)
</details>

<details>
    <summary>Recommended Terminals</summary>

* Windows: [Windows Terminal](https://apps.microsoft.com/detail/9N8G5RFZ9XK3)
* Mac: [iterm2](https://iterm2.com/)
</details>

### Run

* The launcher will ask for all configuration on first launch
    * Or run the launcher with the `--setup` flag. e.g. `.\launch.exe --setup`
* Use the `connect` command to link an MC account and log in once ZenithProxy is launched
* Command Prefixes:
    * Discord: `.` (e.g. `.help`)
    * In-game: `/` OR `!` -> (e.g. `/help`)
    * Terminal: N/A -> (e.g. `help`)
* [Full Commands Documentation](https://github.com/rfresh2/ZenithProxy/wiki/Commands)

### Pterodactyl Panel Support

ZenithProxy can be easily deployed using the [Pterodactyl Panel](https://pterodactyl.io/):

1. Import the [`egg-zenith.json`](https://github.com/rfresh2/ZenithProxy/blob/1.21.0/egg-zenith.json) file into your Pterodactyl Panel
2. Create a new server using the ZenithProxy egg
4. Start the server

### Running on Linux Servers

See the [Linux Guide](https://github.com/rfresh2/ZenithProxy/wiki/Linux-Guide)

I highly recommend using a terminal multiplexer - a program that manages terminal sessions. 

If you do not use one, **ZenithProxy will be killed after you exit your SSH session.**

* (Recommended) [tmux](https://tmuxcheatsheet.com/how-to-install-tmux/)
* [screen](https://linuxize.com/post/how-to-use-linux-screen/)
* [pm2](https://pm2.keymetrics.io/docs/usage/quick-start/)

## Configuration

### Release Channels

* (Default) `java` - Supports all operating systems
* (Recommended) `linux` - Linux native x86_64 executable. ~50% reduced memory usage and instant startup

### DNS Setup

* To use a domain name you need the following DNS records:
  * an `A` record to the public IP address of your server [Example](https://i.imgur.com/IvFhjhI.png)
  * an `SRV` record for `_minecraft._tcp` with the port and the `A` record as its target. [Example](https://i.imgur.com/D4XDGDF.png)

### Discord Bot Setup

* Create a discord bot here: https://discord.com/developers/
  * [Screenshots and how to get the bot's token](https://discordpy.readthedocs.io/en/stable/discord.html)
* Enable `Message Content Intent` under the "Bot" tab. [Example](https://i.imgur.com/iznLeDV.png)
* Invite the discord bot to a server:
  1. In the "OAuth2" tab, [generate an invite link with these permissions](https://imgur.com/rSn10ZN)
  2. Open the invite link in a web browser and select the server to invite the bot to
* Now in your discord server:
  1. In the [discord server settings](https://i.imgur.com/q8YQMJT.png), create [a role for users to manage the bot.](https://i.imgur.com/aJwE1Y8.png) Assign the role to yourself and any other users who should be able to manage the bot.
  1. Create a [channel to manage the proxy in](https://i.imgur.com/DVeJBpo.png)
  1. (Optional) create another channel for the chat relay
* At first launch, the launcher will ask you to configure the token/role/channel ID's (or you can use `discord` command after)
  * To get the ID's, you must enable [Developer Mode](https://i.imgur.com/qujvmiC.png) in your discord user settings
  * Right-click on the roles/channels you created and [click "Copy ID"](https://i.imgur.com/RDm3Gso.png)

## Running Multiple Instances

Create a new folder for each instance with its own copy of the launcher files. [Example](https://i.imgur.com/phKxmrL.png)

Instances must be independently run and configured. i.e. separate terminal sessions, discord bots, ports, config files, etc.

See the [Linux Guide](https://github.com/rfresh2/ZenithProxy/wiki/Linux-Guide) for help copying files, creating folders, etc.

### 2b2t Limits

2b2t limits accounts without priority queue based on:
1. Accounts currently connected per IP address
2. In-game session time, excluding time in queue.

Current limits are documented in [a discord channel](https://discord.com/channels/1127460556710883391/1200685719073599488)

### Development

I highly recommend using Intellij for building and running local development instances.

Gradle will automatically install the required Java version for compiling (currently Java 23)

Most useful gradle tasks:
* `run` - Builds and runs a local dev instance
* `jarBuild` - Builds an executable jar to `build/libs/ZenithProxy.jar`
* `build` - Compiles and runs tests without building a jar

## Special Thanks

* [Pork2b2tBot Contributors](https://github.com/PorkStudios/Pork2b2tBot/graphs/contributors)
* [MCProtocolLib Contributors](https://github.com/GeyserMC/MCProtocolLib/graphs/contributors)
