package me.gosdev.chatpointsttv;

public enum Permissions {
    BROADCAST("chatpointsttv.broadcast"),
    MANAGE("chatpointsttv.manage"),
    TARGET("chatpointsttv.target");

    public final String permissionId;

    Permissions(String label) {
        this.permissionId = label;
    }
}
