package com.shimizu.ar.core.ar_message.java.common.Infrastructure;

import android.util.Log;

import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.ArMessageData;
import com.shimizu.ar.core.ar_message.java.common.Infrastructure.data_class.UserProfileData;
import com.shimizu.ar.core.ar_message.kotlin.ar_message.ArMessageActivity;
import com.shimizu.ar.core.ar_message.kotlin.domain.collection.GeospatialArMessageCollection;
import com.shimizu.ar.core.ar_message.kotlin.domain.UserCurrentPosition;
import com.shimizu.ar.core.ar_message.kotlin.domain.UserProfile;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeospatialArMessageModel {

    public void insertArMessage(ArMessageData arMessageData) {
        String query = "INSERT INTO ar_messages (longitude, latitude, altitude, quaternion_x, quaternion_y, quaternion_z, quaternion_w, message, text_color, background_color, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                MySqlOperation mySqlOperation = new MySqlOperation();
                mySqlOperation.executeUpdate(query, arMessageData.longitude, arMessageData.latitude, arMessageData.altitude, arMessageData.quaternion_x, arMessageData.quaternion_y, arMessageData.quaternion_z, arMessageData.quaternion_w, arMessageData.message, arMessageData.textColor, arMessageData.backgroundColor, new UserProfile(ArMessageActivity.context).getUserId());
            });
            executor.shutdown();
        } catch (Exception e) {
            Log.e("ERROR", "failed insert ArMessage");
        }
    }

    public void getArMessage(UserCurrentPosition userCurrentPosition, GeospatialArMessageCollection geospatialArMessageCollection){
        Double longitudeFixedDistance = 0.00022;
        Double latitudeFixedDistance  = 0.00018;
        Double altitudeFixedDistance  = 3.0;
        String query = "SELECT longitude, latitude, altitude, quaternion_x, quaternion_y, quaternion_z, quaternion_w, message, text_color, background_color, user_name, user_icon " +
                "FROM ar_messages " +
                "INNER JOIN users ON ar_messages.user_id = users.user_id " +
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
                    geospatialArMessageCollection.addGeospatialArMessageCollection(new ArMessageData((Double)result[0], (Double)result[1], (Double)result[2], (Float)result[3], (Float)result[4], (Float)result[5], (Float)result[6], (String)result[7], (Integer)result[8], (Integer)result[9]), new UserProfileData((String)result[10], (byte[])result[11]));
                });
                geospatialArMessageCollection.changeCreateArMessageObject(true);
            });
            executor.shutdown();
        } catch (Exception e) {
            Log.e("ERROR", "failed get ArMessage");
        }
    }
}
