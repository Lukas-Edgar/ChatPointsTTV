package me.gosdev.chatpointsttv;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Permissions {
    BROADCAST("chatpointsttv.broadcast"),
    MANAGE("chatpointsttv.manage"),
    TARGET("chatpointsttv.target");

    public final String permissionId;

}
