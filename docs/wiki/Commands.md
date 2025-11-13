# ZenithProxy Commands Documentation

## Command Prefixes

#### Discord

`.` (e.g. `.help`)

#### In-game

`/` OR `!` (e.g. `/help`)

#### Terminal

N/A (e.g. `help`)

## Core Commands

### connect

Connects ZenithProxy to the destination MC server

**Aliases:** `c`

**Usage**

  ```connect```


### disconnect

Disconnects ZenithProxy from the destination MC server

**Aliases:** `dc`

**Usage**

  ```disconnect```


### help

ZenithProxy command list

**Aliases:** `h`

**Usage**

  ```help```

  ```help <category>```

  ```help <command>```


### spectator

Configures the Spectator feature.



The spectator whitelist only allows players to join as spectators.

Players who are regular whitelisted (i.e. with the `whitelist` command) can always join as spectators regardless.



Spectator entities control what entity is used to represent spectators in-game.



Full commands allow spectators access to all standard ZenithProxy commands like `connect`, `disconnect`, etc.

If this is disabled, spectators only have access to a limited set of core commands.

**Usage**

  ```spectator on/off```

  ```spectator whitelist add/del <player>```

  ```spectator whitelist addAll <player 1>,<player 2>...```

  ```spectator whitelist list```

  ```spectator whitelist clear```

  ```spectator entity list```

  ```spectator entity <entity>```

  ```spectator chat on/off```

  ```spectator playerCamOnJoin on/off```

  ```spectator fullCommands on/off```

  ```spectator fullCommands slashCommands on/off```

  ```spectator fullCommands requireRegularWhitelist on/off```


### status

Prints the current status of ZenithProxy, the in-game player, and modules.

**Aliases:** `s`

**Usage**

  ```status```

  ```status modules```


### update

Restarts and updates ZenithProxy if `autoUpdate` is enabled

**Aliases:** `restart` / `shutdown` / `reboot`

**Usage**

  ```update```


### whitelist

Manages the list of players allowed to login.



Whitelisted players are allowed to both control the account in-game and spectate.



`autoAddZenithAccount` will add the MC account you have logged in Zenith with to the whitelist.



