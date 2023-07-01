# ZenithProxy

Discord Server: ([ZProxy](https://discord.gg/mf9uUvuUCU), [rProxy](https://discord.gg/Gvyb4g6c34))

Minecraft 1.12.2 proxy/bot. Primarily for use on 2b2t.org.

The primary purpose is to Minecraft accounts always online and be securely shared by multiple people.

This project also provides the data for the [2b2t.vc API](https://api.2b2t.vc) and [Discord Bot](https://bot.2b2t.vc).

# What is a proxy?

This proxy itself consists of two components:

1. A Minecraft Server ("Proxy Server")
2. A Minecraft Client ("Proxy Client")

Players use a Minecraft client to connect to the Proxy Server as you would a normal MC server.
The Proxy Client connects to a destination MC server (i.e. 2b2t.org).
The Player's packets to the Proxy Server get forwarded to the Proxy Client which forwards them to the destination
MC server.

Player MC Client -> Proxy Server -> Proxy Client -> MC Server

When no Player Client is connected the Proxy Client can still act as a bot: moving around, chatting, etc.

# How does it work?

The Proxy caches the client's world state including chunks, entities, other players, etc. to allow Player Clients to connect and disconnect at any time.

The Proxy is also able to read/modify/cancel/send arbitrary packets in either direction at any time. This is used to simulating 
player movements, spectator mode, discord chat relay, and more.

# Features

* Secure Whitelist system that allows sharing MC accounts without sharing passwords
* Extensive discord integration
    * Chat relay
    * Player in visual range alerts
    * 25+ commands and modules to configure every feature in the proxy
* Spectator mode
  * Multiple players can connect to the proxy and spectate the player as entities in-game or playercam
* Advanced AntiAFK with pathing and gravity
* 2b2t Queue Skip
  * Reconnects and skips the queue when the proxy is online for 6 hours (2b2t's max connection time)
* AutoUpdater that automatically pulls and restarts the proxy at convenient times
* Many, many, more features.

# Getting Started

## Prerequisites

1. Linux or Windows server. I recommend DigitalOcean `2 GB Memory / 1 AMD vCPU` in the NYC-1 region for
   minimal ping to 2b2t. 2b2t allows a maximum of 3 accounts concurrently connected per IP address.
2. JDK17 and git installed. JDK17's path must be set on the JAVA_HOME environment variable.

## Setup

1. `git clone git@github.com:rfresh2/ZenithProxy.git`
2. Execute in CLI: `./start.sh` (Linux) or `./start.bat` (Windows). Close the instance with `CTRL-C`.
3. Edit `config.json`. Important sections:
  * `authentication` -> Input your Minecraft account details
    * You must disable 2FA on your Microsoft account and may need to approve the proxy's
      IP [here](https://account.live.com/Activity)
  * `server` -> Optionally change the port the proxy listens on.
  * `proxyIP` -> set this to DNS name or IP address + port clients should connect to.
    * For DNS you need the following records:
        * an `A` record pointing to the proxy IP
        * an `SRV` record pointing to the `A` record and port. e.g. `0 5 25565 rfresh.proxy.com`
  * `discord`
      * Create a discord bot here `https://discord.com/developers/`. `Message Content Intent` MUST be enabled.
          * Invite the discord bot to a server. Create a role for users to manage the proxy, a channel to manage the
            proxy
            in, and a channel for the chat relay.
      * `token` -> discord bot token
    * `channelId` -> the channel ID where you will manage the proxy
    * `enable` -> set to true if you wish to use discord (highly recommended)
    * `accountOwnerRoleId` -> a discord role ID that allows users to manage sensitive configuration like the whitelist
    * `chatRelay.channelId` -> The channel where the MC server chat relay will be sent and received from.

## Run

* `./start.sh` (Linux) or `./start.bat` (Windows) or `./gradlew run` (Universal)
* Use `.help` in discord to get a list of available commands.
    * `.connect` and `.disconnect` will login/disconnect the proxy from the MC server.

# Developers

* [rfresh2](https://github.com/rfresh2)
* [odpay](https://github.com/odpay)

## Special Thanks

* [Pork2b2tBot Developers](https://github.com/PorkStudios/Pork2b2tBot/graphs/contributors)
* [MCProtocolLib Developers](https://github.com/GeyserMC/MCProtocolLib/graphs/contributors)

# License

This project was originally forked from [Pork2b2tBot](https://github.com/PorkStudios/Pork2b2tBot) and carries the same
MIT license
