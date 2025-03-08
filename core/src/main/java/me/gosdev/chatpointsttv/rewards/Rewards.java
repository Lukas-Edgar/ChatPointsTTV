package me.gosdev.chatpointsttv.rewards;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.gosdev.chatpointsttv.utils.RewardType;
import org.bukkit.configuration.ConfigurationSection;

import me.gosdev.chatpointsttv.ChatPointsTTV;

import static org.bukkit.Bukkit.getLogger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Rewards {

    private static final Logger LOG = getLogger();
    public static final String EVERYONE = "*";

    private static final Map<RewardType, List<Reward>> REWARD_LIST = new EnumMap<>(RewardType.class);

    public static List<Reward> getRewards(RewardType type) {
        if (REWARD_LIST.get(type) != null) {
            return REWARD_LIST.get(type);
        }

        Object config = ChatPointsTTV.getInstance().getRewardConfig(type);
        List<Reward> rewards = new ArrayList<>();

        if (config instanceof ArrayList<?> configList && type.equals(RewardType.FOLLOW)) { // Should only be non-specific Follow rewards
            addDefaultFollowRewards(type, configList, rewards);
        } else if (config instanceof ConfigurationSection configRewards) {
            addRewardsFromSection(type, configRewards, rewards);
        } else {
            String message = String.format("invalid reward configuration for %s", type);
            LOG.log(Level.WARNING, message);
            return Collections.emptyList();
        }
        rewards.sort(new RewardComparator());
        REWARD_LIST.put(type, rewards);

        return REWARD_LIST.get(type);
    }

    private static void addRewardsFromSection(RewardType type, ConfigurationSection configRewards, List<Reward> rewards) {
        if (type == RewardType.FOLLOW) {
            addRewardsForChannel(type, rewards, null, configRewards);
        } else {
            for (String event : configRewards.getKeys(false)) {
                ConfigurationSection channelSection = configRewards.getConfigurationSection(event);
                if (channelSection == null) {
                    rewards.add(new Reward(type, EVERYONE, event, configRewards.getStringList(event)));
                } else {
                    addRewardsForChannel(type, rewards, event, channelSection);
                }
            }
        }
    }

    private static void addRewardsForChannel(RewardType type, List<Reward> rewards, String key, ConfigurationSection channelSection) {
        Set<String> channelKeys = channelSection.getKeys(false);
        for (String channelKey : channelKeys) {
            String channel = channelKey.equals("default") ? EVERYONE : channelKey;
            rewards.add(new Reward(type, channel, key, channelSection.getStringList(channelKey)));
        }
    }

    private static void addDefaultFollowRewards(RewardType type, ArrayList<?> configList, List<Reward> rewards) {
        List<String> rewardList = configList.stream().map(String.class::cast).toList();
        rewards.add(new Reward(type, EVERYONE, null, rewardList));
    }

    public static void resetRewards() {
        REWARD_LIST.clear();
    }
}
