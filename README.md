# Bubble Chat Head

## Overview
Bubble Chat Head is an Android app that allows you to mark any area of your phone's screen (even when the app is in the background), extract the text from that area using OCR, and view the extracted text inside the app.

## Features
- Floating chat head overlay that stays on top of other apps
- Mark any area of your screen by drawing a rectangle
- Extract text from the selected area using Google ML Kit OCR
- View the extracted text inside the app

## How to Use

### 1. Build and Install
- Open the project in Android Studio.
- Connect your Android device or start an emulator (API 33+ required).
- Click **Run** to build and install the app.

### 2. Enable Overlay Permission
- On first launch, toggle the floating head switch in the app.
- Grant the "Display over other apps" permission when prompted.

### 3. Enable Floating Chat Head
- Toggle the switch to enable the floating chat head.
- The chat head will appear and float over other apps.

### 4. Mark an Area of the Screen
- **Long-press** the floating head switch in the app to start screen selection.
- Grant screen capture permission when prompted.
- A transparent overlay will appear. Drag to draw a rectangle over the area you want to capture.
- Release your finger to finish selection.

### 5. Extract and View Text
- The app will capture the selected area, extract text using OCR, and display the result in a Toast message (or in the chat system if integrated).
- You can repeat the process as needed.

## Notes
- The app must have overlay and screen capture permissions to work correctly.
- The floating chat head allows you to trigger screen selection from anywhere, even when the app is in the background.
- The extracted text is currently shown in a Toast. You can extend the app to display it in a chat interface.

## Troubleshooting
- If the chat head does not appear, ensure overlay permission is granted.
- If screen selection does not work, ensure screen capture permission is granted.
- Some apps may block overlays or screen capture for security reasons.

## License
MIT

