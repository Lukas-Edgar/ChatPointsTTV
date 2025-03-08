package me.gosdev.chatpointsttv.tests;

import java.time.Instant;
import java.util.Optional;

import com.github.twitch4j.common.enums.SubscriptionPlan;
import static com.github.twitch4j.common.util.TypeConvert.jsonToObject;
import com.github.twitch4j.eventsub.events.ChannelChatMessageEvent;
import com.github.twitch4j.eventsub.events.ChannelChatNotificationEvent;
import com.github.twitch4j.eventsub.events.ChannelFollowEvent;
import com.github.twitch4j.eventsub.events.ChannelRaidEvent;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.domain.ChannelPointsReward;
import com.github.twitch4j.pubsub.domain.ChannelPointsUser;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

import me.gosdev.chatpointsttv.ChatPointsTTV;
import me.gosdev.chatpointsttv.utils.TwitchUtils;

public class EventTest {

    private EventTest() {}

    public static final String CHATTER_USER_ID = "{\"chatter_user_id\":\"";
    public static final String CHATTER_USER_LOGIN = "\",\"chatter_user_login\":\"";
    public static final String CHATTER_USER_NAME = "\",\"chatter_user_name\":\"";
    public static final String BROADCASTER_USER_ID = "\",\"broadcaster_user_id\":\"";
    public static final String BROADCASTER_USER_LOGIN = "\",\"broadcaster_user_login\":\"";
    public static final String BROADCASTER_USER_NAME = "\",\"broadcaster_user_name\":\"";
    public static final String USER_NAME = "\",\"user_name\":\"";
    public static final String USER_LOGIN = "\",\"user_login\":\"";
    public static final String USER_ID = "{\"user_id\":\"";
    public static final String NOTICE_TYPE_SUB = "\",\"notice_type\": \"sub\"";

    @SuppressWarnings("deprecation")
    public static RewardRedeemedEvent channelPointsRedemptionEvent(String channel, String chatter, String title, Optional<String> userInput) { //TODO: Move away from PubSub
        ChannelPointsRedemption redemption = new ChannelPointsRedemption();
        ChannelPointsReward reward = new ChannelPointsReward();
        ChannelPointsUser redeemer = new ChannelPointsUser();
        redeemer.setDisplayName(chatter);

        reward.setTitle(title);

        redemption.setChannelId(TwitchUtils.getUserId(channel));
        redemption.setUser(redeemer);
        userInput.ifPresent(redemption::setUserInput);

        redemption.setReward(reward);

        return new RewardRedeemedEvent(Instant.now(), redemption);
    }

    public static ChannelFollowEvent followEvent(String channel, String chatter) {

        return jsonToObject(
            USER_ID + TwitchUtils.getUserId(chatter) +
                    USER_LOGIN + chatter.toLowerCase() +
                    USER_NAME + chatter +
                    BROADCASTER_USER_ID + TwitchUtils.getUserId(channel) +
                    BROADCASTER_USER_LOGIN + channel.toLowerCase() +
                    BROADCASTER_USER_NAME + channel +
            "\",\"followed_at\":\"" + Instant.now().toString() +"\"}",
        ChannelFollowEvent.class);
    }

    public static ChannelChatMessageEvent cheerEvent(String channel, String chatter, int amount) {

        return jsonToObject(
            CHATTER_USER_ID + TwitchUtils.getUserId(chatter) +
                    CHATTER_USER_LOGIN + chatter.toLowerCase() +
                    CHATTER_USER_NAME + chatter +
                    BROADCASTER_USER_ID + TwitchUtils.getUserId(channel) +
                    BROADCASTER_USER_LOGIN + channel.toLowerCase() +
                    BROADCASTER_USER_NAME + channel +
            "\",\"cheer\": {\"bits\": " + amount +"}}",
        ChannelChatMessageEvent.class);
    }

    public static ChannelChatNotificationEvent subEvent(String channel, String chatter, SubscriptionPlan plan, int months) {
        ChannelChatNotificationEvent event = jsonToObject(
            CHATTER_USER_ID + TwitchUtils.getUserId(chatter) +
                    CHATTER_USER_LOGIN + chatter.toLowerCase() +
                    CHATTER_USER_NAME + chatter +
                    BROADCASTER_USER_ID + TwitchUtils.getUserId(channel) +
                    BROADCASTER_USER_LOGIN + channel.toLowerCase() +
                    BROADCASTER_USER_NAME + channel +
            "\",\"notice_type\": \"sub" +
            "\",\"sub\":{\"sub_tier\":\"" + (plan.equals(SubscriptionPlan.TWITCH_PRIME) ? "1000" : plan.toString()) + 
            isPrime(plan) +
            ",\"duration_months\":" + months + "}}",
            ChannelChatNotificationEvent.class);

        ChatPointsTTV.getInstance().log.info(plan.toString());

        return event;
            }

    public static ChannelChatNotificationEvent resubEvent(String channel, String chatter, SubscriptionPlan plan, int months) {

        return jsonToObject(
                CHATTER_USER_ID + TwitchUtils.getUserId(chatter) +
                        CHATTER_USER_LOGIN + chatter.toLowerCase() +
                        CHATTER_USER_NAME + chatter +
                        BROADCASTER_USER_ID + TwitchUtils.getUserId(channel) +
                        BROADCASTER_USER_LOGIN + channel.toLowerCase() +
                        BROADCASTER_USER_NAME + channel +
                        NOTICE_TYPE_SUB +
                        "\",resub\":{\"sub_tier\":\"" + plan.name() +
                        isPrime(plan) +
                        "\",is_gift\": \"false\"" +
                        "\",duration_months\":" + months +
                        "\",cumulative_months\":" + months +
                        "\",streak_months\":" + months + "}}",
            ChannelChatNotificationEvent.class);
            }

    private static String isPrime(SubscriptionPlan plan) {
        return "\",\"is_prime\":" + (plan.equals(SubscriptionPlan.TWITCH_PRIME) ? "true" : "false");
    }

    public static ChannelChatNotificationEvent subGiftEvent(String channel, String chatter, SubscriptionPlan plan, int amount) {

        return jsonToObject(
            CHATTER_USER_ID + TwitchUtils.getUserId(chatter) +
                    CHATTER_USER_LOGIN + chatter.toLowerCase() +
                    CHATTER_USER_NAME + chatter +
                    BROADCASTER_USER_ID + TwitchUtils.getUserId(channel) +
                    BROADCASTER_USER_LOGIN + channel.toLowerCase() +
                    BROADCASTER_USER_NAME + channel +
            "\",\"notice_type\": \"community_sub_gift" +
            "\",\"community_sub_gift\":{\"sub_tier\":\"" + plan +
            "\",\"total\":" + amount + "}}",
            ChannelChatNotificationEvent.class);
    }

    public static ChannelRaidEvent raidReward(String channel, String raider, int viewers) {

        return jsonToObject(
            "{\"from_broadcaster_user_id\":\"" + TwitchUtils.getUserId(raider) +
            "\",\"from_broadcaster_user_login\":\"" + raider.toLowerCase() +
            "\",\"from_broadcaster_user_name\":\"" + raider +
            "\",\"to_broadcaster_user_id\":\"" + TwitchUtils.getUserId(channel) +
            "\",\"to_broadcaster_user_login\":\"" + channel.toLowerCase() +
            "\",\"to_broadcaster_user_name\":\"" + channel +
            "\",\"viewers\": \"" + viewers + "\"}",
            ChannelRaidEvent.class);
    }
}
