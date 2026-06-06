# Tradigo Ultra V4.0 - Setup Guide

## Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Node.js 18+
- Firebase CLI: `npm install -g firebase-tools`

## Step 1: Firebase Setup
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create new project: `TradigoUltraV4`
3. Enable Authentication (Email/Password, Google)
4. Create Firestore Database (test mode)
5. Download `google-services.json` and place in `android-app/app/`

## Step 2: API Keys
Create `android-app/local.properties`:
```properties
sdk.dir=/Users/YOUR_NAME/Library/Android/sdk
razorpay.key.id=rzp_test_YOUR_KEY
