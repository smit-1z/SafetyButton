package org.jaivik.location001;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {


    private static final int RESULT_PICK_CONTACT = 1001;
    Button btnAdd, btnClear, btnSetID, selectContact,btnMsg;
    ImageButton backBtn;
    EditText editNums, editMyID,custMsg;
    TextView txtInfo;
    ArrayList<String> myCurrentReceivers = new ArrayList<>();

    public static String settingsFile = "myNumbers";
    public static String idFile="myID";
    public static String cusMsg="customMessageList";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        txtInfo = findViewById(R.id.txtVvCurrentSettings);
        editNums = findViewById(R.id.editTxtNumbers);
        btnAdd = findViewById(R.id.buttonAddNumbers);
        btnClear = findViewById(R.id.buttonClear);
        editMyID = findViewById(R.id.editTextMyID);
        btnSetID = findViewById(R.id.buttonSetID);
        custMsg = findViewById(R.id.custMsgTv);
        btnMsg = findViewById(R.id.btnMsg);
        backBtn = findViewById(R.id.backBtn);
        selectContact = findViewById(R.id.selectContact);
        refreshInfoView();

        btnMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeSettings("",cusMsg);
                String msg = custMsg.getText().toString().trim();
                writeSettings(msg, cusMsg);
                custMsg.setText("");
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });
    }

    public void OnClick(View v){

        int id = v.getId();

        if(id==btnAdd.getId()) {
            writeSettings(readSettings(settingsFile) + "," + editNums.getText().toString(), settingsFile);
            displayToast("Added Successfully");
            editNums.setText("");
        }
        else if(id==selectContact.getId()){
            displayToast("select contact");
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
        }

        else if(id==btnClear.getId()){
            writeSettings("", settingsFile);
            displayToast("Cleared List");
        }
        else if(id==btnSetID.getId()){

            String mID = editMyID.getText().toString().trim();
            if(mID==null || mID==""){
                displayToast("Please Enter Valid ID");
            }
            else {
                writeSettings(mID, idFile);
                displayToast("ID set as : "+mID+"\nRestart the App after changing Sender ID");

            }
            editMyID.setText("");
        }

        refreshInfoView();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(),MainActivity.class));
    }
    public void refreshInfoView(){
        txtInfo.setText("Sender : \n"+readSettings(idFile));
        String mySettings = readSettings(settingsFile);

        if(mySettings!=""){
            txtInfo.append("\n\nReceivers: ");
            String[] myReceivers = mySettings.split(",");
            for(int i=0; i<myReceivers.length; i++){
                if(myReceivers[i]!=""){
                    txtInfo.append(myReceivers[i]+"\n");
                }
            }
        }
        else {
            txtInfo.append("\n\nNo Receivers Registered");
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    Cursor cursor = null;
                    try {
                        String contactNumber = null;
                        //String contactName = null;
// getData() method will have the
// Content Uri of the selected contact
                        Uri uri = data.getData();
//Query the content uri
                        cursor = getContentResolver().query(uri, null, null, null, null);
                        cursor.moveToFirst();
// column index of the phone number
                        int phoneIndex = cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER);
// column index of the contact name
                        int nameIndex = cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        contactNumber = cursor.getString(phoneIndex);
                        //contactName = cursor.getString(nameIndex);
// Set the value to the textviews
//                        tvContactName.setText("Contact Name : ".concat(contactName));
                        editNums.setText(contactNumber);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

}
