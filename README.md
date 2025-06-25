# Zyptra 📱

A full-fledged Android application backup solution with cloud integration. This app allows users to extract and backup APKs from their installed applications and store them securely in Google Drive.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android-green)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue)
![License](https://img.shields.io/badge/license-MIT-orange)

## Features ✨

- 📱 Extract APKs from installed applications
- 🔒 Secure Google Drive integration for cloud backup
- 📋 List all installed applications with details
- 🔍 Search and filter applications
- 🎨 Material Design 3 with dynamic theming
- 🌙 Dark mode support
- 📊 Progress tracking for backup operations
- 🔄 Batch backup operations
- 🎯 Android 12+ support

## Screenshots 📸

[Add your screenshots here]

## Tech Stack 🛠️

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Clean Architecture
- **Dependency Injection**: Hilt
- **Cloud Storage**: Google Drive API
- **Authentication**: Google Sign-In
- **Background Processing**: Kotlin Coroutines
- **Local Storage**: Android Storage Access Framework

## Setup Instructions 🚀

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or newer
- Android SDK with minimum API level 24
- Google Cloud Console account

### Setting up Google Cloud Project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Google Drive API
   - Google Sign-In API
4. Create OAuth 2.0 credentials:
   - Go to "Credentials"
   - Click "Create Credentials" > "OAuth client ID"
   - Choose "Android" as application type
   - Add your package name and SHA-1 signing certificate fingerprint

### Local Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/YadavYashvant/ApkExtractor.git
   ```

2. Configure Google Services:
   - Copy `google-services.template.json` to `google-services.json`
   - Replace the placeholder values with your Google Cloud project credentials

3. Update OAuth Client ID:
   - Open `Config.kt`
   - Replace the `OAUTH_CLIENT_ID` value with your OAuth client ID

4. Generate SHA-1:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   Add this SHA-1 to your Google Cloud Console project.

5. Sync project with Gradle files

### Building and Running

1. Open the project in Android Studio
2. Wait for the Gradle sync to complete
3. Run the app on an emulator or physical device

## Architecture 🏗️

The app follows Clean Architecture principles with MVVM pattern:

```
app/
├── data/
│   ├── model/
│   ├── repository/
│   └── storage/
├── di/
├── service/
├── ui/
│   ├── screens/
│   ├── theme/
│   └── viewmodel/
└── util/
```

## Contributing 🤝

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Future Improvements 🚀

- [ ] Add support for app data backup
- [ ] Implement backup scheduling
- [ ] Add backup encryption
- [ ] Support for multiple cloud storage providers
- [ ] Implement batch restore functionality
- [ ] Add backup version control

## License 📄

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details