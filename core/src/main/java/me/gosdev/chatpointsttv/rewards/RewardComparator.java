package me.gosdev.chatpointsttv.rewards;

import java.util.Comparator;

public class RewardComparator implements Comparator<Reward> {
    @Override    public int compare(Reward o1, Reward o2) {
        if (o1.getType() != o2.getType()){
            throw new UnsupportedOperationException("Cannot compare " + o1.getType().toString() + " rewards with " + o2.getType().toString());
        }

        try {
            int difference = Integer.parseInt(o2.getEvent()) - Integer.parseInt(o1.getEvent());

            if (difference == 0) {
                return compareWildcards(o1, o2);
            }
            return difference;
        } catch (NumberFormatException e) {
            return compareWildcards(o1, o2);
        }

    }

    private int compareWildcards(Reward o1, Reward o2) {
        if (isWildcard(o1) && !isWildcard(o2)) {
            return 1;
        }
        if (isWildcard(o2) && !isWildcard(o1)) {
            return -1;
        }
        return 0;
    }

    private boolean isWildcard(Reward reward) {
        return reward.getChannel().equals(Rewards.EVERYONE);
    }

}
