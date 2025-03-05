package me.gosdev.chatpointsttv.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.helix.domain.ModeratedChannel;
import com.github.twitch4j.helix.domain.ModeratedChannelList;
import com.github.twitch4j.helix.domain.StreamList;
import com.github.twitch4j.helix.domain.UserList;
import com.netflix.hystrix.exception.HystrixRuntimeException;

import me.gosdev.chatpointsttv.ChatPointsTTV;
import me.gosdev.chatpointsttv.twitch.TwitchClient;

public class TwitchUtils {

    private TwitchUtils() {}

    public static List<String> getModeratedChannelIDs(String auth, String userId) throws HystrixRuntimeException {
        String cursor = null;
        List<String> modsOutput = new ArrayList<>();

        do {
            ModeratedChannelList moderatorList = ChatPointsTTV.getInstance().getTwitch().getClient().getHelix().getModeratedChannels(
                    auth,
                    userId,
                    100,
                    cursor
            ).execute();
            cursor = moderatorList.getPagination().getCursor();
            for (ModeratedChannel channel : moderatorList.getChannels()) {
                modsOutput.add(channel.getBroadcasterId());
            }
        } while (cursor != null);
        return modsOutput;
    }

    public static String planToString(SubscriptionPlan plan) {
        return switch (plan.toString()) {
            case "Prime" -> "Tier 1 (Prime)";
            case "1000" -> "Tier 1";
            case "2000" -> "Tier 2";
            case "3000" -> "Tier 3";
            default -> null;
        };
    }

    public static String planToConfig(SubscriptionPlan plan) {
        return switch (plan.toString()) {
            case "Prime" -> "TWITCH_PRIME";
            case "1000" -> "TIER1";
            case "2000" -> "TIER2";
            case "3000" -> "TIER3";
            default -> null;
        };
    }

    public static String getUserId(String username) {
        TwitchClient client = ChatPointsTTV.getInstance().getTwitch();
        UserList resultList = client.getClient().getHelix().getUsers(client.getOAuth().getAccessToken(), null, Collections.singletonList(username)).execute();
        if (resultList.getUsers().isEmpty()) {
            throw new NullPointerException("Couldn't fetch user: " + username);
        }
        return resultList.getUsers().get(0).getId();
    }

    public static String getUsername(String userId) {
        TwitchClient client = ChatPointsTTV.getInstance().getTwitch();
        UserList resultList = client.getClient().getHelix().getUsers(client.getOAuth().getAccessToken(), Collections.singletonList(userId), null).execute();
        if (resultList.getUsers().isEmpty()) {
            throw new NullPointerException("Couldn't fetch user ID: " + userId);
        }
        return resultList.getUsers().get(0).getDisplayName();
    }

    public static boolean isLive(String accessToken, String username) {
        StreamList request = ChatPointsTTV.getInstance().getTwitch().getClient().getHelix().getStreams(accessToken, null, null, null, null, null, null, Collections.singletonList(username)).execute();
        return !request.getStreams().isEmpty();
    }
}
