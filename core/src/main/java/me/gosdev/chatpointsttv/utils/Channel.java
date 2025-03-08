package me.gosdev.chatpointsttv.utils;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Channel {
    private final String channelName;
    private final String channelId;
    @Setter
    private boolean live;

    public Channel (String name, String id, boolean live) {
        this.channelName = name;
        this.live = live;
        this.channelId = id;
    }

}