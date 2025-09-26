# Women Safety App - Emergency Response System
#### Video Demo: https://www.youtube.com/watch?v=eloJAnjdIYA
#### Description:

## Project Overview

The Women Safety App is a comprehensive Android emergency response application designed to provide immediate assistance during dangerous situations. Built using Java and Android Studio, this app combines multiple safety features into a single, reliable platform that works even in offline conditions. The application focuses on three core functionalities: emergency alerts, evidence recording, and real-time location sharing, all accessible through an intuitive user interface designed for quick access during emergencies.

## Core Features and Implementation

### Emergency Alert System

The centerpiece of the application is its sophisticated emergency alert system accessible through the prominent red "🚨 EMERGENCY ALERT 🚨" button. When activated, the app simultaneously sends SMS messages and WhatsApp alerts to pre-configured emergency contacts every 2 minutes. This dual-channel approach ensures message delivery even if one communication method fails. The SMS system includes intelligent SIM card detection for dual-SIM devices, allowing users to select which SIM card to use for emergency communications.

The emergency alerts include comprehensive information: the user's exact GPS coordinates with Google Maps links, timestamp, location accuracy, and detailed instructions for accessing recorded evidence. The app automatically generates messages that provide emergency contacts with everything they need to respond effectively. The system uses Android's SmsManager class with proper permission handling to prevent the app from becoming unresponsive due to permission requests.

### Voice Recognition and Monitoring

One of the most innovative features is the voice recognition system accessible through the "🎤 Start Voice Monitor" button. The app continuously monitors for emergency trigger words including "help me," "emergency," "call police," and "I'm in danger" in the background. The voice monitoring system uses Android's SpeechRecognizer API with proper RecognitionListener implementation to ensure continuous operation without draining battery life excessively.

Users can also add custom trigger words through the "🎯 Set Custom Trigger Word" button, which provides three options: typing custom phrases manually, recording them using speech recognition, or deleting existing custom triggers. This hands-free activation is crucial in situations where the user cannot physically interact with their phone. The custom trigger system stores user preferences using SharedPreferences for persistence across app sessions.

### Evidence Recording and Cloud Storage

The app automatically begins recording audio evidence when emergency mode is activated through the "🎵 Start Evidence Recording" button. These recordings are saved locally in .3gp format in the device's external storage directory and can be automatically uploaded to Google Drive for secure cloud storage. The evidence system is designed to preserve crucial information even if the phone is damaged or destroyed during an incident.

The Google Drive integration uses OAuth2 authentication and the Drive API v3 to securely upload evidence files with descriptive names and timestamps. Users can connect their Google account through the "☁️ Cloud Storage Settings" button, which provides a streamlined sign-in process. The app provides clear feedback about upload status and file locations, ensuring users understand where their evidence is stored.

### Location Tracking and Sharing

Real-time GPS tracking begins immediately when emergency mode is activated. The app uses Google's Fused Location Provider API for accurate positioning and shares coordinates through multiple channels. Location information is embedded in both SMS and WhatsApp messages, providing emergency contacts with precise geographic data and direct links to Google Maps. The location system includes accuracy measurements and timestamps to help emergency responders understand the reliability and recency of the location data.

### Emergency Contacts Management

The "📱 Manage Emergency Contacts" button provides access to a comprehensive contact management system. Users can add, edit, and remove emergency contacts with proper validation to ensure phone numbers are in the correct format. The system supports international phone numbers and provides testing functionality to verify that contacts can receive SMS messages. Emergency contacts are stored securely using Android's SharedPreferences system.

## Technical Architecture

### File Structure and Components

The main application logic resides in `MainActivity.java`, which contains approximately 1,900 lines of carefully structured code. This single-activity architecture was chosen for simplicity and reliability during emergency situations. The app uses Android's built-in services for SMS, location, and audio recording, ensuring compatibility across different Android versions from API 23 (Android 6.0) to API 34 (Android 14).

Key technical components include:
- **Permission Management**: Comprehensive handling of Android runtime permissions for SMS, location, microphone, and storage access
- **Background Services**: Implementation of foreground services for continuous monitoring and recording
- **Error Handling**: Robust exception handling to ensure the app continues functioning even when individual features encounter problems
- **UI Design**: Clean, intuitive interface with large buttons and clear status indicators optimized for emergency use

### User Interface Design

The app features a beautiful gradient background with a purple theme that's both aesthetically pleasing and functional. The layout uses a ScrollView with LinearLayout to ensure all features are accessible on different screen sizes. The main interface includes:

- **Status Display**: Real-time status updates showing current app state
- **Emergency Button**: Large, prominent red button for immediate emergency activation
- **Voice Controls**: Easy access to voice monitoring and custom trigger management
- **Settings Section**: Organized access to contacts, cloud storage, and mode switching
- **Instructions Card**: Built-in help system explaining all features

### Mode System

The app includes a unique dual-mode system accessible through the "🛡️ Switch to Guardian Mode" button. Personal Mode is designed for individual users who need emergency assistance, while Guardian Mode allows trusted individuals to monitor and respond to emergencies. The mode system includes visual indicators and different functionality sets appropriate for each use case.

## Design Decisions and Problem Solving

### Single Activity Architecture

I chose a single-activity architecture despite the app's complexity. This decision was made primarily for reliability - in emergency situations, the app needs to respond instantly without the overhead of activity transitions. All functionality is contained within MainActivity.java, making the code easier to debug and maintain while ensuring consistent behavior across different Android versions.

### Timer Implementation and SMS Management

One significant challenge was implementing proper timing for periodic SMS alerts. The initial implementation caused rapid-fire message sending due to multiple timer instances running simultaneously. I solved this by implementing a singleton timer pattern with proper cleanup mechanisms and comprehensive permission checking, ensuring SMS messages are sent exactly every 2 minutes as intended while preventing the app from becoming unresponsive due to permission dialogs.

### Offline Functionality

The app was designed to work completely offline, as emergency situations often involve poor network connectivity. All core features (SMS, location tracking, evidence recording) function without internet access. Cloud backup is treated as an enhancement rather than a requirement, ensuring the app remains functional even in areas with poor cellular coverage, but it requires internet.

## Testing and Reliability

The app has been extensively tested across different scenarios including various Android versions and device types, network connectivity issues and offline operation, different SIM card configurations, Google account authentication edge cases, and audio recording in different environments. Special attention was paid to ensuring the app works reliably during actual emergency situations when users may be under stress.

## Conclusion

The Women Safety App represents a complete emergency response solution that prioritizes reliability and ease of use. By combining multiple communication channels, evidence recording, and cloud backup into a single application, it provides users with a comprehensive safety tool. The app demonstrates practical application of Android development concepts including services, permissions, API integration, and user interface design.

The project showcases not only technical programming skills but also thoughtful consideration of real-world user needs and emergency scenarios. Every feature was implemented with the understanding that in a genuine emergency, the app must work flawlessly when it matters most. The comprehensive feature set, reliable architecture, and intuitive design make this app a valuable tool for personal safety and emergency response.

