package com.zenith.feature.whitelist;

import lombok.Getter;

import static com.zenith.Shared.CONFIG;

@Getter
public class PlayerListsManager {
    private PlayerList whitelist;
    private PlayerList spectatorWhitelist;
    private PlayerList friendsList;
    private PlayerList ignoreList;
    private PlayerList stalkList;

    public void init() { // must be called after config is loaded
        whitelist = new PlayerList("whitelist", CONFIG.server.extra.whitelist.whitelist);
        spectatorWhitelist = new PlayerList("spectatorWhitelist", CONFIG.server.spectator.whitelist);
        friendsList = new PlayerList("friendsList", CONFIG.client.extra.friendsList);
        ignoreList = new PlayerList("ignoreList", CONFIG.client.extra.chat.ignoreList);
        stalkList = new PlayerList("stalkList", CONFIG.client.extra.stalk.stalking);
        startRefreshTasks();
    }

    // todo: refactor refresh task into a single task that refreshes all lists
    //  duplicate players in multiple lists should not need their own refresh
    //  also move out the game profile contains cache update so it updates all lists
    public void startRefreshTasks() {
        getWhitelist().startRefreshTask();
        getSpectatorWhitelist().startRefreshTask();
        getFriendsList().startRefreshTask();
        getIgnoreList().startRefreshTask();
        getStalkList().startRefreshTask();
    }
}
