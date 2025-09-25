package com.example.womensafetyapp2;

import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private File outputDir;
    private File currentFile;
    private boolean isRecording = false;

    public AudioRecorder(File outputDirectory) {
        this.outputDir = outputDirectory;
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    public void startRecording() throws IOException {
        if (isRecording) {
            throw new IllegalStateException("Recording is already in progress");
        }

        if (outputDir == null) {
            throw new IOException("Output directory is null");
        }

        // Create unique filename with timestamp
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "evidence_recording_" + timestamp + ".3gp";
        currentFile = new File(outputDir, fileName);

        try {
            mediaRecorder = new MediaRecorder();

            // Configure MediaRecorder
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(currentFile.getAbsolutePath());

            // Set maximum duration (10 minutes) and file size (50MB)
            mediaRecorder.setMaxDuration(10 * 60 * 1000); // 10 minutes
            mediaRecorder.setMaxFileSize(50 * 1024 * 1024); // 50MB

            mediaRecorder.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.i(TAG, "Maximum recording duration reached");
                } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    Log.i(TAG, "Maximum file size reached");
                }
            });

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            Log.i(TAG, "Recording started: " + currentFile.getAbsolutePath());

        } catch (Exception e) {
            cleanup();
            throw new IOException("Failed to start recording: " + e.getMessage(), e);
        }
    }

    public File stopRecording() throws IOException {
        if (!isRecording) {
            throw new IllegalStateException("No recording in progress");
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            isRecording = false;

            if (currentFile != null && currentFile.exists() && currentFile.length() > 0) {
                Log.i(TAG, "Recording stopped successfully: " + currentFile.getAbsolutePath());
                Log.i(TAG, "File size: " + currentFile.length() + " bytes");
                return currentFile;
            } else {
                throw new IOException("Recording file is empty or doesn't exist");
            }

        } catch (Exception e) {
            cleanup();
            throw new IOException("Failed to stop recording: " + e.getMessage(), e);
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public long getRecordingDuration() {
        if (currentFile != null && currentFile.exists()) {
            // This is an approximation - for exact duration, you'd need MediaMetadataRetriever
            return System.currentTimeMillis() - currentFile.lastModified();
        }
        return 0;
    }

    private void cleanup() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder", e);
            }
            mediaRecorder = null;
        }
        isRecording = false;

        // Clean up empty or corrupted files
        if (currentFile != null && currentFile.exists() && currentFile.length() == 0) {
            currentFile.delete();
            currentFile = null;
        }
    }

    public void release() {
        if (isRecording) {
            try {
                stopRecording();
            } catch (IOException e) {
                Log.e(TAG, "Error stopping recording during release", e);
            }
        }
        cleanup();
    }

    // Get all recorded files in the output directory
    public File[] getAllRecordings() {
        if (outputDir != null && outputDir.exists()) {
            return outputDir.listFiles((dir, name) -> name.startsWith("evidence_recording_") && name.endsWith(".3gp"));
        }
        return new File[0];
    }

    // Delete old recordings to save space (keep only last 10)
    public void cleanupOldRecordings() {
        File[] recordings = getAllRecordings();
        if (recordings != null && recordings.length > 10) {
            // Sort by last modified date
            java.util.Arrays.sort(recordings, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

            // Delete oldest files, keep newest 10
            for (int i = 0; i < recordings.length - 10; i++) {
                if (recordings[i].delete()) {
                    Log.i(TAG, "Deleted old recording: " + recordings[i].getName());
                }
            }
        }
    }
}