# Disconnect Reasons Wiki

### What is this?
Users constantly send me screenshots and messages of ZenithProxy's disconnect messages asking "why?".

Instead of spending many hours repeating myself, this wiki is meant to document this for each kind of disconnect reason.

### How to Read This Wiki

Disconnect categories are displayed as headers.

Under the header, an example of a disconnect reason will be shown like this:

> Disconnected

And additional info will be written below the example.

## Kicked
> You have lost connection to the server

You can be kicked by 2b2t for:

* Flagging the AntiCheat
* Failing to bypass AntiAFK
* Reaching the 8hr session time limit (non-prio players only)

## Manual Disconnect
> Manual Disconnect

Generic disconnect reason sent by ZenithProxy when a user runs the `disconnect` command.

Can also be sent by other ZenithProxy modules performing disconnects like `autoDisconnect autoClientDisconnect`

## ZenithProxy Modules
> System disconnect

> AutoDisconnect

Generic disconnect reason sent by various ZenithProxy modules like ActiveHours

## Connection Issue
> Read timed out.

ZenithProxy will trigger a disconnect if it has not received any packets from the destination server (i.e. 2b2t) for a configurable time length (default 60 seconds). This will occur due to internet connection issues on either your side or 2b2t's.

You can configure ZenithProxy's behavior with commands, see `help clientConnection`

> Connection closed

> java.net.SocketException: Connection Reset

> io.netty.channel.unix.Errors$NativeIoException: Broken Pipe

The internet connection to 2b2t was closed by your operating system. Could be either you or 2b2t that has an internet connection issue.

## Connection Issue (2b2t)
> An internal error occurred in your connection

> Your connection to 2b2t encountered a problem

2b2t closed their internet connection to you. Could occur for various reasons, but it is them who are having issues.

## Connection Issue (You)
> Connection timed out

If you do not have a working internet connection you can't connect to 2b2t

## Server Restart
> Server restarting

> Server closed

> Proxy shutting down

2b2t is restarting. 2b2t will sometimes send a warning before the server restarts but the server could also crash without warning.

## SOCKS5 Proxy
> io.netty.handler.proxy.ProxyConnectException

Only affects you if you have a SOCKS5 or HTTPS proxy configured in `help clientConnection`.

Can occur if you have bad credentials for the proxy, or can occur because proxy connections are just not reliable.

## Authentication Failure
> Login Failed

ZenithProxy was unable to login to your Minecraft account. Could occur if you enter bad credentials, or those credentials expire, or Microsoft requires you to do some action to unlock your account.

Often, logging into the account in the vanilla MC launcher and joining a server will resolve authentication issues.

## Authentication Rate Limiting
> Authentication servers are down

> You are logging in too fast

ZenithProxy is either unable to login to your MC account or join a server because Microsoft has rate limited the login. Try waiting a few minutes before trying to login again.

## Already Connected
> You are already connected to this proxy

The account you're connecting to 2b2t with is already logged into 2b2t.

## Illegal Disconnect
> Illegal characters in chat

You are using a client with an IllegalDisconnect setting. This is often a setting in AutoDisconnect modules.

There is a glitch on 2b2t that causes the player to stay logged in after you disconnect sometimes.

Your client is forcing 2b2t to kick you by sending an illegal chat packet so you avoid getting that glitch.
