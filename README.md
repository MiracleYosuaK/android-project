# Wazuh Mobile - Security Event Monitoring

A modern Android application for monitoring Wazuh security events with real-time dashboard, agent monitoring, and AI-powered security insights.

## Features

### 🚀 Splash Screen
- Modern gradient background with Wazuh branding
- Animated loading indicators
- Smooth transitions

### 🔐 Login Screen
- Dynamic server configuration
- Secure authentication with Wazuh server
- Clean, user-friendly interface
- Credential persistence

### 📊 Dashboard
- **API Connectivity Status**: Real-time monitoring of Wazuh API and OpenAI API connections
- **Event Statistics**: Total events in the last 3 hours with trend indicators
- **High-Critical Alerts**: Count of severity level 10-15 alerts
- **Agent Monitoring**: Live status of all connected agents
- **AI Summary**: OpenAI-generated security overview based on recent events
- **Security Events**: High-priority alerts with detailed information
- **Bottom Navigation**: Easy access to all app features

## Technical Specifications

- **Platform**: Android (API 24+)
- **Language**: Java
- **Architecture**: MVVM with Repository pattern
- **UI Framework**: Material Design Components
- **Network**: Retrofit + OkHttp for API communication
- **Image Loading**: Glide
- **Local Storage**: SharedPreferences

## Project Structure

```
android-project/
├── app/
│   ├── src/main/
│   │   ├── java/com/wazuh/mobile/
│   │   │   ├── SplashActivity.java
│   │   │   ├── LoginActivity.java
│   │   │   ├── MainActivity.java
│   │   │   ├── Agent.java
│   │   │   ├── Alert.java
│   │   │   ├── AgentAdapter.java
│   │   │   └── AlertAdapter.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   ├── values/
│   │   │   └── menu/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Setup Instructions

1. **Import into Android Studio**:
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the `android-project` folder
   - Click "OK" to import

2. **Sync Project**:
   - Android Studio will automatically sync Gradle files
   - Wait for the sync to complete

3. **Build and Run**:
   - Connect an Android device or start an emulator
   - Click the "Run" button or press Ctrl+R (Cmd+R on Mac)

## Configuration

### Wazuh Server Setup
1. Enter your Wazuh server URL in the login screen
2. Provide valid credentials for your Wazuh installation
3. The app will validate the connection and store settings

### API Integration
- **Wazuh API**: Configure endpoints for agent monitoring and event retrieval
- **OpenAI API**: Set up API key for AI-powered security summaries

## Key Components

### Activities
- **SplashActivity**: App initialization with animated loading
- **LoginActivity**: Authentication and server configuration
- **MainActivity**: Main dashboard with all monitoring features

### Models
- **Agent**: Represents monitored systems with status tracking
- **Alert**: Security events with severity levels and timestamps

### Adapters
- **AgentAdapter**: RecyclerView adapter for agent list display
- **AlertAdapter**: RecyclerView adapter for security alerts

## Design Features

- **Professional Color Scheme**: Wazuh brand colors with high contrast
- **Card-based Layout**: Organized information display
- **Responsive Design**: Optimized for various screen sizes
- **Material Design**: Following Android design guidelines
- **Smooth Animations**: Enhanced user experience

## Security Features

- Secure credential storage
- HTTPS communication with Wazuh servers
- Input validation and sanitization
- Session management

## Future Enhancements

- Push notifications for critical alerts
- Offline mode with data caching
- Advanced filtering and search
- Custom dashboard widgets
- Multi-server support
- Biometric authentication

## Dependencies

- AndroidX AppCompat
- Material Design Components
- RecyclerView
- CardView
- SwipeRefreshLayout
- Retrofit for networking
- Gson for JSON parsing
- Glide for image loading

## License

This project is developed for Wazuh security monitoring purposes.

## Support

For technical support or feature requests, please contact the development team.
