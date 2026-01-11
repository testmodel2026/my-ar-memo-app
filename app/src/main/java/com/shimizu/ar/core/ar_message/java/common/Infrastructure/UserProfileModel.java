package com.shimizu.ar.core.ar_message.java.common.Infrastructure;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.shimizu.ar.core.ar_message.kotlin.domain.UserProfile;

public class UserProfileModel {

    public void insertUserProfile(UserProfile userProfile, Context context) {
        String query          = "INSERT INTO users (user_name, user_icon) VALUES (?, ?)";
        Bitmap resizeUserIcon = Bitmap.createScaledBitmap(userProfile.getUserIcon(), 80, 80, true);
        byte[] binaryUserIcon = getBinaryFromBitmap(resizeUserIcon);
        try {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                MySqlOperation mySqlOperation = new MySqlOperation();
                Integer userId = mySqlOperation.executeUpdate(query, userProfile.getUserName(), binaryUserIcon);
                try {
                    String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                    SharedPreferences sharedPrefs = EncryptedSharedPreferences.create(
                            "user_prefs",
                            masterKeyAlias,
                            context,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString("user_id", userId.toString());
                    editor.apply();
                } catch (GeneralSecurityException | IOException e) {
                    Log.e("ERROR", "insertUserProfile");
                }
            });
            executor.shutdown();
        } catch (Exception e) {
            Log.e("ERROR", "failed insert userProfileSave");
        }
    }

    public void updateUserProfile(UserProfile userProfile) {
        String query          = "UPDATE users SET user_name = ?, user_icon = ? WHERE user_id = ?";
        Bitmap resizeUserIcon = Bitmap.createScaledBitmap(userProfile.getUserIcon(), 40, 40, true);
        byte[] binaryUserIcon = getBinaryFromBitmap(resizeUserIcon);
        try {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                MySqlOperation mySqlOperation = new MySqlOperation();
                mySqlOperation.executeUpdate(query, userProfile.getUserName(), binaryUserIcon, userProfile.getUserId());
            });
            executor.shutdown();
        } catch (Exception e) {
            Log.e("ERROR", "failed insert userProfileSave");
        }
    }

    public void getUserProfile(UserProfile userProfile) {
        String query = "select * from users where user_id = ?";
        try {
            MySqlOperation mySqlOperation = new MySqlOperation();
            mySqlOperation.executeQuery(query, userProfile.getUserName());
        } catch (Exception e) {
            Log.e("ERROR", "failed get userProfileSave");
        }
    }

    private byte[] getBinaryFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }
}
