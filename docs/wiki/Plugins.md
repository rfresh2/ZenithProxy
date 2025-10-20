ZenithProxy plugins allow users to add custom modules and commands.

Plugins are only supported on the `java` ZenithProxy release channel (i.e. not `linux`).

You can find your current release channel in the `status` command title.

To switch to the `java` channel: `channel set java 1.21.4`

## Installing Plugins

### Option 1: ZenithProxy Commands

`plugins download <url>`

Example: `plugins download https://github.com/rfresh2/ZenithProxyChatControl/releases/download/1.0.4/ZenithProxyChatControl-1.0.4.jar`

To remove a plugin:

`plugins remove <pluginId>`

Example: `plugins remove chat-control`

### Option 2: Manual

The `plugins` folder is automatically created in the same folder you run the ZenithProxy launcher in.

Place plugin jars in the `plugins` folder.

Restart ZenithProxy to load plugins.

Loading plugins after launch or hot reloading is not supported.

## Where to Find Plugins

\#plugins channel in my discord server: https://discord.gg/nJZrSaRKtb

## Creating Plugins

Example Plugin / Template: https://github.com/rfresh2/ZenithProxyExamplePlugin
