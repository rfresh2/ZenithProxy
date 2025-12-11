# Discord Bot Guide

## Create Bot

Create a discord bot here: https://discord.com/developers/applications

Click the `New Application` button, give it any name, and select `Create`

![](./_assets/img/discord-setup/create-application.png)

## Bot Token

On the left sidebar, select `Bot`, then click `Reset Token`

![](./_assets/img/discord-setup/reset-token.png)

After confirmations, the token and a `Copy` button should appear

## Bot Settings

In the `Installation` tab settings:

* set `Install Link` to None
* `Guild Install` should be checked
* `User Install` doesn't matter.

![](./_assets/img/discord-setup/DiscordSetup1.png)

In the `Bot` tab:

* Enable `Message Content Intent`
* Disable `Public Bot`

![](./_assets/img/discord-setup/DiscordSetup2.png)

## Invite Bot To Server

In the `OAuth2` tab, generate an invite link with these permissions:

![](./_assets/img/discord-setup/DiscordSetup3.png)

Open the invite link in a web browser and select the server to invite the bot to

## Discord Server Setup

In the discord server settings:

![](./_assets/img/discord-setup/DiscordSetup5.png)

1: Create a role for users to manage the bot:

![](./_assets/img/discord-setup/DiscordSetup4.png)

2: Assign the role to yourself and any other users who should be able to manage the bot.

3: Create a channel to manage the bot in:

![](./_assets/img/discord-setup/DiscordSetup6.png)

4: (Optional) Create another channel for ZenithProxy's chat relay (live chat)

## Configure ZenithProxy

At first launch, the launcher will ask you to configure the token/role/channel ID's

If you didn't do this or misconfigured it, you can use the [discord](Commands.md#discord_1) and [chatRelay](Commands.md#chatrelay) commands after

To get the role and channel ID's, you must enable `Developer Mode` in your discord user settings:

![](./_assets/img/discord-setup/DiscordSetup8.png)


Right-click on the roles/channels you created and click `Copy ID`

![](./_assets/img/discord-setup/DiscordSetup9.png)
