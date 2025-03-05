package me.gosdev.chatpointsttv.rewards;

import me.gosdev.chatpointsttv.utils.RewardType;
import me.gosdev.chatpointsttv.utils.TwitchUtils;

import java.util.List;

public class Reward {
    rewardType type;
    private final String event;
    private final List<String> cmds;
    private final String channel;
    private final String channelId;

    public Reward (rewardType type, String channel, String event, List<String> cmds) {
        this.type = type;
        this.channel = channel;
        
        channelId = channel.equals(Rewards.EVERYONE) ? "*" : TwitchUtils.getUserId(channel);

        this.event = event;
        this.cmds = cmds;
    }

    public String getEvent() {
        return event;
    }
    public List<String> getCommands() {
        return cmds;
    }
    public rewardType getType() {
        return type;
    }
    public String getChannel() {
        return channel;
    }
    public String getTargetId() {
        return channelId;
    }
}