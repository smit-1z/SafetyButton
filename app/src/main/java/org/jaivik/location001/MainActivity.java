package org.jaivik.location001;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    public static Location location;
    private TextView locationTv, infoTxt,jMail;
    private Button btnControl, btnSettings, btnCall;
    private ImageView alertImg;

    public static MqttAndroidClient client;
    private String TAG = "MainActivity";
    public static PahoMqttClient pahoMqttClient;
    public static Context mContext;
    public boolean isSubscribed = false;
    public boolean isActive = false;

    public static ArrayList<String> myReceivers = new ArrayList<>();

    private GoogleApiClient googleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private LocationRequest locationRequest;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000;
    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    // integer for permissions results request
    private static final int ALL_PERMISSIONS_RESULT = 1011;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0, MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;
    String phoneNo, message, customMsg;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationTv = findViewById(R.id.location);
        infoTxt = findViewById(R.id.txtInfo);
        btnControl = findViewById(R.id.buttonControl);
        btnSettings = findViewById(R.id.btnSettings);
        alertImg = findViewById(R.id.imgVvAlert);
        btnCall = findViewById(R.id.buttonCall);
        jMail = findViewById(R.id.jMail);
        mContext = getApplicationContext();

        populateSenderNReceivers();

        pahoMqttClient = new PahoMqttClient();
        client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
        Intent intent = new Intent(MainActivity.this, MqttMessageService.class);
        startService(intent);

        /*
        //start activated
        if(MqttMessageService.isActive){
            try{
                pahoMqttClient.subscribe(client, Constants.myPhoneID,0);
                displayToast("Successfully Connected");
                btnControl.setText("DeActivate");
                MqttMessageService.isSubscribed=false;
                MqttMessageService.isActive=true;
                this.isActive=true;
            }
            catch (Exception e){
                e.printStackTrace();
                displayToast("Activation Exception\n"+e);
            }
        }
        */


        if (MqttMessageService.isServiceActive) {
            displayToast("Service is Active");
        }


        if (pahoMqttClient == null) {
            displayToast("paho is null");
        }


        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }
        handleLocationSetting();


        // we build google api client
        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();


        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if deactivated
                if (!isActive) {
                    activate();

                } //if activated
                else {
                    deActivate();
                }
            }

        });

        alertImg.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        alertImg.setPressed(true);
                        alertImg.setImageResource(R.drawable.panic_pressed);
                        v.invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        sendEmergencyAlert();
                        alertImg.setPressed(false);
                        alertImg.setImageResource(R.drawable.panic_unpressed);
                        v.invalidate();
                        break;
                }

                return true;
            }
        });


        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
        });

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Call Police?")
                        .setPositiveButton("Call", new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                        call();
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        jMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent e = new Intent(Intent.ACTION_VIEW);
                Uri data = Uri.parse("mailto:jaivikjap@gmail.com");
                e.setData(data);
                startActivity(Intent.createChooser(e, ""));
            }
        });
    }


    public void handleLocationSetting() {
        boolean gps_enabled = false;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!gps_enabled) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("Enable GPS from Settings")
                    .setPositiveButton("Settings", new
                            DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            displayToast("Safety Button cannot continue without permissions!!!\nExiting");
                            SystemClock.sleep(2000);
                            finish();
                        }
                    })
                    .show();
        }
    }
    public String getMyLocation() {
        int count = 0;
        while (location == null) {
            if (count < 3) {
                displayToast("Please Wait.\nGathering Location Data");
                count++;
                SystemClock.sleep(1000);
            } else {
                displayToast("Failed to Get Location");
                return null;
            }
        }

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        locationTv.setText("Latitude : " + latitude + "\nLongitude : " + longitude);
        infoTxt.setText("LocationChanged : " + location.getTime());
        Toast.makeText(getApplicationContext(), "Loc: " + latitude + " " + longitude, Toast.LENGTH_SHORT).show();

        return latitude + "," + longitude;
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

    public void activate() {
        //if d
        if (!isActive) {
            try {
                populateSenderNReceivers();
                pahoMqttClient.subscribe(client, Constants.myPhoneID, 0);
                //displayToast("Connection Active");
                MqttMessageService.isActive = true;
                isActive = true;
                btnControl.setText("DeActivate");
                infoTxt.setText("Connection Active");

            } catch (Exception e) {
                displayToast("activate: Failed to Subscribe!!!\n");
                infoTxt.setText(e.toString());
            }

        }
    }

    public void deActivate() {
        try {
            pahoMqttClient.unSubscribe(client, Constants.myPhoneID);
            //displayToast("Disconnected");
            isSubscribed = false;
            isActive = false;
            MqttMessageService.isSubscribed = false;
            MqttMessageService.isActive = false;
            btnControl.setText("Activate");
            infoTxt.setText("Disconnected");
        } catch (Exception e) {
            displayToast("deActivate: Failed to UnSubscribe!!!\n");
            infoTxt.setText(e.toString());
        }
    }

    public void sendSMS(String phone) {
        customMsg = readSettings(SettingsActivity.cusMsg);
        message = Constants.myPhoneID + " is in Danger.\n"+customMsg+"\nLocation : https://maps.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        } else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        }

    }

    public void sendEmergencyAlert() {

        if (!isActive) {
            return;
        }

        String[] loc = getMyLocation().split(",");
        try {
            String mPayload = "";
            mPayload += "{";
            mPayload += "\"id\":\"" + Constants.myPhoneID + "\",\"msg\":\"HELP\",";
            mPayload += " \"latitude\":\"" + loc[0] + "\",\"longitude\":\"" + loc[1] + "\"";
            mPayload += "}";

            //String mPayload = "{\"id\":\""+ Constants.myPhoneID+"\", \"msg\":\"HELP\", \"latitude\":\""+ +"\",       }" getMyLocation();

            int size = myReceivers.size();
            if (size == 0) {
                displayToast("No Receivers Added. Add receivers to send alerts");
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return;
            }

            infoTxt.setText("Alerted: " + (size - 1));
            for (int i = 1; i < size; i++) {
                String num = myReceivers.get(i);
                if (num != " " && num != null) {
                    pahoMqttClient.publishMessage(client, mPayload, 0, myReceivers.get(i));

                    sendSMS(myReceivers.get(i));

                    infoTxt.append("\n" + myReceivers.get(i));
                }

            }
            displayToast("Alert Sent");

        } catch (MqttException e) {
            e.printStackTrace();
            displayToast("Publish Failed\n" + e.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            displayToast("Publish Failed\n" + e.toString());
        }
    }



    public void call() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
            }
        } else {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:198"));
            startActivity(callIntent);
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
    // catches the onKeyUp button event
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                sendEmergencyAlert();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    public void startService(View v) {
//        String input = editTextInput.getText().toString();
        Intent serviceIntent = new Intent(this, ExampleService.class);
//        serviceIntent.putExtra("inputExtra", input);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
    }
    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPlayServices()) {
            locationTv.setText("You need to install Google Play Services to use the App properly");
        }
        populateSenderNReceivers();
        if (isActive) {
            deActivate();
            activate();
        }

    }

    public void openGMaps() {

        int count = 0;

        while (location == null) {
            if (count < 3) {
                displayToast("Please Wait.\nGathering Location Data");
                count++;
                SystemClock.sleep(1000);
            } else {
                displayToast("Failed to gather data");
                return;
            }

        }

        // Create a Uri from an intent string. Use the result to create an Intent.
        Uri gmmIntentUri = Uri.parse("geo:" + location.getLatitude() + "," + location.getLongitude() + "?z=15");

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");

        // Attempt to start an activity that can handle the Intent
        startActivity(mapIntent);

    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        // stop location updates
//        if (googleApiClient != null && googleApiClient.isConnected()) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
//            googleApiClient.disconnect();
//        }
//    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }

            return false;
        }

        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Permissions ok, we get last location
        location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (location != null) {
            locationTv.setText("Latitude : " + location.getLatitude() + "\nLongitude : " + location.getLongitude());
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "You need to enable permissions to display location !", Toast.LENGTH_SHORT).show();
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    displayToast("SafetyButton App Cannot work without location!!!");
                                    SystemClock.sleep(1500);
                                    finish();
                                }
                            }).create().show();

                            return;
                        }
                    }
                } else {
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    }
                }

                break;

            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    //smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    //Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS failed, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            break;
            case MY_PERMISSIONS_REQUEST_CALL_PHONE: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:198"));
                    if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    Activity#requestPermissions
                        return;
                    }
                    startActivity(callIntent);

                    Toast.makeText(getApplicationContext(), "Calling",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Call Failed, please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }

    public void displayToast(String s){
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }


}