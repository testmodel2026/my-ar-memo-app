package com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class;

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.interfaces.GeospatialData;

public class EmojiData implements GeospatialData {
    public final Double longitude;
    public final Double latitude;
    public final Double altitude;
    public final Float quaternion_x;
    public final Float quaternion_y;
    public final Float quaternion_z;
    public final Float quaternion_w;
    public final String emojiType;

    public EmojiData( Double longitude, Double latitude, Double altitude, Float quaternion_x, Float quaternion_y, Float quaternion_z, Float quaternion_w, String emojiType) {
        this.longitude       = longitude;
        this.latitude        = latitude;
        this.altitude        = altitude;
        this.quaternion_x    = quaternion_x;
        this.quaternion_y    = quaternion_y;
        this.quaternion_z    = quaternion_z;
        this.quaternion_w    = quaternion_w;
        this.emojiType       = emojiType;
    }

    @Override
    public Double getLongitude() {
        return this.longitude;
    }

    @Override
    public Double getLatitude() {
        return this.latitude;
    }

    @Override
    public Double getAltitude() {
        return this.altitude;
    }

    @Override
    public Float getQuaternion_x() {
        return this.quaternion_x;
    }

    @Override
    public Float getQuaternion_y() {
        return this.quaternion_y;
    }

    @Override
    public Float getQuaternion_z() {
        return this.quaternion_z;
    }

    @Override
    public Float getQuaternion_w() {
        return this.quaternion_w;
    }
}
