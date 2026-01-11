package com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class;

public class UserProfileData {

    public final String userName;
    public final byte[] userIcon;

    public UserProfileData(String userName, byte[] userIcon) {
        this.userName = userName;
        this.userIcon = userIcon;
    }
}
