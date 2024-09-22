package com.example.tamilvoice;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Locale;
import android.os.Handler;

public class PhoneCall extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int TTS_CHECK_CODE = 123;
    private static final int CONTACT_PICK_CODE = 1;
    private TextToSpeech textToSpeech;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TextToSpeech(this, this);
    }

    public void openContactsApp(View view) {
        // Launch the Contacts app
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        startActivityForResult(intent, CONTACT_PICK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONTACT_PICK_CODE && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{ContactsContract.Contacts._ID};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                String contactId = cursor.getString(contactIdIndex);
                cursor.close();

                // Query phone numbers for the contact using the contact ID
                Cursor phoneCursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    int numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    phoneNumber = phoneCursor.getString(numberIndex);
                    String contactName = getContactName(contactId);

                    // Speak the contact name
                    speakOut("Calling " + contactName);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Make a call to the selected contact
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + phoneNumber));
                            startActivity(callIntent);
                        }
                    }, 2000);
                }

                if (phoneCursor != null) {
                    phoneCursor.close();
                }
            }
        }
    }

    private String getContactName(String contactId) {
        String contactName = "";
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts._ID + " = ?",
                new String[]{contactId},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            contactName = cursor.getString(nameIndex);
            cursor.close();
        }
        return contactName;
    }

    private void speakOut(String text) {
        // Set speech parameters
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ID");

        // Speak the text
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ID");
    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("ta", "IND"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language not supported, handle error
            } else {
                // Enable speech after TTS initialized
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        // Speak phone number after contact name
                       // speakOut(phoneNumber);
                    }

                    @Override
                    public void onError(String utteranceId) {}
                });
            }
        } else {
            // Initialization failed, handle error
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the TTS engine
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
