# ZenithProxy

based software to avoid waiting for numbers on blockgame


TODO:

* Design Discord bot integration
  * Commands (most commands should be able to perform functions too minor for people to have to launch mc and connect to the proxy for)
    * chat interactions
    * live configuration of the bots config (including it's afk userless features (like autoeat and autolog))
    * baritone and configured pearl loading commands when the headless client stuff is sorted out
  * Permissions
  * Interesting integrations, e.g. chat logs, watching who comes online
  * notifications of important events
  * notification system for important events ( + configurable ping system for notifications, alongside the capability for the bot to directly message the owners on discord)
    * player entering/exiting visual range (whilst proxy is userless) (could have an auto message but this could get the account muted)
    * whisper notifications and interactive replies through discord message replies (configurable auto reply if proxy is userless (e.g. "i am currently AFK, your message has still been processed. dm me on discord if you'd like; odpay#1632)
    * online timer alerts (if account is approaching it's onlime time limit)
    * notifications for proxy-controlled disconnects, most are already performed with reasons attached
    * alerting when a user connects to the proxy (with distinct info attached like username and all that)
    * alerts for failed user-connection attempts (usually if they are not whitelisted) 
    * general notifications for anything the bot may automatically do if the proxy is userless (such as autologging etc.)
* Rework to support multiple proxy instances for multiple accounts with individual control
* Multi-client support
  * Incl. built in headless client to use for antiafk or movement with baritone
  * Multi-client connect with players switching from spectator to control


*forked from daporkchop's proxy*