Blacklist is only used and shown if the whitelist or spectator whitelist is disabled (see the `unsupported` command`)

**Aliases:** `wl`

**Usage**

  ```whitelist add/del <player>```

  ```whitelist addAll <player 1>,<player 2>...```

  ```whitelist list```

  ```whitelist clear```

  ```whitelist autoAddZenithAccount on/off```

  ```whitelist blacklist add/del <player>```

  ```whitelist blacklist clear```



## Manage Commands

### auth

Configures the proxy's authentication settings.



To switch accounts, use the `clear` command.



`attempts` configures the number of login attempts before wiping the cache.



`alwaysRefreshOnLogin` will always refresh the token on login instead of trusting the cache. This can cause

Microsoft to rate limit your account. Auth tokens will always refresh in the background even if this is off.



`deviceCode` is the default and recommended authentication type.

If authentication fails, try logging into the account on the vanilla MC launcher and joining a server. Then try again in Zenith.

If this still fails, try one of the alternate auth types.

**Usage**

  ```auth clear```

  ```auth attempts <int>```

  ```auth alwaysRefreshOnLogin on/off```

  ```auth type <deviceCode/emailAndPassword/deviceCode2/prism>```

  ```auth email <email>```

  ```auth password <password>```

  ```auth mention on/off```

  ```auth openBrowser on/off```

  ```auth maxRefreshIntervalMins <minutes>```

  ```auth useClientConnectionProxy on/off```

  ```auth chatSigning on/off```

  ```auth chatSigning force on/off```

  ```auth chatSigning commands on/off```


### autoUpdate

Configures the AutoUpdater.



Updates are not immediately applied while the client is connected.

When an update is found, it will be applied 30 seconds after the next disconnect, or immediately if already disconnected.

**Usage**

  ```autoUpdate on/off```


### chatRelay

Configures the Discord ChatRelay feature.



The ChatRelay is a live feed of chat messages and/or connection messages from the server to a Discord channel.



Mentions can be configured when a whisper is received or your name is seen in chat.



Messages typed in the ChatRelay discord channel will be sent as chat messages in-game

Discord message replies will be sent as whispers in-game.



Ignore regex will filter out messages, see here for help writing regex: https://regex101.com/ (Java flavor)

**Usage**

  ```chatRelay on/off```

  ```chatRelay channel <channelId>```

  ```chatRelay connectionMessages on/off```

  ```chatRelay whispers on/off```

  ```chatRelay publicChat on/off```

  ```chatRelay deathMessages on/off```

  ```chatRelay serverMessages on/off```

  ```chatRelay whisperMentions on/off```

  ```chatRelay nameMentions on/off```

  ```chatRelay mentionsWhileConnected on/off```

  ```chatRelay ignoreQueue on/off```

  ```chatRelay sendMessages on/off```

  ```chatRelay ignoreRegex add <regex>```

  ```chatRelay ignoreRegex del <index>```

  ```chatRelay ignoreRegex list```

  ```chatRelay ignoreRegex clear```


### chatSchema

Configure how ZenithProxy parses public chats and whispers.



Correct schemas are needed for chat relay and chat based features to work correctly.



Schemas have the following special tokens:

* $s -> Chat/whisper sender, player name

* $r -> Whisper receiver, player name

* $m -> Message, text content of the chat/whisper

* $w -> Wildcard, any varying text, e.g. a role prefix `[ADMIN] rfresh2: test message`



Example 2b2t chat schema:

* public chat: `<$s> $m`

* whisper inbound: `$s whispers: $m`

* whisper outbound: `to $r: $m`



You can configure different schemas for different servers based on the server address.



Server address is without port, e.g. `connect.2b2t.org` or `192.168.0.5`

**Usage**

  ```chatSchema set <publicChat/whisperInbound/whisperOutbound> <serverAddress> <schema>```

  ```chatSchema preset <serverAddress> <2b2t/essentials>```

  ```chatSchema del <serverAddress>```

  ```chatSchema list```


### clientConnection

Manages the connection configuration from ZenithProxy to the destination MC server.

**Usage**

  ```clientConnection autoConnect on/off```

  ```clientConnection proxy on/off```

  ```clientConnection proxy type <type>```

  ```clientConnection proxy host <host>```

  ```clientConnection proxy port <port>```

  ```clientConnection proxy user <user>```

  ```clientConnection proxy password <password>```

  ```clientConnection proxy auth clear```

  ```clientConnection bindAddress <address>```

  ```clientConnection timeout on/off```

  ```clientConnection timeout <seconds>```

  ```clientConnection ping packetInterval <seconds>```

  ```clientConnection keepAlive mode <passthrough/independent>```

  ```clientConnection keepAlive queueTimeout <ms>```


### commandConfig

Configures ZenithProxy command prefixes and settings.

**Usage**

  ```commandConfig discord prefix <string>```

  ```commandConfig ingame on/off```

  ```commandConfig ingame slashCommands on/off```

  ```commandConfig ingame slashCommands replaceServerCommands on/off```

  ```commandConfig ingame slashCommands suggestions on/off```

  ```commandConfig ingame prefix <string>```

  ```commandConfig ingame allowWhitelistedToUseAccountOwnerCommands on/off```


### database

Configures the database module used for https://api.2b2t.vc



This is disabled by default. No ZenithProxy users contribute or collect data, this is purely for use with my own accounts.

**Aliases:** `db`

**Usage**

  ```database on/off```

  ```database host <host>```

  ```database port <port>```

  ```database username <username>```

  ```database password <password>```

  ```database redis address <address>```

  ```database redis username <username>```

  ```database redis password <password>```

  ```database queueWait on/off```

  ```database queueLength on/off```

  ```database publicChat on/off```

  ```database joinLeave on/off```

  ```database deathMessages on/off```

  ```database restarts on/off```

  ```database playerCount on/off```

  ```database tablist on/off```

  ```database playtime on/off```

  ```database time on/off```


### debug

Debug settings for features in testing or for use in development.

**Usage**

  ```debug sync inventory```

  ```debug sync chunks```

  ```debug clearEffects```

  ```debug packetLog on/off```

  ```debug packetLog client on/off```

  ```debug packetLog server on/off```

  ```debug packetLog filter <string>```

  ```debug kickDisconnect on/off```

  ```debug dc```

  ```debug debugLogs on/off```

  ```debug chunkCacheFullbright on/off```

  ```debug defaultClientRenderDistance <int>```

  ```debug lockFile on/off```

  ```debug uploadLog```

  ```debug uploadDebugLog```

  ```debug passthroughResourcePacks on/off```


### discord

Manages the Discord bot's configuration.



The relay is configured using the `chatRelay` command

**Usage**

  ```discord on/off```

  ```discord channel <channel ID>```

  ```discord token <token>```

  ```discord role <role ID>```

  ```discord relayChannel <channelId>```

  ```discord manageProfileImage on/off```

  ```discord manageNickname on/off```

  ```discord manageDescription on/off```

  ```discord managePresence on/off```

  ```discord showNonWhitelistIP on/off```

  ```discord ignoreOtherBots on/off```


### displayCoords

Configures whether the discord bot's messages should display coordinates. Only usable by account owner(s).

**Aliases:** `coords`

**Usage**

  ```displayCoords on/off```


### friend

Manage the friend list.

Friends change behavior for various modules like VisualRange, KillAura, and AutoDisconnect

**Aliases:** `f`

**Usage**

  ```friend add/del <player>```

  ```friend addAll <player 1>,<player 2>...```

  ```friend list```

  ```friend clear```


### jvmArgs

Configures ZenithProxy's JVM arguments used by the launcher.



By default, this is empty and a set of default JVM arguments are used.



The primary arg to configure is `-Xmx` which sets the maximum memory heap size.



The default `-Xmx` used by the launcher depends on the `java` or `linux` release channel:

* `java`: 300M

* `linux`: 200M



You should only need to increase this if the server view distance is > 15.



Be warned, changing this setting can cause ZenithProxy to be unable to restart. You will need to manually

edit the `launch_config.json` to fix this if that happens.

**Usage**

  ```jvmArgs reset```

  ```jvmArgs get```

  ```jvmArgs setXmx <megabytes>```

  ```jvmArgs set <args>```


### kick

Kicks all players or a specific player. Only usable by account owners.

**Usage**

  ```kick```

  ```kick <player>```


### plugins

[BETA]



Configures the ZenithProxy plugin manager.



Plugins are user-created add-ons that add modules and commands.



Plugins are only supported on the `java` release channel.

**Aliases:** `plugin`

**Usage**

  ```plugins on/off```

  ```plugins list```

  ```plugins download <url>```

  ```plugins remove <pluginId>```


### reconnect

Disconnect and reconnects from the destination MC server.



Can be used to perform a reconnect "queue skip" on 2b2t

**Usage**

  ```reconnect```


### channel

Configures the current AutoUpdater release channel.



The release channel is a combination of a platform (java or linux) and a Minecraft protocol version.

**Aliases:** `release` / `releaseChannel`

**Usage**

  ```channel list```

  ```channel set <platform> <minecraft version>```


### server

Change the MC server ZenithProxy connects to.

**Usage**

  ```server <IP>```

  ```server <IP> <port>```


### serverConnection

Configures the MC server hosted by Zenith and players' connections to it



The `proxyIP` is the IP players should connect to. This is purely informational.



The `port` argument changes the port the ZenithProxy MC server listens on



`upnp` will try to open the port to the public internet, useful for self-hosting on a home network



The `ping` arguments configure the server list ping response ZenithProxy sends to players.

`onlinePlayers` = MC profiles of players

`onlinePlayerCount` = number of players connected

`maxPlayers` = number of players that can connect

`lanBroadcast` = LAN server broadcast

`log` = logs pings



The `timeout` arguments configures how long until players are kicked due no packets being received.

**Usage**

  ```serverConnection proxyIP <ip>```

  ```serverConnection port <port>```

  ```serverConnection upnp on/off```

  ```serverConnection ping on/off```

  ```serverConnection ping onlinePlayers on/off```

  ```serverConnection ping onlinePlayerCount on/off```

  ```serverConnection ping maxPlayers <int>```

  ```serverConnection ping lanBroadcast on/off```

  ```serverConnection ping log on/off```

  ```serverConnection enforceMatchingConnectingAddress on/off```

  ```serverConnection timeout on/off```

  ```serverConnection timeout <seconds>```

  ```serverConnection autoConnectOnLogin on/off```

  ```serverConnection updateServerIcon on/off```

  ```serverConnection chatSigning mode <disguised/passthrough/system>```


### spectatorEntity

Changes the current spectator entity. Only usable by spectators

**Aliases:** `e`

**Usage**

  ```spectatorEntity```

  ```spectatorEntity <entity>```


### entityToggle

Toggles the visibility of spectator entities. Only usable by spectators.

**Aliases:** `etoggle`

**Usage**

  ```entityToggle```


### playerCam

Toggles spectators between player and entity cameras. Only usable by spectators

**Usage**

  ```playerCam```


### theme

Changes the color theme of alerts and messages.



Use `theme list` to see available colors.



Where Colors Are Used:

  * Primary: Most embeds and command responses if not an error.

  * Success: General "this worked" responses, server join, and friends

  * Error: Error responses, server leave, and enemies

  * In Queue: The proxy is in queue, reconnecting, or is in a transitional state

**Aliases:** `color`

**Usage**

  ```theme list```

  ```theme primary <color>```

  ```theme success <color>```

  ```theme error <color>```

  ```theme inQueue <color>```


### transfer

Transfers connected players to a destination MC server

**Usage**

  ```transfer <address>```


### unsupported

Unsupported settings that cause critical security issues.



Do not use edit these unless you absolutely understand what you are doing.



No user support will be provided if you modify any of these settings.



All subcommands are only usable from the terminal.

**Usage**

  ```unsupported whitelist on/off```

  ```unsupported spectatorWhitelist on/off```

  ```unsupported allowOfflinePlayers on/off```

  ```unsupported auth type offline```

  ```unsupported auth offlineUsername <username>```



## Info Commands

### connectionTest

Tests whether this proxy or another MC server is accessible from the public internet.



If the test succeeds, that means other people can connect.



If the test fails, either the `proxyIP` setting is not set to a public IP address or your instance is not

exposed on the public internet.



To configure the `proxyIP` use the `help serverConnection` command



On a VPS this is usually due to a firewall needing to be disabled.



On a home PC you would need both disable any firewall and configure port forwarding in your router.

**Usage**

  ```connectionTest```

  ```connectionTest <address>```

  ```connectionTest testOnStart on/off```


### discordNotifications

Configures various discord notifications regarding player and proxy connections, deaths, and more.

**Aliases:** `alerts` / `notifications`

**Usage**

  ```discordNotifications role set <roleId>```

  ```discordNotifications role reset```

  ```discordNotifications connect mention on/off```

  ```discordNotifications online mention on/off```

  ```discordNotifications disconnect mention on/off```

  ```discordNotifications startQueue mention on/off```

  ```discordNotifications death mention on/off```

  ```discordNotifications serverRestart mention on/off```

  ```discordNotifications loginFailed mention on/off```

  ```discordNotifications clientConnect mention on/off```

  ```discordNotifications clientDisconnect mention on/off```

  ```discordNotifications spectatorConnect mention on/off```

  ```discordNotifications spectatorDisconnect mention on/off```

  ```discordNotifications nonWhitelistedConnect mention on/off```

  ```discordNotifications mcVersionMismatchWarning on/off```


### inventory

Show and interact with the player's inventory or containers.

**Aliases:** `inv`

**Usage**

  ```inventory```

  ```inventory show```

  ```inventory hold <slot>```

  ```inventory swap <from> <to>```

  ```inventory drop <slot>```

  ```inventory drop stack <slot>```

  ```inventory close```

  ```inventory withdraw```

  ```inventory deposit```

  ```inventory click <left/right> <slot>```

  ```inventory button <buttonId>```

  ```inventory actionDelayTicks <ticks>```

  ```inventory ncpStrict on/off```

  ```inventory autoCloseOpenContainers on/off```

  ```inventory autoCloseOpenContainers delaySeconds <seconds>```


### license

Displays the software license and information about your legal rights

**Usage**

  ```license```


### map

Generate and render map images.

Map ID's to render must be cached during the current session

Generated maps can optionally be aligned to the vanilla map grid, or generated with a custom view distance.

Generated maps cannot be larger than what chunks are currently cached in the proxy

**Usage**

  ```map render <mapId>```

  ```map render all```

  ```map generate```

  ```map generate align```

  ```map generate <viewDistance>```


### playtime

Gets the playtime of a player on 2b2t using https://api.2b2t.vc/

**Aliases:** `pt`

**Usage**

  ```playtime <playerName>```


### prio

Configure alerts for 2b2t priority queue status and bans

**Usage**

  ```prio mentions on/off```

  ```prio banMentions on/off```

  ```prio banCheck```

  ```prio banCheck <playerName>```


### queueStatus

Gets the current 2b2t queue length and wait ETA

**Aliases:** `queue` / `q`

**Usage**

  ```queueStatus```

  ```queueStatus refresh```

  ```queueStatus refresh interval <minutes>```

  ```queueStatus refresh eta on/off```

  ```queueStatus refresh whileNotOn2b2t on/off```


### queueWarning

Configure warnings sent when 2b2t queue positions are reached.



The list of queue positions to send the warnings can be configured, each with an optional mention.

**Usage**

  ```queueWarning on/off```

  ```queueWarning list```

  ```queueWarning clear```

  ```queueWarning add <position>```

  ```queueWarning add <position> mention```

  ```queueWarning del <position>```


### raycast

Debug testing command. Gets the block or entity the player is currently looking at.

**Usage**

  ```raycast```


### seen

Gets the first and last times a player was seen on 2b2t using https://api.2b2t.vc

**Aliases:** `firstseen` / `lastseen`

**Usage**

  ```seen <playerName>```


### stats

Gets the 2b2t stats of a player using https://api.2b2t.vc

**Usage**

  ```stats <playerName>```


### tablist

Prints the current MC server's player list

**Aliases:** `tab`

**Usage**

  ```tablist```

  ```tablist header```


### waypoints

Save and manage waypoints.



Waypoints can be used as pathfinder goals:

`b goto <waypointId>`

`b click <left/right> <waypointId>`

**Usage**

  ```waypoints add <id> <x> <y> <z>```

  ```waypoints add <id> <dimension> <x> <y> <z>```

  ```waypoints del <id>```

  ```waypoints clear```

  ```waypoints list```



## Module Commands

### actionLimiter

Limits player actions and movements.



Players who login with the same account as the one used by ZenithProxy will be immune to these restrictions.



If the movement limits are reached by a player, they will be disconnected while the proxy account will stay logged in.



Other limits do not disconnect players and instead cancel the actions.

**Aliases:** `al`

**Usage**

  ```actionLimiter on/off```

  ```actionLimiter allowMovement on/off```

  ```actionLimiter movementDistance <distance>```

  ```actionLimiter movementHome <x> <z>```

  ```actionLimiter movementMinY <y>```

  ```actionLimiter allowInventory on/off```

  ```actionLimiter allowBlockBreaking on/off```

  ```actionLimiter allowInteract on/off```

  ```actionLimiter allowEnderChest on/off```

  ```actionLimiter allowUseItem on/off```

  ```actionLimiter allowBookSigning on/off```

  ```actionLimiter allowChat on/off```

  ```actionLimiter allowServerCommands on/off```

  ```actionLimiter allowRespawn on/off```

  ```actionLimiter itemsBlacklist on/off```

  ```actionLimiter itemsBlacklist add/del <item>```

  ```actionLimiter itemsBlacklist addAll <item 1>,<item 2>...```

  ```actionLimiter itemsBlacklist clear```

  ```actionLimiter itemsBlacklist list```


### activeHours

Set times for ZenithProxy to automatically connect at.



By default, 2b2t's queue wait ETA is used to determine when to log in.

The connect will occur when the current time plus the ETA is equal to a time set.



If Queue ETA calc is disabled, connects will occur exactly at the set times instead.



 Time zone Ids ("TZ Identifier" column): https://w.wiki/A2fd

 Time format: hh:mm, Examples: 1:42, 14:42, 14:01

**Usage**

  ```activeHours on/off```

  ```activeHours timezone <timezone ID>```

  ```activeHours add/del <time>```

  ```activeHours status```

  ```activeHours whilePlayerConnected on/off```

  ```activeHours queueEtaCalc on/off```

  ```activeHours fullSessionUntilNextDisconnect on/off```


### antiAFK

Configures the AntiAFK module.



To avoid being kicked on 2b2t the only required action is swing OR walk.



The walk action will move the player roughly in a square shape. To avoid falling down any ledges, enable safeWalk



For delay settings, 1 tick = 50ms

**Aliases:** `afk`

**Usage**

  ```antiAFK on/off```

  ```antiAFK rotate on/off```

  ```antiAFK rotate delay <ticks>```

  ```antiAFK swing on/off```

  ```antiAFK swing delay <ticks>```

  ```antiAFK walk on/off```

  ```antiAFK walk delay <ticks>```

  ```antiAFK safeWalk on/off```

  ```antiAFK walkDistance <ticks>```

  ```antiAFK jump on/off```

  ```antiAFK jump onlyInWater on/off```

  ```antiAFK jump delay <ticks>```

  ```antiAFK sneak on/off```

  ```antiAFK sneak delay <ticks>```


### antiKick

AntiKick kicks players controlling the proxy client if they are inactive for a set amount of time.



Inactivity is defined as not moving, fishing, or swinging - which are what prevents 2b2t from kicking players.

**Usage**

  ```antiKick on/off```

  ```antiKick playerInactivityKickMins <minutes>```

  ```antiKick minWalkDistance <blocks>```


### antiLeak

Configures the AntiLeak module. Cancels chat packets that could leak your coordinates.

i.e. due to inputting incorrect baritone commands, sharing waypoints, etc.



rangeCheck -> only cancels if the numbers in the chat message are within a range of your current coordinates.

rangeFactor -> How near the coordinates in your chat have to be to actual coords to be cancelled.



Equation: `actualCoord / rangeFactor < chatCoord < actualCoord * rangeFactor`

Example: If your coordinates are [500, 800], rangeFactor=10 will cancel if the chat contains a number between 50-5000 or 80-8000.

**Usage**

  ```antiLeak on/off```

  ```antiLeak rangeCheck on/off```

  ```antiLeak rangeFactor <number>```


### autoArmor

Automatically equips the best armor in your inventory

**Usage**

  ```autoArmor on/off```


### autoDisconnect

Configures the AutoDisconnect module.



Every mode and setting requires the module to be enabled to be active.



Modes:



  * Health: Disconnects when health is below a set threshold level

  * Thunder: Disconnects during thunderstorms (i.e. avoid lightning burning down bases)

  * Unknown Player: Disconnects when a player not on the friends list, whitelist, or spectator whitelist is in visual range

  * TotemPop: Disconnects when your totem is popped

Multiple modes can be enabled, they are non-exclusive



Settings non-exclusive to modes:

  * WhilePlayerConnected: If AutoDisconnect should disconnect while a player is controlling the proxy account

  * AutoClientDisconnect: Disconnects when the controlling player disconnects

  * CancelAutoReconnect: Cancels AutoReconnect when AutoDisconnect is triggered. If the proxy account has prio this is ignored and AutoReconnect is always cancelled

**Aliases:** `autoLog`

**Usage**

  ```autoDisconnect on/off```

  ```autoDisconnect health on/off```

  ```autoDisconnect health <integer>```

  ```autoDisconnect thunder on/off```

  ```autoDisconnect unknownPlayer on/off```

  ```autoDisconnect totemPop on/off```

  ```autoDisconnect totemPop minTotemsRemaining <count>```

  ```autoDisconnect whilePlayerConnected on/off```

  ```autoDisconnect autoClientDisconnect on/off```

  ```autoDisconnect cancelAutoReconnect on/off```


### autoDrop

Automatically drop items in player inventory.



Dropping can be configured based on modes:



    * `all`: any item

    * `whitelist`: only added items

    * `blacklist`: any item not added

**Usage**

  ```autoDrop on/off```

  ```autoDrop mode <all/whitelist/blacklist>```

  ```autoDrop add/del <item>```

  ```autoDrop addAll <item1>,<item2>,...```

  ```autoDrop list```

  ```autoDrop clear```

  ```autoDrop delay <ticks>```

  ```autoDrop dropStack on/off```

  ```autoDrop rotation on/off```

  ```autoDrop rotation sync```

  ```autoDrop rotation <yaw> <pitch>```


### autoEat

Automatically eats food when health or hunger is below a set threshold.

**Usage**

  ```autoEat on/off```

  ```autoEat health <int>```

  ```autoEat hunger <int>```

  ```autoEat warning on/off```

  ```autoEat allowUnsafeFood on/off```


### autoFish

Automatically fishes, both casting and reeling.



AutoFishing will prevent you from being AFK kicked. It's recommended to disable AntiAFK.

**Usage**

  ```autoFish on/off```

  ```autoFish rotation <yaw> <pitch>```

  ```autoFish rotation sync```


### autoMend

Equips items that are both damaged and have the mending enchantment to the offhand.



Can be enabled while at an XP farm to repair items in your inventory.

**Usage**

  ```autoMend on/off```


### autoOmen

Automatically drinks Bad Omen potions in the inventory.



Useful for raid farms on MC 1.21+ servers.

**Usage**

  ```autoOmen on/off```

  ```autoOmen whileRaidActive on/off```

  ```autoOmen whileOmenActive on/off```


### autoReconnect

Automatically reconnects the bot when it is disconnected.

**Usage**

  ```autoReconnect on/off```

  ```autoReconnect delay <seconds>```

  ```autoReconnect maxAttempts <number>```


### autoReply

Automatically replies to whispers with a custom message.

**Usage**

  ```autoReply on/off```

  ```autoReply cooldown <seconds>```

  ```autoReply message <message>```


### autoRespawn

Automatically respawns the player after dying.

**Usage**

  ```autoRespawn on/off```

  ```autoRespawn delay <milliseconds>```


### autoTotem

Automatically equips a totem from the inventory to the offhand when the bot's health is below a set threshold.

**Usage**

  ```autoTotem on/off```

  ```autoTotem inGame on/off```

  ```autoTotem health <int>```

  ```autoTotem popAlert on/off```

  ```autoTotem popAlert mention on/off```

  ```autoTotem noTotemsAlert on/off```

  ```autoTotem noTotemsAlert mention on/off```


### chatHistory

Caches and sends recent chat history to players and spectators who connect to the proxy.

Includes whispers, chat, and system messages.

**Usage**

  ```chatHistory on/off```

  ```chatHistory seconds <seconds>```

  ```chatHistory maxCount <maxCount>```

  ```chatHistory spectators on/off```


### click

Simulates a click to the block or entity in front of you

**Usage**

  ```click left```

  ```click left target <any/none/entity/block>```

  ```click left hold```

  ```click left hold interval <ticks>```

  ```click right```

  ```click right target <any/none/entity/block>```

  ```click right hold```

  ```click right hold <mainHand/offHand/alternate>```

  ```click right hold interval <ticks>```

  ```click addedBlockReach <float>```

  ```click addedEntityReach <float>```

  ```click hold forceRotation on/off```

  ```click hold forceRotation <yaw> <pitch>```

  ```click hold sneak on/off```

  ```click hold target <any/none/entity/block>```

  ```click stop```


### coordObf

[BETA]



Obfuscates actual coordinates to players and spectators.



Designed specifically for 2b2t, to let players you don't trust to visit your base/stash



How it works:

* For each player, a chunk coordinate offset is generated

* Packets that contain coordinates are modified with that offset

* Respawns/server switches will regenerate the offset or disconnect the player

* Various exploits like bedrock patterns, eye of ender triangulation, and beehive data are blocked



It is highly recommended to use this in conjunction with the `actionLimiter` module



You should avoid allowing players to travel or respawn to 0,0, visit the worldborder, or any other known landmarks

to avoid leaking the offset.



There are multiple modes for how the offset is generated. Random is recommended as it reduces the likelihood

and impact of the offset being discovered.

**Usage**

  ```coordObf on/off```

  ```coordObf mode <random/constant/atLocation>```

  ```coordObf regenOnTpMinDistance <blocks>```

  ```coordObf randomMinDistanceFromSelf <blocks>```

  ```coordObf randomMinDistanceFromSpawn <blocks>```

  ```coordObf randomMaxDistanceFromSpawn <blocks>```

  ```coordObf constantOffset <x> <z>```

  ```coordObf constantOffsetNetherTranslate on/off```

  ```coordObf constantOffsetMinSpawnDistance <blocks>```

  ```coordObf atLocation <x> <z>```

  ```coordObf obfuscateBedrock on/off```

  ```coordObf obfuscateBiomes on/off```

  ```coordObf obfuscateBiomesKey <biomeId>```

  ```coordObf obfuscateLighting on/off```

  ```coordObf eyeOfEnderDisconnect on/off```

  ```coordObf blockOffsetsDisconnect on/off```

  ```coordObf validateSetup on/off```


### extraChat

Hide certain types of messages in-game or in the terminal chat log.

**Usage**

  ```extraChat on/off```

  ```extraChat hideChat on/off```

  ```extraChat hideWhispers on/off```

  ```extraChat hideDeathMessages on/off```

  ```extraChat showConnectionMessages on/off```

  ```extraChat insertClickableLinks on/off```

  ```extraChat hide2b2tActionBarText on/off```

  ```extraChat logChatMessages on/off```

  ```extraChat logOnlyQueuePositionUpdates on/off```

  ```extraChat whisperCommand <command>```


### ignore

Hides chat and death messages and notifications from a configured list of players.

**Usage**

  ```ignore add/del <player>```

  ```ignore addAll <player 1>,<player 2>...```

  ```ignore list```

  ```ignore clear```


### killAura

Attacks entities near the player.



Custom targets list: https://link.2b2t.vc/1



Aggressive mobs are mobs that are actively targeting and attacking the player.

**Aliases:** `ka`

**Usage**

  ```killAura on/off```

  ```killAura attackDelay <ticks>```

  ```killAura tpsSync on/off```

  ```killAura targetPlayers on/off```

  ```killAura targetHostileMobs on/off```

  ```killAura targetHostileMobs onlyAggressive on/off```

  ```killAura targetNeutralMobs on/off```

  ```killAura targetNeutralMobs onlyAggressive on/off```

  ```killAura targetArmorStands on/off```

  ```killAura targetCustom on/off```

  ```killAura targetCustom add/del <entityType>```

  ```killAura weaponSwitch on/off```

  ```killAura weaponType <any/sword/axe>```

  ```killAura weaponMaterial <any/diamond/netherite>```

  ```killAura raycast on/off```

  ```killAura priority <none/nearest>```


### modulePriority

Configures the priority of ZenithProxy modules.



A higher priority means the module's actions (like rotations, clicks, etc.) will take precedence over modules with lower priorities.



Should not be edited normally, only for advanced use cases.



Default module priorities may be changed between versions.

**Usage**

  ```modulePriority autoTotem <default/int>```

  ```modulePriority autoArmor <default/int>```

  ```modulePriority autoEat <default/int>```

  ```modulePriority autoOmen <default/int>```

  ```modulePriority click <default/int>```

  ```modulePriority killAura <default/int>```

  ```modulePriority pathfinder <default/int>```

  ```modulePriority spawnPatrol <default/int>```

  ```modulePriority antiAfk <default/int>```

  ```modulePriority autoMend <default/int>```

  ```modulePriority autoDrop <default/int>```

  ```modulePriority autoFish <default/int>```

  ```modulePriority spook <default/int>```

  ```modulePriority list```


### pathfinder

Baritone pathfinder

**Aliases:** `path` / `b`

**Usage**

  ```pathfinder goto <x> <z>```

  ```pathfinder goto <x> <y> <z>```

  ```pathfinder goto <waypointId>```

  ```pathfinder stop```

  ```pathfinder follow```

  ```pathfinder follow <playerName>```

  ```pathfinder thisway <blocks>```

  ```pathfinder getTo <block>```

  ```pathfinder mine <block>```

  ```pathfinder click <left/right> <x> <y> <z>```

  ```pathfinder click <left/right> <waypointId>```

  ```pathfinder click <left/right> entity <type>```

  ```pathfinder break <x> <y> <z>```

  ```pathfinder place <x> <y> <z> <item>```

  ```pathfinder pickup```

  ```pathfinder pickup <item>```

  ```pathfinder clearArea <pos1> <pos2>```

  ```pathfinder status```

  ```pathfinder settings```


### pearlLoader

Loads player's pearls.



Positions must be of interactable blocks like levers, buttons, trapdoors, etc.



They should be unobstructed and reachable.

**Aliases:** `pl`

**Usage**

  ```pearlLoader add <id> <x> <y> <z>```

  ```pearlLoader del <id>```

  ```pearlLoader load <id>```

  ```pearlLoader list```

  ```pearlLoader returnToStartPos on/off```


### rateLimiter

Limits how often players are allowed to attempt logins and send packets.



The rate limit is counted as the number of seconds between logins allowed.

The login rate limit is per IP address.



The packet rate limiter is counted per connection.

Packets received are counted in a configurable interval of seconds.

If more packets than the rateLimit are received in that interval, the player is disconnected.

**Usage**

  ```rateLimiter login on/off```

  ```rateLimiter login rateLimit <seconds>```

  ```rateLimiter packet on/off```

  ```rateLimiter packet interval <seconds>```

  ```rateLimiter packet rateLimit <int>```


### replay

Captures a ReplayMod recording.



Replays can optionally be uploaded to discord if they are under the discord message size limit.



If a replay is too large for discord, it can be uploaded to https://file.io instead if `fileIoUpload` is enabled.



A `maxRecordingTime` of 0 means there is no limit, however, recording are always stopped on disconnects.



`autoRecord mode <mode` can automatically record while certain conditions are met.



Additional recording modes can be configured in the `visualRange` command.

**Usage**

  ```replay start```

  ```replay stop```

  ```replay discordUpload on/off```

  ```replay fileIoUpload on/off```

  ```replay maxRecordingTime <minutes>```

  ```replay autoRecord mode <off/proxyConnected/playerConnected/health>```

  ```replay autoRecord health <integer>```

  ```replay featureFlags on/off```


### requeue

Cancels KeepAlive packets until the client is kicked to 2b2t's queue.



This should kick you to the start of the queue, similar to a reconnect queue skip.



Can be useful for resetting player state without having to do a full reconnect

**Usage**

  ```requeue```


### respawn

Performs a respawn

**Usage**

  ```respawn```


### rotate

Rotates the bot in-game.



Note that many other modules can change the player's rotation after this command is executed.

**Usage**

  ```rotate <yaw> <pitch>```

  ```rotate yaw <yaw>```

  ```rotate pitch <pitch>```


### sendMessage

Sends a message in-game.

**Aliases:** `say` / `m`

**Usage**

  ```sendMessage <message>```


### switch

Switch the connected player to an alternate MC server.



Can be used to switch between multiple ZenithProxy instances quickly.



Servers being switched to must have transfers enabled and be on an MC version >=1.20.6

**Usage**

  ```switch```

  ```switch register <name> <address> <port>```

  ```switch del <name>```

  ```switch clear```

  ```switch list```

  ```switch <name>```


### sessionTimeLimit

Sends an in-game warning before you are kicked for reaching the 2b2t session time limit.

**Usage**

  ```sessionTimeLimit on/off```

  ```sessionTimeLimit refresh```

  ```sessionTimeLimit ingame list```

  ```sessionTimeLimit ingame add <minutes>```

  ```sessionTimeLimit ingame del <minutes>```

  ```sessionTimeLimit ingame clear```

  ```sessionTimeLimit discord list```

  ```sessionTimeLimit discord add <minutes>```

  ```sessionTimeLimit discord add <minutes> mention```

  ```sessionTimeLimit discord del <minutes>```

  ```sessionTimeLimit discord clear```


### skin

Temporarily change your skin to another player's skin.



This is only client-side and only affects how you see yourself.

**Usage**

  ```skin <playerName>```


### spammer

Spams messages or whispers in-game. Use with caution, this can and will get you muted.



To add messages in bulk, use the `addAll` subcommand. Each message is delimited by the `,,` characters.

**Aliases:** `spam`

**Usage**

  ```spammer on/off```

  ```spammer whisper on/off```

  ```spammer whilePlayerConnected on/off```

  ```spammer delayTicks <int>```

  ```spammer randomOrder on/off```

  ```spammer appendRandom on/off```

  ```spammer list```

  ```spammer clear```

  ```spammer add <message>```

  ```spammer addAt <index> <message>```

  ```spammer addAll <message 1>,,<message 2>...```

  ```spammer del <index>```


### swap

Swaps the current controlling player to spectator mode.

**Usage**

  ```swap```

  ```swap force```


### spook

Rotates and stares at players in visual range.



Can often confuse other players in-game into thinking you are a real player.

**Usage**

  ```spook on/off```

  ```spook mode <visualRange/nearest>```


### stalk

Sends alerts when players join or leave

**Usage**

  ```stalk on/off```

  ```stalk list```

  ```stalk add/del <player>```


### tasks

[BETA]



Schedules commands to be executed after a delay or after specified events.



Examples:

`tasks add timed 15m pearlLoader load rfresh`

`tasks add interval mapgen 30s 1h map generate`

`tasks add event continueTraveling online once b goto 0 1500`

**Aliases:** `task`

**Usage**

  ```tasks add timed <id> <delay> <command>```

  ```tasks add interval <id> <startDelay> <repeatDelay> <command>```

  ```tasks add event <id> <connect/death/disconnect/online/playerConnect/playerDisconnect> <repeat/once> <command>```

  ```tasks del <id>```

  ```tasks list```

  ```tasks clear```

  ```tasks logCommandActionOutput on/off```

  ```tasks taskCommandExecutedNotification on/off```


### spawnPatrol

Patrols spawn and paths to any player it finds, killing them if you have kill aura enabled.

**Usage**

  ```spawnPatrol on/off```

  ```spawnPatrol goal <x> <y> <z>```

  ```spawnPatrol maxPatrolRange <blocks>```

  ```spawnPatrol targetOnlyNakeds on/off```

  ```spawnPatrol stickyTargeting on/off```

  ```spawnPatrol targetAttackers on/off```

  ```spawnPatrol nether on/off```

  ```spawnPatrol stuckKill on/off```

  ```spawnPatrol stuckKill seconds <seconds>```

  ```spawnPatrol stuckKill minDist <blocks>```

  ```spawnPatrol stuckKill antiStuck on/off```

  ```spawnPatrol ignore add/del <player>```

  ```spawnPatrol ignore addAll <player1,player2,...>```

  ```spawnPatrol ignore clear```

  ```spawnPatrol ignore list```


### via

Configure the integrated ViaVersion module.



`zenithToServer` -> ZenithProxy connecting to the MC server

`playerToZenith` -> players connecting to ZenithProxy

**Usage**

  ```via zenithToServer on/off```

  ```via zenithToServer disableOn2b2t on/off```

  ```via zenithToServer version auto```

  ```via zenithToServer version <MC version>```

  ```via playerToZenith on/off```


### visualRange

Configure the VisualRange notification feature.



Alerts are sent both in the terminal and in discord, with optional discord mentions.



`replayRecording` settings will start recording when players enter your visual range and stop

when players leave, after the set cooldown.



`enemy` mode will only record players who are not on your friends list.

`all` mode will record all players, regardless of being on the friends list.



To add players to the friends list see the `friends` command.

**Aliases:** `vr`

**Usage**

  ```visualRange on/off```

  ```visualRange list```

  ```visualRange enter on/off```

  ```visualRange enter mention on/off```

  ```visualRange enter whisper on/off```

  ```visualRange enter whisper message <message>```

  ```visualRange enter whisper cooldown <seconds>```

  ```visualRange enter whisper command <command>```

  ```visualRange leave on/off```

  ```visualRange logout on/off```

  ```visualRange ignoreFriends on/off```

  ```visualRange replayRecording on/off```

  ```visualRange replayRecording mode <enemy/all>```

  ```visualRange replayRecording cooldown <minutes>```



