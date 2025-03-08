package me.gosdev.chatpointsttv.rewards;

import lombok.Getter;
import me.gosdev.chatpointsttv.utils.RewardType;
import me.gosdev.chatpointsttv.utils.TwitchUtils;

import java.util.List;

@Getter
public class Reward {
    private final RewardType type;
    private final String event;
    private final List<String> commands;

    private final String channel;
    private final String channelId;

    public Reward (RewardType type, String channel, String event, List<String> cmds) {
        this.type = type;
        this.channel = channel;
        
        channelId = channel.equals(Rewards.EVERYONE) ? "*" : TwitchUtils.getUserId(channel);

        this.event = event;
        this.commands = cmds;
    }
}