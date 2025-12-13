- Title:
  - Plasmo Voice: feature-gated integration + UDP relay alignment + queue parsing

- Background and motivation:
  - On some SMP or Role Play servers, the server is often very crowded and players must wait in a queue. The queue position is shown in the action bar instead of subtitles, and these servers often use Plasmo Voice as the built-in voice chat solution.
  - To preserve the original goal of the project (serving 2b2t) without side effects, all new options are disabled by default and must be explicitly enabled via configuration.

- Feature summary:
  - When connecting to ZenithProxy, the client can now correctly connect to the Plasmo Voice server and use Plasmo Voice for voice chat. This is enabled by caching and re-sending the initial handshake packets.
  - The queue position shown in the action bar can now be parsed and matched.
    [img]

- Main changes (by feature area):
  - Configuration switches and command exposure
    - Added the `server.plasmoVoice` configuration block, providing three switches: `enabled`, `registerChannels`, and `udpRelay`. The feature is disabled by default and only takes effect when explicitly enabled.  
      - `src/main/java/com/zenith/util/config/Config.java:684-727`
    - Exposed Plasmo Voice related switches via the `serverConnection` command. When enabling or disabling options that require a restart, the ZenithProxy MC server is restarted automatically.  
      - `src/main/java/com/zenith/command/impl/ServerConnectionCommand.java:21-64,70-202,213-232`
    - Documented the new command arguments in the wiki, making operational configuration easier.  
      - `docs/wiki/Commands.md:720-740`

  - Plasmo Voice channel registration (optional)
    - After the client finishes login and switches to the CONFIGURATION state, it conditionally sends a `minecraft:register` custom payload to register Plasmo Voice channels:
      - Registered channels: `plasmo:voice/v2`, `plasmo:voice/v2/installed`, `plasmo:voice/v2/service`
      - Only active when `server.plasmoVoice.enabled && server.plasmoVoice.registerChannels` is true, so servers that do not use PV are unaffected.  
      - `src/main/java/com/zenith/network/client/handler/incoming/CLoginFinishedHandler.java:14-27`

  - PV connection packet rewrite + UDP relay
    - In the inbound custom payload handler on the client side, added specialized handling for the `plasmo:voice/v2` channel:
      - If `plasmoVoice.enabled` or `udpRelay` is disabled, packets are passed through transparently.  
      - When enabled, the handshake packets are cached so they can be reused when spectators connect later.  
      - For connection packets of type `0x01`, parse and rewrite them, replacing the original server address with the proxy’s outbound address and port.  
      - Broadcast the rewritten PV connection packet to all configured proxy players.  
      - `src/main/java/com/zenith/network/client/handler/incoming/CustomPayloadHandler.java:23-140`
    - Added a UDP relay implementation `VoiceUdpRelay`, which binds to the proxy’s external IP and transfer port when the server starts:
      - Maintains `secret -> client` and `secret -> remote` mappings.  
      - Uses a fixed MAGIC header to identify PV UDP packets and only relays packets that match the protocol.  
      - When receiving UDP packets from the client, forwards them to the corresponding remote; when receiving from the remote, sends them back to the last known client.  
      - All logging is controlled by `debugLogs` to avoid log spam under default settings.  
      - To avoid unbounded growth of secret mappings under abnormal conditions, caps the number of tracked secrets and clears stale mappings when the cap is reached.  
      - `src/main/java/com/zenith/voice/VoiceUdpRelay.java:1-90`
    - Aligned the UDP relay lifecycle and UPnP behavior in the core proxy class:
      - In `startServer`, when PV and UDP relay are enabled, create and start `VoiceUdpRelay`, using `server.getProxyPortForTransfer()` for port and transfer settings.  
      - In `openUpnp` and `closeUpnp`, besides the TCP listening port, also open/close UPnP mappings for the UDP transfer port.  
      - In `stopServer`, stop the UDP relay cleanly and release the thread and socket.  
      - `src/main/java/com/zenith/Proxy.java:34-35,81-104,533-621`
    - After a player finishes login and the proxy-side configuration is complete, if UDP relay is enabled, send the cached PV handshake packets so later-joining players can also establish voice sessions correctly:
      - `src/main/java/com/zenith/network/server/handler/ProxyServerLoginHandler.java:30-137`

  - Queue position parsing (ActionBar)
    - In the handler processing 2b2t action bar messages, added queue position parsing logic that only runs while the player is actually in queue:
      - Uses `ComponentSerializer.serializePlain` to convert the action bar text to plain text.  
      - Supports multiple formats (English, Chinese, simple `current/total` style):
        - `Position in queue: <n>` (case-insensitive variants).  
        - `位置：<n>` or `位置:<n>` and similar Chinese hints.  
        - `<n>/...` style short hints.  
      - Compares the parsed position with `session.getLastQueuePosition()`. When it changes, asynchronously publishes a `QueuePositionUpdateEvent` to the event bus and updates the session’s latest queue position.  
      - On parsing errors, only logs a warning and does not interrupt the normal packet flow.  
      - `src/main/java/com/zenith/network/client/handler/incoming/SetActionBarTextHandler.java:21-71`

  - Mojang SessionServer API robustness
    - Increased the HTTP client connection timeout from 2 seconds to 8 seconds, reducing false negatives for cross-region networks:  
      - `src/main/java/com/zenith/feature/api/Api.java:19-23`
    - Added up to 3 retries with random backoff for the `hasJoined` API:
      - When the status code is 200, immediately parse and return a `GameProfile`.  
      - If the body starts with `<!DOCTYPE html>`, treat it as Mojang-side rate limiting and log an error.  
      - Other exceptions only log an error on the final attempt; earlier failures are silent.  
      - After each failure, sleep for 300–1000 ms randomly to avoid hammering the Mojang endpoint.  
      - `src/main/java/com/zenith/feature/api/sessionserver/SessionServerApi.java:65-91`

