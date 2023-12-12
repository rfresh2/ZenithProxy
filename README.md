# ZenithProxy

<p align="center">
  <a href="https://discord.gg/nJZrSaRKtb">
  <img alt="Discord" src="https://dcbadge.vercel.app/api/server/nJZrSaRKtb">
  </a>
</p>

<p align="center">
  <img alt="Downloads" src="https://img.shields.io/github/downloads/rfresh2/ZenithProxy/total">
  <img src="https://img.shields.io/badge/MC-1.12.2-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.1-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.3-yellow.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/github/languages/code-size/rfresh2/ZenithProxy.svg" alt="Code size"/>
  <img src="https://img.shields.io/github/repo-size/rfresh2/ZenithProxy.svg" alt="GitHub repo size"/>
  <img src="https://tokei.rs/b1/github/rfresh2/ZenithProxy?category=code&style=flat" alt="Lines of Code"/>
</p>


Minecraft proxy/bot intended for use on 2b2t.org. 

Unlike a traditional MC bot, you can login to it like a normal MC server and control the account.

The primary purpose is to have accounts always online in-game and securely shared by multiple people.

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
* ViaVersion built in - can connect to any MC server version
* Secure Whitelist system - share MC accounts without sharing passwords
* Extensive Discord Bot integration
    * Chat relay
    * Player in visual range alerts
* Command System - Discord, In-game, and Terminal
  * 25+ commands and modules to configure every feature
* Spectator mode
  * Multiple players can connect to the proxy and spectate the player
* Advanced AntiAFK with full player movement simulation
* Modules including AutoEat, AutoDisconnect, AutoReconnect, AutoRespawn, AutoTotem, KillAura, Spammer, AutoReply
* Many, many, more features.

# Getting Started

## Setup and Download

1. Download `ZenithProxyLauncher.zip` [here](https://github.com/rfresh2/ZenithProxy/releases/launcher) and unzip to a new folder:
    * Linux CLI:
        * Download: `wget https://github.com/rfresh2/ZenithProxy/releases/download/launcher/ZenithProxyLauncher.zip`
        * Unzip: `unzip ZenithProxyLauncher.zip`
2. Open a terminal to the launcher folder and run `python3 setup.py` (if `python3` is not recognized, try `python` or `py`)

## System Requirements

1. Linux or Windows computer. I recommend DigitalOcean: 
   * [$200 Free Digital](https://m.do.co/c/f3afffef9a46)[Ocean Credits](https://m.do.co/c/3a3a226e4936).
   * [Guide and automatic setup script](https://github.com/rfresh2/ZenithProxy/wiki/DigitalOcean-Setup-Guide).
2. [Python 3.10+](https://www.python.org/downloads/)
3. Java 21+ (Not required for `linux` release channel on supported CPU)
   * Linux Installation: 
      * Install: https://sdkman.io/install
      * Run: `sdk install java 21-zulu`
   * Windows Installation: https://adoptium.net/

## Run

* `./launch.sh` (Linux) or `.\launch.bat` (Windows). Then use the `connect` command to log in
* Command Prefixes:
  * Discord: `.` (e.g. `.help`)
  * In-game: `!` -> (e.g. `!help`)
  * Terminal: N/A -> (e.g. `help`)

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

* Create a discord bot here: [discord.com/developers](https://discord.com/developers/)
* Enable `Message Content Intent` under the "Bot" tab. [Example](https://i.imgur.com/iznLeDV.png)
* Invite the discord bot to a server.
* Create a role for users to manage the proxy, a channel to manage the 
  proxy in, and a channel for the chat relay. The bot's role must have permissions to send and receive messages in both channels
* Run `setup.py` or configure with the `discord` command.

## Special Thanks

* [Pork2b2tBot Contributors](https://github.com/PorkStudios/Pork2b2tBot/graphs/contributors)
* [MCProtocolLib Contributors](https://github.com/GeyserMC/MCProtocolLib/graphs/contributors)
