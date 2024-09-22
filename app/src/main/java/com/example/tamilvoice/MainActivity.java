package com.example.tamilvoice;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;
//import com.google.android.gms.location.LocationListener;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.View;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.BatteryManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int SPEECH_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 3;
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CONTACT_REQUEST_CODE=4;
    private static final int REQUEST_CODE_IMAGE_CAPTURE = 103;
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 102;
    private static final int REQUEST_CODE_MCAMERA_PERMISSION = 104;
    private static final int REQUEST_CODE_MIMAGE_CAPTURE = 105;


    private Button btnListen;
    private TextView txtBatteryPercentage;
    int longt, latt;
    public String adr, sub_city, city;
    public Criteria criteria;
    public String bestProvider;


    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView image = findViewById(R.id.imageView);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform the desired action or start an activity here
                startSpeechRecognition();

            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new BatteryRecognitionListener());

        textToSpeech = new TextToSpeech(this, this);
    }


    private void startSpeechRecognition() {
        // Check for microphone permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            // Start speech recognition
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "உங்களுக்கு எவ்வாறு உதவ வேண்டும்");
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start speech recognition
                startSpeechRecognition();
            } else if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            }
            else if (requestCode == REQUEST_CODE_CAMERA_PERMISSION &&(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) ){
                startCamera();
            }
            else if (requestCode == REQUEST_CODE_MCAMERA_PERMISSION &&(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) ){
                startMCamera();
            }

            else {
                Toast.makeText(this, " permission required ", Toast.LENGTH_SHORT).show();
            }


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String command = results.get(0);
                processCommand(command);
            }
        }

        else if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (imageBitmap != null) {
                recognizeTextFromImage(imageBitmap);
            }

        }
        else if (requestCode == REQUEST_CODE_MIMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            if (imageBitmap != null) {
                recognizeMoneyFromImage(imageBitmap);
            }

        }


        super.onActivityResult(requestCode, resultCode, data);

    }

    private void processCommand(String command){
        if(command.equalsIgnoreCase("options")){
            listAllCommands();
        }
        else if (command.equalsIgnoreCase("battery percentage")) {
            // Get battery percentage and speak it
            speakBatteryPercentage();
        } else if (command.equalsIgnoreCase("date")) {
            speakCurrentDate();
        } else if (command.equalsIgnoreCase("time")) {
            speakCurrentTime();
        }
        else if (command.equalsIgnoreCase("location")) {
            getLocation();
        }
        else if (command.equalsIgnoreCase("weather")) {
            // Get current weather and speak it
            weather_service();
        }
        else if (command.equalsIgnoreCase("text")) {
            // Open camera for text recognition
            //openCameraForTextRecognition();
            speakText("படத்தைப் பிடித்து, வலது கீழே சோளத்தில் வைக்கப்பட்டுள்ள சரி என்பதைக் கிளிக் செய்யவும்");
            checkCameraPermission();
        }
        else if(command.equalsIgnoreCase("Money")){
            speakText("படத்தைப் பிடித்து, வலது கீழே சோளத்தில் வைக்கப்பட்டுள்ள சரி என்பதைக் கிளிக் செய்யவும்");
            checkMoneyCameraPermission();

        }
        else if(command.equalsIgnoreCase("Contact")){
            //openCameraForObject();
            speakText("மேல் வலது மூலையில் வைக்கப்பட்டுள்ள பொத்தானைத் தட்டவும்");
        }
        else if(command.equalsIgnoreCase("explore")){
            //openCameraForObject();
            speakText("மேல் இடது மூலையில் வைக்கப்பட்டுள்ள பொத்தானைத் தட்டவும்");
        }
        else if(command.equalsIgnoreCase("Face")){
            //openCameraForObject();
            speakText("திரையின் கீழே வைக்கப்பட்டுள்ள பொத்தானைத் தட்டவும்");
        }

        else {
            speakText("மன்னிக்கவும், உங்கள் கட்டளை எனக்குப் புரியவில்லை");
        }

    }
    public void GotoFaceRecognitionActivity(View view){
        Intent intent = new Intent(this,FaceRecognitionActivity.class);
        startActivity(intent);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }
    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_CODE_IMAGE_CAPTURE);
        }
    }
    private void recognizeTextFromImage(Bitmap imageBitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();

        if (!textRecognizer.isOperational()) {
            Toast.makeText(this, "Text recognition not available", Toast.LENGTH_SHORT).show();
        } else {
            Frame imageFrame = new Frame.Builder()
                    .setBitmap(imageBitmap)
                    .build();

            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

            if (textBlocks != null && textBlocks.size() > 0) {
                StringBuilder recognizedText = new StringBuilder();
                for (int i = 0; i < textBlocks.size(); i++) {
                    TextBlock textBlock = textBlocks.valueAt(i);
                    recognizedText.append(textBlock.getValue());
                    recognizedText.append("\n");
                }
                final String recognizeText = recognizedText.toString();
                // Output the recognized text as voice output
                if (textToSpeech != null && !textToSpeech.isSpeaking()) {
                    textToSpeech.speak(recognizeText, TextToSpeech.QUEUE_FLUSH, null, null);
                }

            }
            else {

                Toast.makeText(this, "No text found in the image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void checkMoneyCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CODE_MCAMERA_PERMISSION);
        } else {
            startMCamera();
        }
    }
    private void startMCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_CODE_MIMAGE_CAPTURE);
        }
    }
    private void recognizeMoneyFromImage(Bitmap imageBitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();

        if (!textRecognizer.isOperational()) {
            Toast.makeText(this, "Text recognition not available", Toast.LENGTH_SHORT).show();
        } else {
            Frame imageFrame = new Frame.Builder()
                    .setBitmap(imageBitmap)
                    .build();

            SparseArray<TextBlock> textBlock = textRecognizer.detect(imageFrame);

            if (textBlock != null && textBlock.size() > 0) {
                StringBuilder recognizedText = new StringBuilder();
                for (int i = 0; i < textBlock.size(); i++) {
                    TextBlock textBlocks = textBlock.valueAt(i);
                    recognizedText.append(textBlocks.getValue());
                    recognizedText.append("\n");
                }
                final String recognizeText = recognizedText.toString();
                if (recognizeText.contains("500")) {
                    speakText("அது ஐநூறு ரூபாய்");
                }
                else if (recognizeText.contains("200")){
                    speakText("இருநூறு ரூபாய்");

                }
                else if (recognizeText.contains("100")){
                    speakText("அது நூறு ரூபாய்");

                }
                else if (recognizeText.contains("50")){
                    speakText("அது ஐம்பது ரூபாய்");

                }
                else if (recognizeText.contains("20")){
                    speakText("இருபது ரூபாய்");

                }
                else if (recognizeText.contains("10")){
                    speakText("பத்து ரூபாய்");

                }
                else{
                    speakText("மன்னிக்கவும் உங்கள் பணம் அங்கீகரிக்கப்படவில்லை");
                }
            }
            else {

                Toast.makeText(this, "No text found in the image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void openContactsApp(View view) {
        Intent intent = new Intent(MainActivity.this, PhoneCall.class);
        startActivity(intent);
    }
    private void startApp(){
        speakText("பார்வையற்ற உதவியாளர் விண்ணப்பத்திற்கு வரவேற்கிறோம் "+" " +"இந்தப் பயன்பாட்டில் உள்ள அம்சங்களை அறிய, திரையைத் தட்டி, \"options\" எனக் கூறவும்");
        //Options();


    }





    private void listAllCommands() {
        StringBuilder commands = new StringBuilder();
        commands.append("Available commands are: ");
        commands.append("Battery Percentage, ");
        commands.append("Date, ");
        commands.append("Time, ");
        commands.append("Location, ");
        commands.append("Weather, ");
        commands.append("Text,");
        commands.append("Contact,");
        commands.append("Explore,");
        commands.append("Face.");



        speakText(commands.toString());
    }


    private void speakBatteryPercentage() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BatteryBroadcastReceiver batteryReceiver = new BatteryBroadcastReceiver() {
            @Override
            public void onReceiveBatteryLevel(float batteryPercentage) {
                // Display battery percentage on TextView
               // txtBatteryPercentage.setText(String.format(Locale.getDefault(), "Battery Percentage: %.0f%%", batteryPercentage));

                // Speak the battery percentage
                textToSpeech.speak(String.format(Locale.getDefault(), "பேட்டரி சதவீதம்:%.0f", batteryPercentage), TextToSpeech.QUEUE_FLUSH, null, "battery");
            }
        };

        // Register the battery receiver
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language of the TTS engine
            int result = textToSpeech.setLanguage(new Locale("ta", "IND"));
            startApp();
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Text-to-speech language not supported", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Text-to-speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    public class BatteryRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
        }

        @Override
        public void onResults(Bundle results) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }

    public abstract class BatteryBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPercentage = (level / (float) scale) * 100;

                onReceiveBatteryLevel(batteryPercentage);
            }
        }

        public abstract void onReceiveBatteryLevel(float batteryPercentage);
    }

    private void speakCurrentDate() {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        speakText("இன்றைய தேதி " + currentDate);
    }

    //private void openCameraForTextRecognition() {
        // Start the camera activity for text recognition
        //Intent intent = new Intent(MainActivity.this, Camera.class);
        //startActivityForResult(intent, CAMERA_REQUEST_CODE);

    //}

    private void speakCurrentTime() {
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        speakText("இப்போது நேரம் " + currentTime);
    }

    private void speakText(String text) {
        textToSpeech.setLanguage(new Locale("ta", "IND"));
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            criteria = new Criteria();
            bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                longt = (int) longitude;
                latt = (int) latitude;
                try {
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                    List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);

                    adr = addressList.get(0).getFeatureName();
                    sub_city = addressList.get(0).getSubLocality();
                    city = addressList.get(0).getLocality();
                    speakText("இப்போது உங்கள் இருப்பிடம்" + adr + "," + sub_city + "," + city + ",");
                    //Toast.makeText(MainActivity.this,"adr "+adr+" sub_city "+sub_city+" city "+city,Toast.LENGTH_LONG).show();
                    //Toast.makeText(MainActivity.this,"long"+(int)longitude+"lat"+(int)latitude,Toast.LENGTH_LONG).show();


                } catch (IOException e) {
                    e.printStackTrace();
                }


            } else {
                locationManager.requestLocationUpdates(bestProvider, 1000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        longt = (int) longitude;
                        latt = (int) latitude;
                        try {
                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);

                            adr = addressList.get(0).getFeatureName();
                            sub_city = addressList.get(0).getSubLocality();
                            city = addressList.get(0).getLocality();
                            speakText("இப்போது உங்கள் இருப்பிடம்" + adr + "," + sub_city + "," + city + ",");

                            //Toast.makeText(MainActivity.this,"adr "+adr+" sub_city "+sub_city+" city "+city,Toast.LENGTH_LONG).show();
                            //Toast.makeText(MainActivity.this,"long"+(int)longitude+"lat"+(int)latitude,Toast.LENGTH_LONG).show();

                            locationManager.removeUpdates(locationListener);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                });
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
    }
    class Weather extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... address) {
            try {
                URL url = new URL(address[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream is = connection.getInputStream();
                InputStreamReader irs = new InputStreamReader(is);

                int data = irs.read();
                String content = "";
                char ch;
                while (data != -1){
                    ch = (char) data;
                    content = content + ch;
                    data = irs.read();
                }
                return content;
            }catch (Exception e){

            }
            return null;
        }
    }
    private void weather_service(){

        String content;
        Weather weather = new Weather();


        try {
            content = weather.execute("https://api.openweathermap.org/data/2.5/weather?lat="+latt+"&lon="+longt+"&appid=770ef0ba8d3682a9eb278d543bbd8f72&units=metric").get();
            //Toast.makeText(this,content,Toast.LENGTH_LONG).show();
            //Log.i("contentdata",content);

            JSONObject jsonObject = new JSONObject(content);
            String weatherData = jsonObject.getString("weather");
            String mainTemperature = jsonObject.getString("main");
            //Toast.makeText(this,weatherData,Toast.LENGTH_LONG).show();

            // Log.i("weatherdata",weatherData);

            JSONArray array = new JSONArray(weatherData);

            String main = "";
            String description="";
            String temperature ="";
            final String city_Name = "";
            for (int i=0; i<array.length();i++){
                JSONObject weatherpart = array.getJSONObject(i);
                main = weatherpart.getString("main");
                description = weatherpart.getString("description");
            }
            JSONObject mainpart = new JSONObject(mainTemperature);
            temperature = mainpart.getString("temp");
            double temp_int = Double.parseDouble(temperature);
            temp_int = Math.round(temp_int);
            int t_value = (int) temp_int;
            speakText("உங்கள் வெப்பநிலை"+t_value +"டிகிரி செல்சியஸ்");
            //Toast.makeText(this,"Temperature"+resultText,Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"Exception"+e.getMessage(),Toast.LENGTH_LONG).show();
        }





    }
    public void ObjectActivity(View view) {
        Intent intent = new Intent(this, ObjectDetectionActivity.class);
        startActivity(intent);
    }








}
