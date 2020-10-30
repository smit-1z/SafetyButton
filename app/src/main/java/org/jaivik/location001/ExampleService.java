package org.jaivik.location001;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.SmsManager;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;
import android.os.Vibrator;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import static org.jaivik.location001.App.CHANNEL_ID;

public class ExampleService extends Service {

    public static ArrayList<String> myReceivers = new ArrayList<>();
    //    SettingsContentObserver settingsContentObserver;
    private MediaSessionCompat mediaSession;
    int i = 0;
    long[] pattern = {0, 500, 50, 800, 50, 1000};
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service started", Toast.LENGTH_SHORT).show();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safety Button service is running")
                .setContentText("Press VolumeUp key thrice to send alert!")
                .setSmallIcon(R.drawable.ic_final_j)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .build();
        startForeground(1, notification);

        mediaSession = new MediaSessionCompat(this, "PlayerService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0) //you simulate a player which plays something.
                .build());

        VolumeProviderCompat myVolumeProvider = new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, /*max volume*/100, /*initial volume level*/50) {
            @Override
            public void onAdjustVolume(int direction) {
                if (direction == 1) {
                    i = i + direction;
                }

                if (i == 3) {
                    populateSenderNReceivers();
                    sendAlert();
                    vibrate();
                    i = 0;
                }
            }
        };

        mediaSession.setPlaybackToRemote(myVolumeProvider);
        mediaSession.setActive(true);
//        settingsContentObserver = new SettingsContentObserver(this, new Handler());
//        getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingsContentObserver);
        return START_STICKY;
    }

    public void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(pattern, -1);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void sendAlert() {
        int size = myReceivers.size();
        for (int i = 1; i < size; i++) {
            String num = myReceivers.get(i);
            if (num != " " && num != null) {
                sendSMS(myReceivers.get(i));
            }
        }
    }

    public void sendSMS(String phone) {
        String customMsg = readSettings(SettingsActivity.cusMsg);
        String message = Constants.myPhoneID + " is in Danger.\n" + customMsg + "\nLocation : https://maps.google.com/maps?q=" + MainActivity.location.getLatitude() + "," + MainActivity.location.getLongitude();

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, null);
        Toast.makeText(getApplicationContext(), "Alert Sent!", Toast.LENGTH_LONG).show();
    }

    public void populateSenderNReceivers() {
        myReceivers.clear();
        String[] myReceiverStrings = readSettings(SettingsActivity.settingsFile).split(",");
        for (int i = 0; i < myReceiverStrings.length; i++) {
            if (myReceiverStrings[i] != "" || myReceiverStrings[i] != " ") {
                myReceivers.add(myReceiverStrings[i].trim());
            }
        }

        String mID = readSettings(SettingsActivity.idFile).trim();
        if (mID != null || mID != "") {
            Constants.myPhoneID = mID;
            Constants.CLIENT_ID = "SafetyButton_" + mID;
        } else {
            displayToast("Your ID is not configured.\nUsing Default");
            Constants.myPhoneID = "1234567890";
            writeSettings(SettingsActivity.idFile, "1234567890");

        }
    }

    private String readSettings(String fileName) {
        String ret = "";
        try {
            InputStream inputStream = getApplicationContext().openFileInput(fileName);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            //displayToast( "File not found: " + e.toString());
            return ret;
        }
        catch (IOException e) {
            //displayToast( "Can not read file: " + e.toString());
            return ret;
        }
        return ret;
    }

    private void writeSettings(String data, String fileName) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().openFileOutput(fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            displayToast("File write failed: " + e.toString());
        }
    }

    public void displayToast(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

//    public class SettingsContentObserver extends ContentObserver {
//        int previousVolume;
//        Context context;
//
//        SettingsContentObserver(Context c, Handler handler) {
//            super(handler);
//            context = c;
//            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            previousVolume = Objects.requireNonNull(audio).getStreamVolume(AudioManager.STREAM_MUSIC);
//        }
//        @Override
//        public boolean deliverSelfNotifications() {
//            return super.deliverSelfNotifications();
//        }
//        @Override
//        public void onChange(boolean selfChange) {
//            super.onChange(selfChange);
//            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//            int currentVolume = Objects.requireNonNull(audio).getStreamVolume(AudioManager.STREAM_MUSIC);
//            int delta = previousVolume - currentVolume;
//            if (delta > 0) {
//                populateSenderNReceivers();
//                sendAlert();
//                previousVolume = currentVolume;
//            } else if (delta < 0) {
////                Toast.makeText(MainActivity.this, "Volume Increased", Toast.LENGTH_SHORT).show();
//                previousVolume = currentVolume;
//            }
//        }
//    }
}

