package org.jaivik.location001;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class MqttMessageService extends Service {

    private static final String TAG = "MqttMessageService";
    private PahoMqttClient pahoMqttClient;
    private MqttAndroidClient mqttAndroidClient;
    public static MediaPlayer mp;
    public static boolean isSubscribed=false;
    public static boolean isServiceActive=false;
    public static boolean isActive=false;

    public MqttMessageService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        pahoMqttClient = new PahoMqttClient();
        mqttAndroidClient = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.d(TAG, "connection Complete");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "connection Lost!!");
                //displayToast("Connection Lost");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

                if(isActive){
                    //to avoid redundant msg at subscription; false it at deActivation in MainActivity
                    if(isSubscribed){
                        setMessageNotification(s, new String(mqttMessage.getPayload()));
                        //displayToast("Message Arrived");
                    } else {
                        //displayToast("Fake Ignored");
                        isSubscribed=true;
                    }
                    isActive=true;
                }



            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        //Toast.makeText(getApplicationContext(), "Service Started", Toast.LENGTH_SHORT).show();
        isServiceActive=true;
        createNotificationChannel();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        displayToast("onDestroy");
    }

    private void setMessageNotification(@NonNull String topic, @NonNull String msg) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, "iot")
                        .setSmallIcon(R.drawable.ic_message_black_24dp)
                        .setContentText(msg)
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
        Intent resultIntent = new Intent(this, ActionActivity.class);

        JSONObject payload;


        try{
            payload = new JSONObject(msg);


            if(topic.equals(Constants.myPhoneID) && payload.get("msg").toString().equals("HELP")){

                resultIntent.putExtra("victim", payload.getString("id"));
                resultIntent.putExtra("latitude", payload.getString("latitude"));
                resultIntent.putExtra("longitude", payload.getString("longitude"));

                mp = MediaPlayer.create(this, R.raw.alertnuke);
                mp.setLooping(false);
                mp.start();

                //displayToast(String.valueOf(payload.get("id")));

                mBuilder.setContentText("Emergency Alert received from "+payload.getString("id"))
                    .setContentTitle("Safety Button Alert!!!");

                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(100, mBuilder.build());

                //displayToast(payload.toString());

            }

        } catch (JSONException e){
            e.printStackTrace();
            Log.d(TAG, "setMessageNotification: "+e.toString());
            //displayToast("Exception");
        }
    }

    public void displayToast(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("iot", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
