# mc-proxy

based software to avoid waiting for numbers on blockgame


TODO:
  * Interesting integrations, e.g. chat logs, watching who comes online
  * notification system for important events ( + configurable ping system for notifications, alongside the capability for the bot to directly message the owners on discord)
    * whisper notifications and interactive replies through discord message replies (configurable auto reply if proxy is userless (e.g. "i am currently AFK, your message has still been processed. dm me on discord if you'd like; odpay#1632)
    * online timer alerts (if account is approaching it's onlime time limit)
    * alerts for failed user-connection attempts (usually if they are not whitelisted) 
  * Rework to support multiple proxy instances for multiple accounts with individual control
  * Multi-client support
    * Incl. built in headless client to use for antiafk or movement with baritone
    * Multi-client connect with players switching from spectator to control
  * Baritone/forge client connection when no client is connected
  * Rewrite discords commands class structure to share more common code

*forked from daporkchop's proxy*
