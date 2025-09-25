package com.example.womensafetyapp2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.client.http.FileContent;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.util.Collections;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import java.util.Arrays;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.http.javanet.NetHttpTransport;


import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // SMS Timer variables
    private Handler smsHandler;
    private Runnable smsRunnable;
    private boolean isSmsTimerActive = false;

    private static final String RECORDINGS_FOLDER = "WomenSafetyRecordings";
    private Drive driveService;
    private GoogleAccountCredential driveCredential;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private ActivityResultLauncher<IntentSenderRequest> authorizationLauncher;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private List<String> emergencyContacts;
    private MediaPlayer sirenPlayer;
    private AudioRecorder audioRecorder;

    // Voice recognition components
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private boolean isListening = false;
    private boolean isRecordingCustomTrigger = false;

    // Custom trigger words
    private List<String> customTriggerWords;
    private SharedPreferences preferences;


    // UI components
    private Button alertButton;
    private Button recordButton;
    private Button voiceListenButton;
    private Button customTriggerButton;
    private Button contactsButton;
    private Button cloudStorageButton;
    private TextView statusText;
    private Button modeToggleButton;
    private TextView modeStatusText;
    private BroadcastReceiver smsSentReceiver;
    private BroadcastReceiver smsDeliveredReceiver;

    // SIM and SMS verification
    private SubscriptionManager subscriptionManager;
    private TelephonyManager telephonyManager;
    private String selectedPhoneNumber;
    private int selectedSubscriptionId = -1;
    private ConnectivityManager connectivityManager;
    private Handler connectivityHandler;
    private Runnable connectivityChecker;

    // WhatsApp timer
    private Handler whatsappHandler;
    private Runnable whatsappRunnable;
    private boolean isWhatsappTimerActive = false;
    private int whatsappMessageCount = 0;
    private int currentContactIndex = 0;
    private String currentEmergencyMessage = "";
    private boolean isEmergencyActive = false;
    private boolean isWhatsAppFlowActive = false;
    private static final String WHATSAPP_CHANNEL_ID = "whatsapp_progress_channel";
    private static final int WHATSAPP_NOTIFICATION_ID = 1001;

    // Mode and authentication fields
    private boolean isGuardianMode = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();
        initializeUI();
        checkAndRequestPermissions();
        setupLocationServices();
        setupVoiceRecognition();
        loadCustomTriggerWords();
        loadEmergencyContacts();
        startBackgroundGuardianMonitoring();
    }


    private void requestDrivePermissions(GoogleSignInAccount account) {
        Log.d("GoogleDrive", "Requesting Drive permissions for: " + account.getEmail());
        GoogleSignIn.requestPermissions(
                this,
                RC_SIGN_IN + 1,
                account,
                new Scope(DriveScopes.DRIVE_FILE)
        );
    }

    private void signInToGoogleDrive() {
        Log.d("GoogleDrive", "=== Starting Google Drive sign-in ===");

        // Sign out first to ensure clean sign-in
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d("GoogleDrive", "Signed out, starting fresh sign-in");
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("GoogleDrive", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("GoogleDrive", "✅ Sign-in successful: " + account.getEmail());
                Log.d("GoogleDrive", "Account ID: " + account.getId());
                Log.d("GoogleDrive", "Server Auth Code: " + (account.getServerAuthCode() != null ? "Present" : "Null"));

                // Check Drive permissions
                if (GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE_FILE))) {
                    Log.d("GoogleDrive", "✅ Drive permissions already granted");
                    driveCredential.setSelectedAccount(account.getAccount());
                    initializeDriveService();
                    Toast.makeText(this, "✅ Google Drive connected: " + account.getEmail(), Toast.LENGTH_LONG).show();
                } else {
                    Log.d("GoogleDrive", "⚠️ Need to request Drive permissions");
                    requestDrivePermissions(account);
                }

            } catch (ApiException e) {
                Log.e("GoogleDrive", "❌ Sign-in failed with status code: " + e.getStatusCode());
                Log.e("GoogleDrive", "Error message: " + e.getMessage());

                String errorMessage = getSignInErrorMessage(e.getStatusCode());
                Toast.makeText(this, "❌ Google Drive sign-in failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }

        } else if (requestCode == RC_SIGN_IN + 1) {
            // Handle Drive permission result
            Log.d("GoogleDrive", "Drive permission result: " + resultCode);
            if (resultCode == RESULT_OK) {
                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                if (account != null) {
                    driveCredential.setSelectedAccount(account.getAccount());
                    initializeDriveService();
                    Toast.makeText(this, "✅ Google Drive permissions granted!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "⚠️ Google Drive permissions denied. Evidence will be stored locally only.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getSignInErrorMessage(int statusCode) {
        switch (statusCode) {
            case 10: return "Developer error - Check SHA-1 fingerprint in Google Cloud Console";
            case 12500: return "Sign-in cancelled by user";
            case 12501: return "Sign-in currently in progress";
            case 12502: return "Network error - Check internet connection";
            default: return "Unknown error (Code: " + statusCode + ")";
        }
    }

    private void initializeDriveService() {
        new Thread(() -> {
            try {
                Log.d("GoogleDrive", "Initializing Drive service...");

                driveService = new Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory.getDefaultInstance(),
                        driveCredential)
                        .setApplicationName("Women Safety App")
                        .build();

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Google Drive connected successfully!", Toast.LENGTH_SHORT).show();
                    Log.d("GoogleDrive", "Drive service ready!");
                });

            } catch (Exception e) {
                Log.e("GoogleDrive", "Drive service initialization failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Drive setup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void uploadToDrive(java.io.File localFile) {
        if (driveService == null) {
            Log.w("GoogleDrive", "Drive service not initialized, starting sign-in");
            signInToGoogleDrive();
            return;
        }

        Log.d("GoogleDrive", "Starting upload: " + localFile.getName());

        new Thread(() -> {
            try {
                // Create file metadata (using full class name to avoid conflict)
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName("Evidence_" + System.currentTimeMillis() + "_" + localFile.getName());
                fileMetadata.setDescription("Emergency evidence from Women Safety App");

                // Create file content
                FileContent mediaContent = new FileContent("audio/3gpp", localFile);

                // Upload to Drive
                com.google.api.services.drive.model.File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id,name,size")
                        .execute();

                Log.d("GoogleDrive", "Upload successful! File ID: " + uploadedFile.getId());

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ " + localFile.getName() + " uploaded to Google Drive!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e("GoogleDrive", "Upload failed for: " + localFile.getName(), e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void uploadEvidenceToCloud() {
        Log.d("GoogleDrive", "Starting evidence upload...");

        // CORRECT - Look in the same place where recordings are saved
        java.io.File recordingsDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (recordingsDir == null) {
            // Fallback to internal storage
            recordingsDir = new java.io.File(getFilesDir(), "recordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }
            if (!recordingsDir.exists()) {
                Toast.makeText(this, "📁 No recordings found to upload", Toast.LENGTH_SHORT).show();
                return;
            }

            java.io.File[] files = recordingsDir.listFiles();
            if (files == null || files.length == 0) {
                Toast.makeText(this, "📁 No evidence files found in " + recordingsDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                return;
            }

            int uploadCount = 0;
            for (java.io.File file : files) {
                if (file.getName().startsWith("evidence_recording_") && (file.getName().endsWith(".3gp") || file.getName().endsWith(".mp4"))) {
                    Log.d("GoogleDrive", "Found evidence file: " + file.getName() + " (size: " + file.length() + " bytes)");
                    uploadToDrive(file);
                    uploadCount++;
                }
            }

            if (uploadCount > 0) {
                Toast.makeText(this, "📤 Uploading " + uploadCount + " evidence files...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "📁 No audio/video evidence found to upload", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showCloudStorageSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("☁️ Google Drive Storage");

        TextView messageView = new TextView(this);
        String status = (driveService != null) ? "✅ Connected & Ready" : "❌ Not Connected";
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        String accountInfo = (account != null) ? "\n👤 Account: " + account.getEmail() : "\n👤 No account signed in";

        messageView.setText("Google Drive Status: " + status + accountInfo + "\n\n" +
                "🔹 Automatic evidence backup to your Google Drive\n" +
                "🔹 Files stored securely in your personal Drive\n" +
                "🔹 Access from any device with your Google account\n" +
                "🔹 15GB free storage included\n\n" +
                "📁 Evidence files are uploaded with timestamp and 'Evidence_' prefix for easy identification.");
        messageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(5, 1.2f);

        builder.setView(messageView);

        if (driveService == null) {
            builder.setPositiveButton("🔐 Connect Google Drive", (dialog, which) -> signInToGoogleDrive());
        } else {
            builder.setPositiveButton("📤 Upload Evidence Now", (dialog, which) -> uploadEvidenceToCloud());
        }
        builder.setNegativeButton("Close", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }


    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager;

            // Use selected SIM if available
            if (selectedSubscriptionId != -1) {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(selectedSubscriptionId);
            } else {
                smsManager = SmsManager.getDefault();
            }

            // Split long messages
            ArrayList<String> parts = smsManager.divideMessage(message);

            if (parts.size() == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            }

            Log.d("SMS", "SMS sent to: " + phoneNumber);

        } catch (Exception e) {
            Log.e("SMS", "Failed to send SMS to " + phoneNumber + ": " + e.getMessage());
            Toast.makeText(this, "❌ Failed to send SMS to " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendLocationToContacts(Location location) {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "❌ No emergency contacts configured", Toast.LENGTH_SHORT).show();
            return;
        }

        String locationMessage = "🆘 EMERGENCY LOCATION UPDATE 🆘\n" +
                "Current Location: https://maps.google.com/?q=" +
                location.getLatitude() + "," + location.getLongitude() + "\n" +
                "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n" +
                "Accuracy: " + Math.round(location.getAccuracy()) + "m";

        for (String contact : emergencyContacts) {
            sendSMS(contact, locationMessage);
        }

        Toast.makeText(this, "📍 Location sent to " + emergencyContacts.size() + " contacts", Toast.LENGTH_SHORT).show();
    }

    private void initializeDriveService(AuthorizationResult authorizationResult) {
        try {
            // Get the current signed-in Google account
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account == null) {
                Toast.makeText(this, "❌ No Google account signed in", Toast.LENGTH_LONG).show();
                return;
            }

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            // Store credential for later use
            this.driveCredential = credential;

            Toast.makeText(this, "✅ Google Drive service initialized!", Toast.LENGTH_SHORT).show();
            Log.d("GoogleDrive", "Drive service ready for user: " + account.getEmail());

        } catch (Exception e) {
            Toast.makeText(this, "❌ Failed to initialize Drive service", Toast.LENGTH_LONG).show();
            Log.e("GoogleDrive", "Drive service initialization failed", e);
        }
    }

    private void requestGoogleDriveAccess() {
        List<Scope> requestedScopes = Arrays.asList(new Scope(DriveScopes.DRIVE_FILE));

        AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)
                .build();

        Identity.getAuthorizationClient(this)
                .authorize(authorizationRequest)
                .addOnSuccessListener(authorizationResult -> {
                    if (authorizationResult.hasResolution()) {
                        // User needs to grant permission
                        try {
                            authorizationLauncher.launch(
                                    new IntentSenderRequest.Builder(
                                            authorizationResult.getPendingIntent().getIntentSender()
                                    ).build()
                            );
                        } catch (Exception e) {
                            Toast.makeText(this, "❌ Failed to start authorization", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Permission already granted
                        initializeDriveService(authorizationResult);
                        Toast.makeText(this, "✅ Google Drive already authorized!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Authorization request failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("GoogleDrive", "Authorization request failed", e);
                });
    }

    private void loadCustomTriggerWords() {
        String savedTriggers = preferences.getString("custom_triggers", "");
        customTriggerWords.clear();
        if (!savedTriggers.isEmpty()) {
            String[] triggers = savedTriggers.split(",");
            for (String trigger : triggers) {
                if (!trigger.trim().isEmpty()) {
                    customTriggerWords.add(trigger.trim());
                }
            }
        }
        Log.d("CustomTrigger", "Loaded triggers: " + customTriggerWords.toString());
    }


    private void enableGuardianMode() {
        Toast.makeText(this, "🛡️ Guardian Mode activated - monitoring in background", Toast.LENGTH_LONG).show();

        // Start background service that runs ALWAYS
        Intent serviceIntent = new Intent(this, GuardianService.class);
        startService(serviceIntent);
    }

    private void disableGuardianMode() {
        Toast.makeText(this, "👤 Personal Mode activated - Guardian monitoring continues in background", Toast.LENGTH_SHORT).show();
    }

    private void initializeComponents() {
        preferences = getSharedPreferences("WomenSafetyApp", MODE_PRIVATE);
        customTriggerWords = new ArrayList<>();
        emergencyContacts = new ArrayList<>();

        // Initialize Google Sign-In
        setupGoogleSignIn();

        // Initialize Google Drive credential
        driveCredential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(DriveScopes.DRIVE_FILE));
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void uploadToGoogleDrive(File file) {
        // Verify file exists and is readable
        if (!file.exists()) {
            android.util.Log.e("GoogleDrive", "File does not exist: " + file.getAbsolutePath());
            Toast.makeText(this, "❌ Recording file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if Drive service is available
        if (driveService == null) {
            Toast.makeText(this, "⚠️ Google Drive not connected. Please connect first.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // Create metadata for file (NO parent folder - store in root)
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName("Evidence_" + System.currentTimeMillis() + "_" + file.getName());
                fileMetadata.setDescription("Emergency evidence from Women Safety App");

                // Create file content
                FileContent mediaContent = new FileContent("audio/3gpp", file);

                // Upload file using the initialized driveService
                com.google.api.services.drive.model.File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, size")
                        .execute();

                String fileId = uploadedFile.getId();

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ Evidence uploaded to Google Drive: " + file.getName(), Toast.LENGTH_SHORT).show();
                    android.util.Log.d("GoogleDrive", "File uploaded successfully. ID: " + fileId);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ Drive upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    android.util.Log.e("GoogleDrive", "Upload failed", e);
                });
            }
        }).start();
    }

    private boolean isEmergencyService(String phoneNumber) {
        if (phoneNumber == null) return false;
        String cleanNumber = phoneNumber.trim().replaceAll("[^0-9]", "");
        return cleanNumber.equals("911") || cleanNumber.equals("999") || 
               cleanNumber.equals("112") || cleanNumber.equals("000") ||
               cleanNumber.equals("100") || cleanNumber.equals("101") ||
               cleanNumber.equals("102") || cleanNumber.equals("108");
    }

    private void toggleMode() {
        isGuardianMode = !isGuardianMode;
        updateModeDisplay();

        if (isGuardianMode) {
            enableGuardianMode();
        } else {
            // Switch to personal mode but keep guardian monitoring active
            disableGuardianMode();
        }

        // Save mode preference
        preferences.edit().putBoolean("guardian_mode", isGuardianMode).apply();
    }

    private void updateModeDisplay() {
        if (isGuardianMode) {
            modeToggleButton.setText("👤 Switch to Personal Mode");
            modeStatusText.setText("🛡️ GUARDIAN MODE: Actively monitoring + can send alerts");
            modeStatusText.setTextColor(0xFF8E24AA); // Purple color
        } else {
            modeToggleButton.setText("🛡️ Switch to Guardian Mode");
            modeStatusText.setText("👤 PERSONAL MODE: Can send alerts + monitoring in background");
            modeStatusText.setTextColor(0xFFE91E63); // Pink color
        }
    }

    private void startBackgroundGuardianMonitoring() {
        // Start guardian service for EVERYONE - always monitor for emergency alerts
        try {
            Intent serviceIntent = new Intent(this, GuardianService.class);
            startService(serviceIntent);
            Toast.makeText(this, "🛡️ Emergency alert monitoring active", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "⚠️ Guardian service failed to start: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeUI() {
        alertButton = findViewById(R.id.alertButton);
        recordButton = findViewById(R.id.recordButton);
        voiceListenButton = findViewById(R.id.voiceListenButton);
        customTriggerButton = findViewById(R.id.customTriggerButton);
        contactsButton = findViewById(R.id.contactsButton);
        cloudStorageButton = findViewById(R.id.cloudStorageButton);
        statusText = findViewById(R.id.statusText);
        modeToggleButton = findViewById(R.id.modeToggleButton);
        modeStatusText = findViewById(R.id.modeStatusText);

        // Check if UI components exist before setting up listeners
        if (alertButton == null || recordButton == null || voiceListenButton == null) {
            Toast.makeText(this, "⚠️ UI components not found in layout", Toast.LENGTH_LONG).show();
            return;
        }

        // STEP 3: Silent Emergency Alert (no siren on victim's phone)
        alertButton.setOnClickListener(v -> {
            if (isEmergencyActive) {
                new AlertDialog.Builder(this)
                        .setTitle("🛑 Stop Emergency?")
                        .setMessage("If authorities have arrived or you're safe, you can stop emergency mode now.")
                        .setPositiveButton("Stop", (d, w) -> stopEmergencyNow())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                triggerSilentEmergencyAlert();
            }
        });

        recordButton.setOnClickListener(v -> {
            if (audioRecorder == null) {
                startEvidenceRecording();
            } else {
                stopEvidenceRecording();
            }
        });

        voiceListenButton.setOnClickListener(v -> {
            if (!isListening) {
                startVoiceListening();
            } else {
                stopVoiceListening();
            }
        });

        customTriggerButton.setOnClickListener(v -> showCustomTriggerDialog());
        contactsButton.setOnClickListener(v -> openEmergencyContactsActivity());
        cloudStorageButton.setOnClickListener(v -> showCloudStorageSettings());
        
        // Fix cloud storage button text
        if (cloudStorageButton != null) {
            cloudStorageButton.setText("☁️ Cloud Storage");
        }

        // Mode toggle button setup
        if (modeToggleButton != null) {
            modeToggleButton.setOnClickListener(v -> toggleMode());
        }

        updateModeDisplay();
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void setupVoiceRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    if (isRecordingCustomTrigger) {
                        statusText.setText("🎤 Say your custom trigger word now...");
                    } else {
                        statusText.setText("🌸 Listening for emergency words... 🌸");
                    }
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    if (isListening && !isRecordingCustomTrigger) {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onError(int error) {
                    if (isListening && !isRecordingCustomTrigger) {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null) {
                        if (isRecordingCustomTrigger) {
                            handleCustomTriggerRecording(matches);
                        } else {
                            for (String match : matches) {
                                if (isEmergencyTrigger(match)) {
                                    triggerSilentEmergencyAlert(); // Silent mode
                                    return;
                                }
                            }
                        }
                    }
                    if (isListening && !isRecordingCustomTrigger) {
                        speechRecognizer.startListening(speechRecognizerIntent);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty() && !isRecordingCustomTrigger) {
                        String partial = matches.get(0);
                        if (isEmergencyTrigger(partial)) {
                            triggerSilentEmergencyAlert(); // Silent mode
                        }
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
    }

    private boolean isEmergencyTrigger(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        String lowerText = text.toLowerCase().trim();
        android.util.Log.d("VoiceRecognition", "Checking text: '" + lowerText + "'");

        // Default emergency triggers
        String[] defaultTriggers = {"help me", "danger", "emergency", "help", "sos", "call police", "need help"};
        for (String trigger : defaultTriggers) {
            if (lowerText.contains(trigger)) {
                android.util.Log.d("VoiceRecognition", "Default trigger detected: " + trigger);
                return true;
            }
        }

        // Custom triggers
        android.util.Log.d("VoiceRecognition", "Custom triggers count: " + customTriggerWords.size());
        for (String customTrigger : customTriggerWords) {
            if (customTrigger != null && !customTrigger.trim().isEmpty()) {
                String cleanCustomTrigger = customTrigger.toLowerCase().trim();
                if (lowerText.contains(cleanCustomTrigger)) {
                    android.util.Log.d("VoiceRecognition", "Custom trigger detected: " + cleanCustomTrigger);
                    return true;
                }
            }
        }

        return false;
    }

    // STEP 3: SILENT EMERGENCY ALERT - Works with ANY phone number (911, 999, etc.)
    private void triggerSilentEmergencyAlert() {
        try {
            if (emergencyContacts.isEmpty()) {
                Toast.makeText(this, "⚠️ Please add emergency contacts first!", Toast.LENGTH_LONG).show();
                openEmergencyContactsActivity();
                return;
            }

            // COMPLETELY SILENT MODE - No visual or audio alerts that could expose victim
            Toast.makeText(this, "🤫 Silent emergency alert activated...", Toast.LENGTH_SHORT).show();
            if (statusText != null) {
                statusText.setText("🤫 Emergency alert sent silently...");
            }

            sendUniversalEmergencyAlert(); // Works with ANY phone number
            startLocationTracking();
            startAutoEvidenceRecording(); // Auto-record evidence
            uploadEvidenceToCloud(); // Immediate cloud backup
            // REMOVED: activateEnhancedEmergencyMode() - dangerous flashlight removed
            
            // Start WhatsApp timer
            startWhatsAppTimer();
            
            // Mark emergency active and update UI
            isEmergencyActive = true;
            if (alertButton != null) {
                alertButton.setText("🛑 STOP EMERGENCY");
            }
            if (statusText != null) {
                statusText.setText("🚨 Emergency active - alerts in progress");
            }
        } catch (Exception e) {
            Toast.makeText(this, "❌ Emergency alert failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.util.Log.e("EmergencyAlert", "Failed to trigger emergency alert", e);
        }
    }

    // UNIVERSAL EMERGENCY ALERT - Works with 911, 999, regular contacts, etc, recommended by Windsurf
    private void sendUniversalEmergencyAlert() {
        // Prevent multiple SMS timers from running
        if (isSmsTimerActive) {
            android.util.Log.d("SMS_DEBUG", "SMS timer already active, skipping duplicate");
            return;
        }

        isSmsTimerActive = true;
        smsHandler = new Handler(Looper.getMainLooper());
        smsRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isEmergencyActive) {
                        isSmsTimerActive = false;
                        return; // Stop if emergency mode is cancelled
                    }

                    // Send SMS in background thread
                    new Thread(() -> {
                        try {
                            SmsManager smsManager = SmsManager.getDefault();
                            String message = "🚨 EMERGENCY ALERT 🚨\n" +
                                    "I am in danger and need immediate help!\n" +
                                    "Time: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

                            int sentCount = 0;
                            for (String number : emergencyContacts) {
                                try {
                                    String cleanNumber = number.trim().replaceAll("[^0-9+]", "");
                                    smsManager.sendTextMessage(cleanNumber, null, message, null, null);
                                    android.util.Log.d("SMS_DEBUG", "SMS sent to: " + cleanNumber);
                                    sentCount++;
                                    Thread.sleep(500);
                                } catch (Exception e) {
                                    android.util.Log.e("SMS_DEBUG", "Failed to send SMS to: " + number, e);
                                }
                            }

                            final int finalSentCount = sentCount;
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "📱 Emergency SMS sent to " + finalSentCount + " contacts", Toast.LENGTH_SHORT).show();
                            });

                        } catch (Exception e) {
                            android.util.Log.e("SMS_DEBUG", "Error sending SMS batch", e);
                        }
                    }).start();

                    // Schedule next SMS after 2 minutes
                    smsHandler.postDelayed(this, 120000);

                } catch (Exception e) {
                    android.util.Log.e("SMS_DEBUG", "Error in SMS sending cycle", e);
                }
            }
        };

        // Start SMS timer
        smsHandler.post(smsRunnable);
        Toast.makeText(this, "📱 Emergency SMS alerts started - sending every 2 minutes", Toast.LENGTH_LONG).show();
    }

    private void startAutoEvidenceRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (audioRecorder == null) {
                startEvidenceRecording();
                Toast.makeText(this, "🎵 Auto-recording evidence for safety...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startEvidenceRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            requestAllPermissions();
            return;
        }

        // Use a consistent path for recordings
        File recordingDir = new File(getExternalFilesDir(null), RECORDINGS_FOLDER);
        if (!recordingDir.exists()) {
            recordingDir.mkdirs();
        }

        // Fallback to internal storage if external fails
        if (!recordingDir.exists()) {
            recordingDir = new File(getFilesDir(), RECORDINGS_FOLDER);
            recordingDir.mkdirs();
        }

        audioRecorder = new AudioRecorder(recordingDir);
        try {
            audioRecorder.startRecording();
            recordButton.setText("🛑 Stop Evidence Recording");
            statusText.setText("🎵 Recording evidence... 🎵");

            // Log the exact path for debugging
            android.util.Log.d("Recording", "Recording to: " + recordingDir.getAbsolutePath());
            Toast.makeText(this, "✨ Evidence recording started ✨", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "❌ Recording failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            audioRecorder = null;
        }
    }


    private void stopEvidenceRecording() {
        try {
            File recordedFile = audioRecorder.stopRecording();
            recordButton.setText("🎵 Start Evidence Recording");
            statusText.setText("🌸 Evidence saved! 🌸");

            // Immediate Google Drive upload for evidence protection
            if (GoogleSignIn.getLastSignedInAccount(this) != null) {
                uploadToGoogleDrive(recordedFile);
            } else {
                Toast.makeText(this, "✨ Evidence saved locally: " + recordedFile.getName() + " ✨", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "❌ Failed to stop recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            audioRecorder = null;
        }
    }

    private void openEmergencyContactsActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📱 Emergency Contacts");

        // Create styled message with visible text
        StringBuilder contactList = new StringBuilder();
        contactList.append("Current Emergency Contacts:\n\n");

        if (emergencyContacts.isEmpty()) {
            contactList.append("No contacts added yet.\n\n");
        } else {
            for (int i = 0; i < emergencyContacts.size(); i++) {
                contactList.append((i + 1)).append(". ").append(emergencyContacts.get(i)).append("\n");
            }
            contactList.append("\n");
        }

        contactList.append("Emergency Alert Features:\n");
        contactList.append("• Silent mode (no sound on your phone for safety)\n");
        contactList.append("• Automatic evidence recording\n");
        contactList.append("• Location tracking and sharing\n");
        contactList.append("• Cloud evidence backup\n");
        contactList.append("• Works with 911, 999, and personal contacts\n\n");

        contactList.append("SMS Message Format:\n");
        contactList.append("Emergency services: Brief alert\n");
        contactList.append("Personal contacts: Detailed alert with evidence links");

        TextView messageView = new TextView(this);
        messageView.setText(contactList.toString());
        messageView.setTextColor(Color.BLACK);
        messageView.setTextSize(14);
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(3, 1.1f);
        builder.setView(messageView);
        
        builder.setPositiveButton("➕ Add Contact", (dialog, which) -> addEmergencyContact());

        if (!emergencyContacts.isEmpty()) {
            builder.setNeutralButton("🗑️ Remove Contact", (dialog, which) -> removeEmergencyContact());
        }

        builder.setNegativeButton("❌ Close", (dialog, which) -> {
            // Close dialog - no action needed
        });
        builder.show();
    }

    // STEP 1: FIXED VISIBLE INPUT - Enhanced EditText styling
    private void addEmergencyContact() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ Add Emergency Contact");

        TextView instructionView = new TextView(this);
        instructionView.setText("Enter phone number (with country code):\nExamples: +1234567890, 911, 999");
        instructionView.setTextColor(Color.BLACK);
        instructionView.setTextSize(16);
        instructionView.setPadding(50, 20, 50, 20);

        EditText input = new EditText(this);
        input.setHint("+1234567890 or 911");
        input.setPadding(50, 30, 50, 30);
        input.setTextSize(18);
        input.setTextColor(Color.BLACK); // BLACK TEXT - VISIBLE!
        input.setHintTextColor(Color.GRAY); // Gray hint
        input.setBackgroundColor(Color.WHITE); // White background
        input.setSingleLine(true);
        // Add border for better visibility
        input.setBackground(ContextCompat.getDrawable(this, android.R.drawable.edit_text));

        // Create container for both views
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.addView(instructionView);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("✅ Add", (dialog, which) -> {
            String phoneNumber = input.getText().toString().trim();
            if (isValidPhoneNumber(phoneNumber)) {
                // Restrict to ONE emergency contact to ensure WhatsApp works reliably
                if (!emergencyContacts.isEmpty()) {
                    emergencyContacts.clear();
                }
                emergencyContacts.add(phoneNumber);
                saveEmergencyContacts();
                Toast.makeText(this, "✨ Emergency contact set: " + phoneNumber + " (only one contact is allowed for WhatsApp reliability)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "❌ Invalid phone number", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("❌ Cancel", null);
        builder.show();
    }

    // ...
    private void removeEmergencyContact() {
        if (emergencyContacts.isEmpty()) return;

        String[] contactArray = emergencyContacts.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🗑️ Remove Emergency Contact");

        TextView messageView = new TextView(this);
        messageView.setText("Select contact to remove:");
        messageView.setTextColor(Color.BLACK);
        messageView.setTextSize(16);
        messageView.setPadding(50, 30, 50, 30);
        builder.setView(messageView);

        builder.setItems(contactArray, (dialog, which) -> {
            String removedContact = emergencyContacts.remove(which);
            saveEmergencyContacts();
            Toast.makeText(this, "✨ Contact removed: "  + removedContact + " ✨", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("❌ Cancel", (dialog, which) -> {
            // Close dialog - no action needed
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            List<String> deniedPermissions = new ArrayList<>();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (!allGranted) {
                StringBuilder message = new StringBuilder("⚠️ Some permissions were denied:\n\n");
                for (String denied : deniedPermissions) {
                    switch (denied) {
                        case Manifest.permission.SEND_SMS:
                            message.append("• SMS: Cannot send emergency alerts\n");
                            break;
                        case Manifest.permission.ACCESS_FINE_LOCATION:
                            message.append("• Location: Cannot share location\n");
                            break;
                        case Manifest.permission.RECORD_AUDIO:
                            message.append("• Microphone: Cannot use voice triggers or record evidence\n");
                            break;
                        case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                            message.append("• Storage: Cannot save evidence files\n");
                            break;
                    }
                }
                message.append("\nPlease enable these permissions in Settings for full functionality.");

                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Permissions Required")
                        .setMessage(message.toString())
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Close dialog - no action needed
                        })
                        .show();
            } else {
                Toast.makeText(this, "✅ All permissions already granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sirenPlayer != null) {
            sirenPlayer.release();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        // Unregister SMS receivers
        try {
            if (smsSentReceiver != null) {
                unregisterReceiver(smsSentReceiver);
            }
            if (smsDeliveredReceiver != null) {
                unregisterReceiver(smsDeliveredReceiver);
            }
        } catch (Exception e) {
            // Receivers may not be registered
        }
        
        stopVoiceListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isEmergencyActive) {
            if (statusText != null) {
                statusText.setText("🌸 Welcome back! Ready for emergency response 🌸");
            }
        }
        // Continue WhatsApp sequence when returning from WhatsApp
        if (isWhatsAppFlowActive && currentContactIndex < emergencyContacts.size()) {
            new Handler().postDelayed(this::sendWhatsAppToNextContact, 1200);
        }
    }

    private void loadEmergencyContacts() {
        String savedContacts = preferences.getString("emergency_contacts", "");
        emergencyContacts.clear();
        if (!savedContacts.isEmpty()) {
            emergencyContacts.addAll(Arrays.asList(savedContacts.split(",")));
        }
        
        // No default numbers added. Prompt user to add their trusted contact.
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(this, "⚠️ No emergency contact set. Please add ONE trusted WhatsApp contact.", Toast.LENGTH_LONG).show();
        }
        
        Toast.makeText(this, "📱 Loaded " + emergencyContacts.size() + " emergency contacts", Toast.LENGTH_SHORT).show();
    }

    private void saveEmergencyContacts() {
        String contactsString = String.join(",", emergencyContacts);
        preferences.edit().putString("emergency_contacts", contactsString).apply();
    }

    private void startVoiceListening() {
        if (speechRecognizer == null) {
            setupVoiceRecognition();
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "⚠️ Microphone permission required for voice monitoring", Toast.LENGTH_LONG).show();
            requestAllPermissions();
            return;
        }

        isListening = true;
        speechRecognizer.startListening(speechRecognizerIntent);
        voiceListenButton.setText("🔴 Stop Voice Monitor");
        statusText.setText("🎤 Listening for emergency triggers...");
    }

    private void stopVoiceListening() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        voiceListenButton.setText("🎤 Start Voice Monitor");
        statusText.setText("🌸 Voice monitoring stopped 🌸");
    }

    private void showCustomTriggerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎤 Custom Voice Triggers");

        TextView messageView = new TextView(this);
        messageView.setText("Current triggers:\n\n" +
                "• help me\n" +
                "• emergency\n" +
                "• call police\n\n" +
                "Custom triggers: " + customTriggerWords.toString() + "\n\n" +
                "Choose how to add new triggers:");
        messageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(5, 1.2f);

        builder.setView(messageView);
        builder.setPositiveButton("📝 Type Words", (dialog, which) -> showAddTriggerDialog());
        builder.setNeutralButton("🎙️ Record Words", (dialog, which) -> showRecordTriggerDialog());
        builder.setNegativeButton("🗑️ Delete Words", (dialog, which) -> showDeleteTriggerDialog());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void showDeleteTriggerDialog() {
        if (customTriggerWords.isEmpty()) {
            Toast.makeText(this, "No custom triggers to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🗑️ Delete Custom Triggers");

        String[] triggerArray = customTriggerWords.toArray(new String[0]);
        boolean[] checkedItems = new boolean[triggerArray.length];

        builder.setMultiChoiceItems(triggerArray, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("🗑️ Delete Selected", (dialog, which) -> {
            List<String> toDelete = new ArrayList<>();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    toDelete.add(triggerArray[i]);
                }
            }

            if (!toDelete.isEmpty()) {
                customTriggerWords.removeAll(toDelete);
                saveCustomTriggerWords();
                Toast.makeText(this, "✅ Deleted " + toDelete.size() + " trigger(s)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No triggers selected for deletion", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void saveCustomTriggerWords() {
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < customTriggerWords.size(); i++) {
            sb.append(customTriggerWords.get(i));
            if (i < customTriggerWords.size() - 1) {
                sb.append(",");
            }
        }
        editor.putString("custom_triggers", sb.toString());
        editor.apply();
        Log.d("CustomTrigger", "Saved triggers: " + customTriggerWords.toString());
    }

    private void showRecordTriggerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎙️ Record Custom Trigger");

        TextView messageView = new TextView(this);
        messageView.setText("Record yourself saying your custom trigger words clearly.\n\n" +
                "Tips:\n" +
                "• Speak clearly and distinctly\n" +
                "• Say each word separately\n" +
                "• Record in a quiet environment");
        messageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(5, 1.2f);

        builder.setView(messageView);
        builder.setPositiveButton("🎙️ Start Recording", (dialog, which) -> startCustomTriggerRecording());
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void startCustomTriggerRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your custom trigger words clearly...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, "🎙️ Recording... Say your trigger words now!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {
                Toast.makeText(MainActivity.this, "🎙️ Processing...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int error) {
                Toast.makeText(MainActivity.this, "❌ Recording failed. Try again.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String trigger = matches.get(0).toLowerCase().trim();
                    if (!customTriggerWords.contains(trigger)) {
                        customTriggerWords.add(trigger);
                        saveCustomTriggerWords();
                        Toast.makeText(MainActivity.this, "✅ Custom trigger added: " + trigger, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "⚠️ Trigger already exists: " + trigger, Toast.LENGTH_SHORT).show();
                    }
                }
                speechRecognizer.destroy();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void showAddTriggerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("➕ Add Custom Trigger");

        TextView messageView = new TextView(this);
        messageView.setText("Enter a custom trigger word or phrase:");
        messageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(5, 1.2f);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("e.g., help me now");
        input.setTextColor(ContextCompat.getColor(this, android.R.color.black)); // Fix text color
        input.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        input.setPadding(50, 30, 50, 30);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(messageView);
        layout.addView(input);

        builder.setView(layout);
        builder.setPositiveButton("✅ Add", (dialog, which) -> {
            String customTrigger = input.getText().toString().toLowerCase().trim();
            if (!customTrigger.isEmpty() && !customTriggerWords.contains(customTrigger)) {
                customTriggerWords.add(customTrigger);
                saveCustomTriggerWords();
                Toast.makeText(this, "✅ Custom trigger added: " + customTrigger, Toast.LENGTH_SHORT).show();
            } else if (customTriggerWords.contains(customTrigger)) {
                Toast.makeText(this, "⚠️ Trigger already exists!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Please enter a valid trigger", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void handleCustomTriggerRecording(ArrayList<String> matches) {
        // This would be used if recording custom triggers, for now just show message
        Toast.makeText(this, "Custom trigger recording handled", Toast.LENGTH_SHORT).show();
    }

    private void requestAllPermissions() {
        // Handle MANAGE_EXTERNAL_STORAGE for Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "Please grant 'All files access' for evidence storage", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        }

        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_AUDIO
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(this, "✅ All permissions already granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        String cleanNumber = phoneNumber.trim().replaceAll("[^0-9+]", "");
        if (cleanNumber.equals("911") || cleanNumber.equals("999") || cleanNumber.equals("112") || cleanNumber.equals("000")) {
            return true;
        }
        if (cleanNumber.startsWith("+") && cleanNumber.length() >= 11) {
            return cleanNumber.substring(1).matches("\\d+");
        }
        if (cleanNumber.length() >= 10 && cleanNumber.matches("\\d+")) {
            return true;
        }
        return false;
    }

    private void startLocationTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "⚠️ Location permission required", Toast.LENGTH_LONG).show();
            requestAllPermissions();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // More frequent updates during emergency
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    // Only log location, don't send SMS (SMS is handled by the 2-minute timer)
                    android.util.Log.d("Location", "Location updated: " + location.getLatitude() + ", " + location.getLongitude());

                    // Store location for WhatsApp messages, but don't send SMS here (By Windsurf)
                    // The SMS timer in sendUniversalEmergencyAlert() handles all SMS sending
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            Toast.makeText(this, "❌ Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestPermissions() {
        // Check if we already have all permissions
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_AUDIO
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        // Check for MANAGE_EXTERNAL_STORAGE on Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                allGranted = false;
            }
        }

        if (!allGranted) {
            showPermissionExplanationDialog();
        }
    }

    private void showPermissionExplanationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🛡️ Permissions Required for Safety");

        TextView messageView = new TextView(this);
        messageView.setText("This app needs several permissions to protect you:\n\n" +
                "📱 SMS: Send emergency alerts to contacts\n" +
                "📍 Location: Share your location during emergencies\n" +
                "🎤 Microphone: Voice triggers and evidence recording\n" +
                "📞 Phone: Quick emergency calls\n" +
                "🔔 Notifications: Alert status updates\n" +
                "💾 Storage: Save evidence files\n\n" +
                "These permissions are essential for your safety!");
        messageView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        messageView.setTextSize(16);
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(5, 1.2f);
        builder.setView(messageView);

        builder.setPositiveButton("✅ Grant Permissions", (dialog, which) -> {
            requestAllPermissions();
        });
        builder.setNegativeButton("❌ Skip", (dialog, which) -> {
            Toast.makeText(this, "⚠️ App functionality will be limited without permissions", Toast.LENGTH_LONG).show();
        });
        builder.setCancelable(true); // Allow dismissing

        AlertDialog dialog = builder.create();
        dialog.show();

        // Style buttons after showing
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, android.R.color.black));
    }

    private void sendWhatsAppMessage(String phoneNumber, String message) {
        try {
            // Get current location for WhatsApp message
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        String whatsappMessage = message;
                        if (location != null) {
                            String googleMapsUrl = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                            whatsappMessage += "\n\n📍 EMERGENCY LOCATION:\n" +
                                    "Lat: " + String.format("%.6f", location.getLatitude()) + "\n" +
                                    "Lng: " + String.format("%.6f", location.getLongitude()) + "\n" +
                                    "🗺️ Open in Maps: " + googleMapsUrl + "\n" +
                                    "📏 Accuracy: " + Math.round(location.getAccuracy()) + "m\n" +
                                    "⏰ " + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()) +
                                    "\n\n🎙️ EVIDENCE RECORDINGS:\n" +
                                    "📱 Audio evidence is being recorded automatically\n" +
                                    "💾 Files saved locally on device storage\n" +
                                    "📂 Location: /Android/data/com.example.womensafetyapp2/files/recordings/\n" +
                                    "🔍 To access: Settings > Apps > Women Safety App > Storage > Files\n" +
                                    "☁️ Cloud backup: Check app's Cloud Storage settings\n" +
                                    "\n⚠️ PLEASE RESPOND IMMEDIATELY - THIS IS A REAL EMERGENCY! ⚠️";
                        } else {
                            whatsappMessage += "\n\n⚠️ Location unavailable - please check GPS";
                        }
                        
                        sendWhatsAppWithMessage(phoneNumber, whatsappMessage);
                    })
                    .addOnFailureListener(e -> {
                        // Send without location if location fails
                        String fallbackMessage = message + "\n\n⚠️ Location unavailable - GPS error";
                        sendWhatsAppWithMessage(phoneNumber, fallbackMessage);
                    });
            } else {
                // Send without location if no permission
                String noLocationMessage = message + "\n\n⚠️ Location permission not granted";
                sendWhatsAppWithMessage(phoneNumber, noLocationMessage);
            }
        } catch (Exception e) {
            android.util.Log.e("WhatsApp", "WhatsApp integration error", e);
            // Fallback to basic message
            sendWhatsAppWithMessage(phoneNumber, message);
        }
    }

    private void sendWhatsAppWithMessage(String phoneNumber, String message) {
        try {
            // Clean phone number (remove all non-digits)
            String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");
            
            // Create WhatsApp intent with pre-filled message
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + cleanNumber + "&text=" + Uri.encode(message);
            whatsappIntent.setData(Uri.parse(url));
            
            // Try WhatsApp first
            whatsappIntent.setPackage("com.whatsapp");
            
            try {
                // Add flags to make WhatsApp appear on top
                whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // Add evidence location info to the existing message
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            String locationText = "Location not available";
                            if (location != null) {
                                locationText = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                            }

                            // Get evidence folder path
                            File evidenceDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "");
                            String evidencePath = evidenceDir.getAbsolutePath();

                            // Check if user is signed in to Google
                            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                            String driveInfo = "";
                            if (account != null) {
                                driveInfo = "\n☁️ Google Drive: https://drive.google.com (signed in as " + account.getEmail() + ")\n" +
                                        "📁 Look for 'Women Safety App' folder or search for .3gp files\n";
                            } else {
                                driveInfo = "\n☁️ Google Drive: Not signed in - evidence stored locally only\n";
                            }

                            // Enhanced message with all evidence access info
                            String enhancedMessage = message + "\n\n" +
                                    "📍 Location: " + locationText + "\n\n" +
                                    "🎵 EVIDENCE RECORDINGS:\n" +
                                    "📱 Local Storage: " + evidencePath + "\n" +
                                    "🔐 Access via USB: Android/data/com.example.womensafetyapp2/files/Music/\n" +
                                    driveInfo + "\n" +
                                    "💡 Files are in .3gp format (audio recordings)\n" +
                                    "⏰ Timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

                            // Use enhancedMessage instead of message
                            whatsappIntent.putExtra(Intent.EXTRA_TEXT, enhancedMessage);
                            startActivity(whatsappIntent);

                            // Show instruction to user
                            Toast.makeText(this, "📱 WhatsApp opened - send the message to: " + phoneNumber, Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            // Fallback without location
                            File evidenceDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "");
                            String evidencePath = evidenceDir.getAbsolutePath();

                            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
                            String driveInfo = "";
                            if (account != null) {
                                driveInfo = "\n☁️ Google Drive: https://drive.google.com (signed in as " + account.getEmail() + ")\n" +
                                        "📁 Look for 'Women Safety App' folder or search for .3gp files\n";
                            } else {
                                driveInfo = "\n☁️ Google Drive: Not signed in - evidence stored locally only\n";
                            }

                            String enhancedMessage = message + "\n\n" +
                                    "📍 Location: Not available\n\n" +
                                    "🎵 EVIDENCE RECORDINGS:\n" +
                                    "📱 Local Storage: " + evidencePath + "\n" +
                                    "🔐 Access via USB: Android/data/com.example.womensafetyapp2/files/Music/\n" +
                                    driveInfo + "\n" +
                                    "💡 Files are in .3gp format (audio recordings)\n" +
                                    "⏰ Timestamp: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

                            whatsappIntent.putExtra(Intent.EXTRA_TEXT, enhancedMessage);
                            startActivity(whatsappIntent);

                            Toast.makeText(this, "📱 WhatsApp opened - send the message to: " + phoneNumber, Toast.LENGTH_LONG).show();
                        });
                
                android.util.Log.d("WhatsApp", "WhatsApp message prepared for: " + phoneNumber);
            } catch (Exception e) {
                // If WhatsApp not installed, try WhatsApp Business
                whatsappIntent.setPackage("com.whatsapp.w4b");
                try {
                    whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(whatsappIntent);
                    Toast.makeText(this, "📱 WhatsApp Business opened - PRESS SEND to deliver emergency message!", Toast.LENGTH_LONG).show();
                    android.util.Log.d("WhatsApp", "WhatsApp Business message prepared for: " + phoneNumber);
                } catch (Exception e2) {
                    // If neither WhatsApp app is available, open in browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(browserIntent);
                    Toast.makeText(this, "🌐 WhatsApp Web opened - PRESS SEND to deliver emergency message!", Toast.LENGTH_LONG).show();
                    android.util.Log.d("WhatsApp", "WhatsApp web opened for: " + phoneNumber);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("WhatsApp", "Failed to open WhatsApp", e);
            Toast.makeText(this, "❌ Failed to open WhatsApp for " + phoneNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void startWhatsAppTimer() {
        if (isWhatsappTimerActive) return;

        isWhatsappTimerActive = true;
        whatsappHandler = new Handler();
        whatsappRunnable = new Runnable() {
            @Override
            public void run() {
                sendWhatsAppMessageToContacts();
                whatsappHandler.postDelayed(this, 120000); // 2 minutes
            }
        };
        whatsappHandler.post(whatsappRunnable);
    }

    private void sendWhatsAppMessageToContacts() {
        whatsappMessageCount++;
        currentEmergencyMessage = "🚨 EMERGENCY ALERT UPDATE 🚨\nI am still in danger and need help!\nThis is an automated emergency alert from my safety app.\nPlease contact authorities and check on me immediately.\nTime: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());

        // Reset contact index and start sending to first contact
        currentContactIndex = 0;
        sendWhatsAppToNextContact();
    }

    private void sendWhatsAppToNextContact() {
        if (currentContactIndex < emergencyContacts.size()) {
            String currentContact = emergencyContacts.get(currentContactIndex);
            
            // Debug logging
            android.util.Log.d("WhatsApp", "Sending to contact " + (currentContactIndex + 1) + "/" + emergencyContacts.size() + ": " + currentContact);
            
            // Send WhatsApp to current contact
            sendWhatsAppMessage(currentContact, currentEmergencyMessage);
            
            // Show progress via swipe-dismissable notification
            isWhatsAppFlowActive = true;
            showWhatsAppProgressNotification("WhatsApp " + (currentContactIndex + 1) + "/" + emergencyContacts.size() + " - Contact: " + currentContact + "\nTap SEND in WhatsApp");
            
            // Move to next contact after delay
            currentContactIndex++;
            
            // Schedule next contact after 15 seconds (gives user more time to send current message)
            if (currentContactIndex < emergencyContacts.size()) {
                Handler delayHandler = new Handler();
                delayHandler.postDelayed(() -> {
                    android.util.Log.d("WhatsApp", "Moving to next contact after delay");
                    sendWhatsAppToNextContact();
                }, 15000); // 15 second delay - increased for better user experience
            } else {
                // All contacts processed
                cancelWhatsAppNotification();
                isWhatsAppFlowActive = false;
                Toast.makeText(this, "✅ WhatsApp flow completed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopEmergencyNow() {
        try {
            android.util.Log.d("Emergency", "Stopping emergency mode...");
            
            // Stop WhatsApp timer
            if (whatsappHandler != null && whatsappRunnable != null) {
                whatsappHandler.removeCallbacks(whatsappRunnable);
                android.util.Log.d("Emergency", "WhatsApp timer stopped");
            }
            isWhatsappTimerActive = false;
            isWhatsAppFlowActive = false;
            cancelWhatsAppNotification();

            // Stop location tracking
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                android.util.Log.d("Emergency", "Location tracking stopped");
            }

            // Stop SMS timer
            if (smsHandler != null && smsRunnable != null) {
                smsHandler.removeCallbacks(smsRunnable);
                isSmsTimerActive = false;
            }

            // Stop evidence recording
            if (audioRecorder != null) {
                try {
                    stopEvidenceRecording();
                    android.util.Log.d("Emergency", "Evidence recording stopped");
                } catch (Exception e) {
                    android.util.Log.e("Emergency", "Error stopping evidence recording", e);
                }
            }

            // Reset emergency state
            isEmergencyActive = false;
            currentContactIndex = 0;
            currentEmergencyMessage = "";

            // Update UI immediately
            if (alertButton != null) {
                alertButton.setText("🚨 EMERGENCY ALERT");
                android.util.Log.d("Emergency", "Alert button text reset");
            }
            if (statusText != null) {
                statusText.setText("🌸 Emergency stopped. Stay safe.");
                android.util.Log.d("Emergency", "Status text updated");
            }
            
            // Show confirmation
            Toast.makeText(this, "✅ Emergency mode stopped successfully", Toast.LENGTH_SHORT).show();
            android.util.Log.d("Emergency", "Emergency mode stopped successfully");
            
        } catch (Exception e) {
            android.util.Log.e("Emergency", "Failed to stop emergency cleanly", e);
            Toast.makeText(this, "⚠️ Error stopping emergency: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void ensureWhatsAppNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    WHATSAPP_CHANNEL_ID,
                    "WhatsApp Progress",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows progress and instructions for WhatsApp sending");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void showWhatsAppProgressNotification(String text) {
        ensureWhatsAppNotificationChannel();
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, WHATSAPP_CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle("Women Safety App")
                .setContentText("WhatsApp message flow")
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true) // allow swipe to dismiss
                .setOngoing(false);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(WHATSAPP_NOTIFICATION_ID, builder.build());
    }

    private void cancelWhatsAppNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(WHATSAPP_NOTIFICATION_ID);
    }

    private AlertDialog createStyledDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setTextColor(Color.WHITE);
        messageView.setTextSize(16);
        messageView.setPadding(50, 30, 50, 30);
        messageView.setLineSpacing(5, 1.2f);
        messageView.setBackgroundColor(Color.parseColor("#6A1B9A"));
        builder.setView(messageView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
        
        return dialog;
    }
}