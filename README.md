# 🏠 Melhome Bridge: Better MELCloud Home Integration for Google Home

This project provides a custom Android app and a self-hosted Node.js bridge to connect your Mitsubishi Air Conditioners (using MELCloud Home) to Google Home, overcoming the limitations of the official integration.

If you are tired of only being able to turn your AC on/off and change the temperature, this project is for you.

## ✨ Features
*   **Full Google Home Control:** Finally use Google Assistant to control fan speeds ("Ok Google, set AC fan speed to 2").
*   **Custom Android App:** A fast, lightweight alternative to the official app, offering native control over Vanes (Vertical & Horizontal direction).
*   **Privacy-Focused & Self-Hosted:** You run your own bridge on a free Render account. Your MELCloud credentials never leave your control.
*   **100% Free & Open-Source:** No subscriptions, no proprietary hubs.

---

## 🛠️ How to set it up (Takes about 10-15 minutes)

You don't need to be a developer to set this up, just follow the steps below!

### Step 1: Deploy your free Bridge on Render
Since this is a self-hosted solution, you need a tiny server to act as a bridge between your Android app and Google Home. We use Render because it has a generous free tier.

1.  Create a free account on [Render.com](https://render.com/).
2.  Fork this repository to your own GitHub account: **[tountoun66/melhome-bridge](https://github.com/tountoun66/melhome-bridge)**
3.  On Render, click **New +** and select **Web Service**.
4.  Connect your GitHub account and select your newly forked `melhome-bridge` repository.
5.  Use the following settings:
    *   **Build Command:** `npm install`
    *   **Start Command:** `node index.js` (or `npm start`)
    *   **Instance Type:** Free
6.  Click **Create Web Service**. Wait a few minutes for the status to turn green (*Live*).
7.  **Copy your Render URL** (it will look like `https://your-app-name.onrender.com`). Keep this handy!

### Step 2: Create a Google Smart Home Test Project
Because this is a DIY integration, we will use Google's "Test Mode" to link your personal bridge to your personal Google Home account.

1.  Go to the [Google Action Console](https://console.actions.google.com/) and click **New Project**. Name it something like "Melhome".
2.  Select **Smart Home** as the project type.
3.  Go to the **Develop** tab, then **Actions**:
    *   Set the **Fulfillment URL** to your Render URL followed by `/fulfillment` (e.g., `https://your-app-name.onrender.com/fulfillment`).
4.  Go to **Account linking** under the Develop tab:
    *   **Linking Type:** OAuth and Authorization Code
    *   **Client ID:** `1234` (Just put dummy data, our custom bridge doesn't check this)
    *   **Client Secret:** `1234` (Dummy data again)
    *   **Authorization URL:** `https://your-app-name.onrender.com/oauth/auth`
    *   **Token URL:** `https://your-app-name.onrender.com/oauth/token`
5.  Click **Save**.
6.  In the top right corner of the console, click the **Test** button to enable testing on your Google account.

### Step 3: Configure the Android App

1.  Download and install the latest APK of the **Melhome** Android app from the [Releases section](#) (Add your APK link here!).
2.  Open the app and tap the **Settings (⚙️)** icon.
3.  In the **Render URL** field, paste your Render web service URL (e.g., `https://your-app-name.onrender.com`).
4.  Tap **Save**.
5.  Tap **Link to Google Home** (Associer à Google Home). A 4-digit pairing code will appear on your screen. Leave it open.

### Step 4: Link in the Google Home App

1.  Open the official **Google Home** app on your phone.
2.  Tap **+ Add**, then **Set up device**, then **Works with Google**.
3.  Search for your project name (it will usually have `[test]` in front of it, like `[test] Melhome`).
4.  A login screen will pop up. Enter the **4-digit pairing code** displayed on your Melhome Android app.
5.  Success! Your Mitsubishi AC units will now appear in your Google Home app, ready to be controlled by voice or touch.

---

## 💻 Server Files (For reference)
This repository contains the necessary files to deploy the Node.js server. 
*   `index.js`: The main bridge logic handling OAuth and Google Smart Home intents.
*   `package.json`: Dependencies required to run the server on Render.

*You do not need to edit these files to make it work for you!*