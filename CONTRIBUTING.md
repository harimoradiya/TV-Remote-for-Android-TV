# Contributing to TV Remote for Android TV

We welcome contributions to **TV Remote for Android TV**! As an open-source project, your help makes this app better for everyone. Follow the guidelines below to submit your improvements.

---

## How to Contribute

### 1. Fork and Clone
1. Fork the repository on GitHub: `https://github.com/harimoradiya/TV-Remote-for-Android-TV`
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TV-Remote-for-Android-TV.git
   cd TV-Remote-for-Android-TV
   ```

### 2. Set Up the Project
1. Open the project in Android Studio.
2. Build the project using the debug build variant to ensure dependencies load correctly.

### 3. Create a Feature Branch
Create a branch for your changes:
```bash
git checkout -b feature/your-feature-name
```

### 4. Code Style & Standards
- Follow standard Kotlin coding styles and conventions.
- Keep UI components built with Jetpack Compose simple and clean.
- Do not add any monetization elements (ads, billing libraries, paywalls, etc.). The project must remain 100% free.

### 5. Prevent Committing Secrets
- Do **not** commit local config files, keystores, API keys, or private properties.
- Ensure that `google-services.json`, `local.properties`, and keystores (`.jks`/`.keystore`) are ignored by Git.

### 6. Test Your Changes
- Build and run the app on an emulator or a physical device.
- Test Wi-Fi scanning, pairing, D-pad navigation, and keyboard sync flows to verify stability.
- Ensure that the app builds successfully in release mode:
  ```bash
  ./gradlew assembleDebug
  ```

### 7. Submit a Pull Request
1. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. Open a Pull Request (PR) against our `main` branch.
3. Describe the changes, the problem solved, and details of how you verified the changes.

Thank you for contributing!