- Code references (for review):
  - Configuration / commands / docs
    - `src/main/java/com/zenith/util/config/Config.java:684-727`
    - `src/main/java/com/zenith/command/impl/ServerConnectionCommand.java:21-64,70-202,213-232`
    - `docs/wiki/Commands.md:720-740`
  - Plasmo Voice registration and relay
    - `src/main/java/com/zenith/network/client/handler/incoming/CLoginFinishedHandler.java:14-27`
    - `src/main/java/com/zenith/network/client/handler/incoming/CustomPayloadHandler.java:23-140`
    - `src/main/java/com/zenith/voice/VoiceUdpRelay.java:1-90`
    - `src/main/java/com/zenith/Proxy.java:34-35,81-104,533-621`
    - `src/main/java/com/zenith/network/server/handler/ProxyServerLoginHandler.java:30-137`
  - Queue parsing
    - `src/main/java/com/zenith/network/client/handler/incoming/SetActionBarTextHandler.java:21-71`
  - SessionServer API
    - `src/main/java/com/zenith/feature/api/Api.java:19-23`
    - `src/main/java/com/zenith/feature/api/sessionserver/SessionServerApi.java:65-91`

- Verification:
  - Plasmo Voice integration
    - After enabling the Plasmo Voice options, restart the ZenithProxy MC server and connect to the proxy with a client that has Plasmo Voice installed:
      - Confirm that Plasmo Voice is active on the client and can join voice channels.
      - Check that server logs contain PV connection rewrite and UDP relay related entries.
      [2025/12/13 01:36:05] [Client] [INFO] CustomPayload received: plasmo:voice/v2 len=17
  - Queue position parsing
    - Connect to a server where queue position is shown in the action bar, trigger queue detection and enter the queue. The console should show related logs, and the MOTD should display the queue position.
    [img]

- Compatibility:
  - Default behavior:
    - When `plasmoVoice.enabled=false`, no PV channels are registered, and no packets are rewritten or UDP relay started. The behavior is completely transparent to environments like 2b2t.
  - UDP relay and UPnP:
    - When UDP relay is enabled, it uses the UDP port corresponding to `server.getProxyPortForTransfer()`.  
    - When UPnP is enabled, it attempts to map this UDP port via UPnP. The effect may vary depending on the router (some devices do not support UDP UPnP).
  - Security:
    - The UDP relay only forwards packets based on the PV protocol `secret` mapping. It does not decrypt payloads or persist any audio data.  
    - For public deployments, it is recommended to combine this with IP allowlists, firewalls, and other protections.

- Commit list:
  - config: add server.plasmoVoice feature gate (default off)
  - command: expose plasmoVoice toggles via serverConnection
  - docs: document serverConnection plasmoVoice flags
  - voice: register PV channels behind feature flag
  - voice: rewrite PV connection packets and add UDP relay
  - proxy: map PV UDP relay port via UPnP when enabled
  - queue: parse 2b2t action bar for queue position updates
  - api: increase session server timeouts and retry hasJoined

- Example command sequence:
  - Configure proxy and enable PV:
    - serverConnection proxyIP 192.168.1.x:25565 // default Plasmo Voice port
    - serverConnection plasmoVoice on
    - serverConnection plasmoVoice udpRelay on
    - serverConnection plasmoVoice registerChannels on
