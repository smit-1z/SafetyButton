package org.jaivik.location001;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ActionActivity extends AppCompatActivity {

    TextView txtInfo;
    Button btnOpenMaps, btnSMS, btnCall;
    String phoneNo, message;

    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0, MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;


    double latitude = -1.1, longitude = -1.1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action);

        MqttMessageService.mp.stop();
        MqttMessageService.mp.release();

        txtInfo = findViewById(R.id.txtVvInfo);
        btnOpenMaps = findViewById(R.id.btnOpenMaps);
        //btnSMS = findViewById(R.id.buttonSMS);
        btnCall = findViewById(R.id.buttonCall);
        Intent i = getIntent();

        latitude = Double.parseDouble(i.getStringExtra("latitude"));
        longitude = Double.parseDouble(i.getStringExtra("longitude"));
        String victimID = i.getStringExtra("victim");

        txtInfo.setText(victimID + " has sent you an Emergency alert!");

        btnOpenMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGMaps();
            }
        });


        /*
        btnSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ActionActivity. this )
                        .setMessage( "Send SMS to Police?")
                        .setPositiveButton( "Send" , new
                                DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick (DialogInterface paramDialogInterface , int paramInt) {
                                        sendSMS();
                                    }
                                })
                        .setNegativeButton("Cancel", null)
                        .show() ;
            }
        });
        */

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(ActionActivity.this)
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


    }

    public void openGMaps() {

        if (latitude == -1.1 || longitude == -1.1) {
            displayToast("Invalid Locations Received!\nUnable to open in Maps");
            return;
        }

        // Create a Uri from an intent string. Use the result to create an Intent.
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?z=15");

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps");

        //displayToast(""+location.getTime());

        // Attempt to start an activity that can handle the Intent
        startActivity(mapIntent);

    }

    public void sendSMS() {

        phoneNo = "+918347153518";
        message = Constants.myPhoneID + " is in Danger.\nLocation : " + latitude + "," + longitude;

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
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        }

    }

    public void call() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CALL_PHONE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        MY_PERMISSIONS_REQUEST_CALL_PHONE);
            }
        } else {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:198"));
            startActivity(callIntent);
        }

    }


    public void displayToast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNo, null, message, null, null);
                    Toast.makeText(getApplicationContext(), "SMS sent.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "SMS faild, please try again.", Toast.LENGTH_LONG).show();
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
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for Activity#requestPermissions for more details.
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
}
