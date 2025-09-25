# Women Safety App - Emergency Response System
#### Video Demo: <https://www.youtube.com/watch?v=dQw4w9WgXcQ>
#### Description:

The Women Safety App is a comprehensive Android emergency response application designed to provide immediate assistance during dangerous situations. Built using Java and Android Studio, this app combines multiple safety features into a single, reliable platform that works even in offline conditions. The app focuses on three core functionalities: emergency alerts, evidence recording, and real-time location sharing.

## Project Overview

The Women Safety App is a single-activity application with a clean, intuitive interface. The app uses Android's built-in services for SMS, location, and audio recording, ensuring compatibility across different Android versions. Key components include permission management, background services, error handling, and UI design.

## Key Features and Functionality

### Emergency Alert System

The centerpiece of the application is its sophisticated emergency alert system. When activated, the app simultaneously sends SMS messages and WhatsApp alerts to pre-configured emergency contacts every 2 minutes. This dual-channel approach ensures message delivery even if one communication method fails. The SMS system includes intelligent SIM card detection for dual-SIM devices, allowing users to select which SIM card to use for emergency communications.

The emergency alerts include comprehensive information: the user's exact GPS coordinates with Google Maps links, timestamp, location accuracy, and detailed instructions for accessing recorded evidence. The app automatically generates messages that provide emergency contacts with everything they need to respond effectively.

### Voice-Activated Triggers

One of the most innovative features is the voice recognition system that can detect emergency trigger words. The app continuously monitors for phrases like "help me," "emergency," or "call police" in the background. Users can also add custom trigger words through two methods: typing them manually or recording them using speech recognition. This hands-free activation is crucial in situations where the user cannot physically interact with their phone.

### Evidence Recording and Cloud Backup

The app automatically begins recording audio evidence when emergency mode is activated. These recordings are saved locally in .3gp format and can be automatically uploaded to Google Drive for secure cloud storage. The evidence system is designed to preserve crucial information even if the phone is damaged or destroyed during an incident.

The Google Drive integration uses OAuth2 authentication and the Drive API to securely upload evidence files with descriptive names and timestamps. Users can connect their Google account through a streamlined sign-in process, and the app provides clear feedback about upload status and file locations.

### Location Tracking and Sharing

Real-time GPS tracking begins immediately when emergency mode is activated. The app uses Google's Fused Location Provider for accurate positioning and shares coordinates through multiple channels. Location information is embedded in both SMS and WhatsApp messages, providing emergency contacts with precise geographic data and direct links to Google Maps.

## Technical Implementation

### File Structure and Architecture

The main application logic resides in `MainActivity.java`, which contains approximately 1,900 lines of carefully structured code. This single-activity architecture was chosen for simplicity and reliability during emergency situations.

Key components include:
- **Permission Management**: Comprehensive handling of Android runtime permissions for SMS, location, microphone, and storage access
- **Background Services**: Implementation of foreground services for continuous monitoring and recording
- **Error Handling**: Robust exception handling to ensure the app continues functioning even when individual features encounter problems
- **UI Design**: Clean, intuitive interface with large buttons and clear status indicators

### Google Drive Integration

The cloud storage functionality uses Google's Drive API v3 with proper credential management. The implementation includes:
- OAuth2 authentication flow using GoogleSignInClient
- Automatic file upload with descriptive naming conventions
- Error handling for network issues and authentication failures
- Fallback to local storage when cloud services are unavailable

### SMS and Communication Systems

The SMS system was designed to handle various edge cases:
- Dual SIM support with automatic detection and user selection
- Message splitting for long SMS content
- Retry mechanisms for failed deliveries
- Integration with Android's SmsManager for reliable message sending

## Design Decisions and Challenges

### Single vs. Multiple Activities

I chose a single-activity architecture despite the app's complexity. This decision was made primarily for reliability - in emergency situations, the app needs to respond instantly without the overhead of activity transitions. All functionality is contained within MainActivity.java, making the code easier to debug and maintain.

### Timer Implementation

One significant challenge was implementing proper timing for periodic SMS alerts. The initial implementation caused rapid-fire message sending due to multiple timer instances running simultaneously. I solved this by implementing a singleton timer pattern with proper cleanup mechanisms, ensuring SMS messages are sent exactly every 2 minutes as intended.

### Offline Functionality

The app was designed to work completely offline, as emergency situations often involve poor network connectivity. All core features (SMS, location tracking, evidence recording) function without internet access. Cloud backup is treated as an enhancement rather than a requirement.

### Permission Handling

Android's runtime permission system required careful implementation to ensure the app requests only necessary permissions and handles denials gracefully. The app includes educational dialogs explaining why each permission is needed for user safety.

## Testing and Reliability

The app has been extensively tested across different scenarios:
- Various Android versions and device types
- Network connectivity issues and offline operation
- Different SIM card configurations
- Google account authentication edge cases
- Audio recording in different environments

## Future Enhancements

While the current version provides comprehensive emergency functionality, potential improvements could include:
- Integration with additional cloud storage providers
- Support for video evidence recording
- Integration with professional emergency services
- Multi-language support for international users

## Conclusion

The Women Safety App represents a complete emergency response solution that prioritizes reliability and ease of use. By combining multiple communication channels, evidence recording, and cloud backup into a single application, it provides users with a comprehensive safety tool. The app demonstrates practical application of Android development concepts including services, permissions, API integration, and user interface design.

The project showcases not only technical programming skills but also thoughtful consideration of real-world user needs and emergency scenarios. Every feature was implemented with the understanding that in a genuine emergency, the app must work flawlessly when it matters most.
