package com.shimizu.ar.core.ar_message.java.common.Infrastructure;

import android.util.Log;

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.EmojiData;
import com.shimizu.ar.core.ar_message.kotlin.domain.UserCurrentPosition;
import com.shimizu.ar.core.ar_message.kotlin.domain.collection.GeospatialEmojiCollection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeospatialEmojiModel {

    public void insertEmoji(EmojiData emojiData) {
        String query = "INSERT INTO emojis (longitude, latitude, altitude, quaternion_x, quaternion_y, quaternion_z, quaternion_w, emoji_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                MySqlOperation mySqlOperation = new MySqlOperation();
                mySqlOperation.executeUpdate(query, emojiData.longitude, emojiData.latitude, emojiData.altitude, emojiData.quaternion_x, emojiData.quaternion_y, emojiData.quaternion_z, emojiData.quaternion_w, emojiData.emojiType);
            });
            executor.shutdown();
        } catch (Exception e) {
            Log.e("ERROR", "failed insert ArMessage");
        }
    }

    public void getEmoji(UserCurrentPosition userCurrentPosition, GeospatialEmojiCollection geospatialEmojiCollection){
        Double longitudeFixedDistance = 0.00022;
        Double latitudeFixedDistance  = 0.00018;
        Double altitudeFixedDistance  = 3.0;
        String query = "SELECT longitude, latitude, altitude, quaternion_x, quaternion_y, quaternion_z, quaternion_w, emoji_type " +
                "FROM emojis " +
                "WHERE longitude BETWEEN ? - " + longitudeFixedDistance + " AND ? + " + longitudeFixedDistance +
                " AND latitude BETWEEN ? - " + latitudeFixedDistance + " AND ? + " + latitudeFixedDistance +
                " AND altitude BETWEEN ? - " + altitudeFixedDistance + " AND ? + " + altitudeFixedDistance +
                " ORDER BY insert_datetime DESC LIMIT 10";
        try {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                MySqlOperation mySqlOperation = new MySqlOperation();
                List<Object[]> resultSet = mySqlOperation.executeQuery(query, userCurrentPosition.getLongitude(), userCurrentPosition.getLongitude(), userCurrentPosition.getLatitude(), userCurrentPosition.getLatitude(), userCurrentPosition.getAltitude(), userCurrentPosition.getAltitude());
                resultSet.forEach(result -> {
                    geospatialEmojiCollection.addGeospatialArMessageCollection(new EmojiData((Double)result[0], (Double)result[1], (Double)result[2], (Float)result[3], (Float)result[4], (Float)result[5], (Float)result[6], (String)result[7]));
                });
                geospatialEmojiCollection.changeCreateEmojiObject(true);
            });
            executor.shutdown();
        } catch (Exception e) {
            Log.e("ERROR", "failed get emoji");
        }
    }
}
