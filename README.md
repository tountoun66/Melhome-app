# 🏠 Melhome Bridge v1.1: Better MELCloud Home Integration for Google Home

This project provides a custom Android app and a self-hosted Node.js bridge to connect your Mitsubishi Air Conditioners (using MELCloud Home) to Google Home, overcoming the limitations of the official integration.

If you are tired of only being able to turn your AC on/off and change the temperature, this project is for you.

## ✨ Features

*   **Full Google Home Voice Control:** Go far beyond the basic official integration. You can now use Google Assistant to control:
    *   **Power:** Turn AC on or off.
    *   **Temperature:** Set or ask for the current target temperature.
    *   **Modes:** Switch seamlessly between *Auto*, *Heat*, *Cool*, *Dry*, and *Fan* modes.
    *   **Fan Speed:** Adjust the ventilation power (e.g., *"Ok Google, set AC fan speed to 2"* or *"Set AC to max speed"*).
*   **Custom Android App:** A fast, lightweight alternative to the official app, offering native manual control over your AC units, including **Vanes control** (Vertical & Horizontal airflow direction).
*   **[NEW in v1.1] 100% Autonomous Background Sync:** You no longer need to manually open the app to keep your session alive. Your phone does it silently in the background!
*   **Privacy-Focused & Self-Hosted:** You run your own bridge on a free Render account. Your MELCloud credentials never leave your device (stored in a hardware-encrypted vault) and are never stored on our servers.
*   **100% Free & Open-Source:** No subscriptions, no proprietary hubs required.

---

## 🛠️ How to Set It Up (Takes about 10-15 minutes)

You don't need to be a developer to set this up. Just follow the step-by-step guide below!

### Step 1: Deploy your free Bridge on Render
Since this is a self-hosted solution, you need a tiny server to act as a bridge between your Android app and Google Home. We use Render because it has a generous free tier.

1.  Create a free account on [Render.com](https://render.com/).
2.  On the Render dashboard, click **New +** and select **Web Service**.
3.  Under "Public Git repository", simply paste the link to this project:
    `https://github.com/tountoun66/melhome-bridge`
    *(No need to fork or download anything!)*
4.  Click **Continue** and use the following settings:
    *   **Build Command:** `npm install`
    *   **Start Command:** `npm start` *(Important: Do not use node index.js)*
    *   **Instance Type:** Free
5.  Click **Create Web Service**. Wait a few minutes for the status to turn green (*Live*).
6.  **Copy your Render URL** (it will look like `https://your-app-name.onrender.com`). Keep this handy!

### Step 2: Create a Google Smart Home Test Project
Because this is a DIY integration, we will use Google's "Test Mode" to link your personal bridge to your personal Google Home account.

1.  Go to the [Google Action Console](https://console.actions.google.com/) and click **New Project**. Name it something like "Melhome".
2.  Select **Cloud-to-cloud** as the project type. *(If prompted to select a device category, ensure you select **AC**, **Heating**, or **AC Unit** so Google knows how to control it).*
3.  Go to the **Develop** tab, then **Actions**:
    *   Set the **Fulfillment URL** to your Render URL followed by `/fulfillment` (e.g., `https://your-app-name.onrender.com/fulfillment`).
4.  Go to **Account linking** under the Develop tab:
    *   **Linking Type:** OAuth and Authorization Code
    *   **Client ID:** `1234` *(Just put dummy data, our custom bridge doesn't check this)*
    *   **Client Secret:** `1234` *(Dummy data again)*
    *   **Authorization URL:** `https://your-app-name.onrender.com/oauth/auth`
    *   **Token URL:** `https://your-app-name.onrender.com/oauth/token`
5.  Click **Save**.
6.  In the top right corner of the console, click the **Test** button to enable testing on your Google account.

### Step 3: Configure the Android App

1.  Download the **ZIP file** containing the Melhome Android app v1.1 directly from this GitHub repository. Extract the `.zip` file, and install the included `.apk` on your phone.
2.  Open the app and **log in to your MELCloud Home account** using your official credentials. *(Your login details are instantly hardware-encrypted via Android's EncryptedSharedPreferences and never leave your phone).*
3.  Once logged in, tap the **Settings (⚙️)** icon.
4.  In the **Render URL** field, paste your Render web service URL (e.g., `https://your-app-name.onrender.com`).
5.  Tap **Save**.
6.  Tap **Link to Google Home** (*Associer à Google Home*). A 4-digit pairing code will appear on your screen. Leave it open.

### Step 4: Link in the Google Home App

1.  Open the official **Google Home** app on your phone.
2.  Tap **+ Add**, then **Set up device**, then **Works with Google**.
3.  Search for your project name (it will usually have `[test]` in front of it, like `[test] Melhome`).
4.  A login screen will pop up. Enter the **4-digit pairing code** displayed on your Melhome Android app.
5.  **Success!** Your Mitsubishi AC units will now appear in your Google Home app, ready to be controlled by voice or touch.

---

## 🧠 Architecture Evolution: Why we added a Background Worker (v1.1)

In v1.0 of this project, the Android app deliberately avoided background tasks. The idea was to prevent Android's aggressive battery optimizations from killing the app, forcing users to manually open the app to refresh their session if Mitsubishi disconnected them. 

**The Problem with v1.0:** 
This manual approach proved frustrating. When the session expired in the background, Google Home would suddenly reply with *"Melhome is unavailable"*. Users had to open their phone just to make voice commands work again, defeating the purpose of a smart home integration.

**The v1.1 Solution:**
We completely redesigned the authentication flow to be 100% autonomous while maintaining maximum security:
1.  **Military-Grade Local Security:** Your MELCloud password is never stored in plain text. It is locked inside your phone using Android's `EncryptedSharedPreferences` (AES256_GCM).
2.  **Smart Background Worker:** The app now uses a modern Android `WorkManager`. Every 2 hours, this lightweight background task wakes up, securely decrypts your session, and silently pushes a fresh access token to your Render bridge. 
3.  **Result:** Google Home always has a valid token. You get **24/7 uptime without ever needing to open the app manually again**, with zero impact on your phone's battery!

---

## ⚠️ Crucial Optimization: Preventing Server Sleep

Even though your phone now syncs perfectly in the background, if you are hosting this bridge on **Render**'s free tier, the server itself will automatically go to sleep after 15 minutes of inactivity. Waking it up takes ~30 seconds, causing Google Home to time out and report an error on your first command.

### The Solution: Keep the Server Awake
To maintain an instant response time, use a free "ping" service to keep your Render instance awake 24/7.

**Configuration using UptimeRobot (Takes 2 minutes):**

1. Create a free account on [UptimeRobot.com](https://uptimerobot.com).
2. From your dashboard, click the green **+ Add New Monitor** button.
3. Fill out the form with the following settings:
   * **Monitor Type**: `HTTP(s)`
   * **Friendly Name**: `Melhome Bridge Render` (or any preferred name)
   * **URL (or IP)**: `https://YOUR_RENDER_ADDRESS.onrender.com/oauth/auth` *(Make sure to replace this with your actual Render URL!)*
   * **Monitoring Interval**: `10 minutes` *(Must be strictly set below 15 minutes).*
4. Scroll down and click **Create Monitor**.

That's it! UptimeRobot keeps the server alive, and your phone's new background Worker keeps the session alive. Your integration is now rock solid.

---

## 💻 Server Files (For reference)
This repository contains the necessary files to deploy the Node.js server (`index.js` and `package.json`). 

**Note: You do not need to download or manually edit these files.** By providing the link to this public repository directly in Render (Step 1), the platform will automatically fetch and run the code for you!

---

## 👨‍💻 Credits & Support

Created and maintained by **[tountoun66](https://github.com/tountoun66)**.

If you encounter any issues, have questions, or want to suggest improvements, please open an issue on the [GitHub repository Issues tab](https://github.com/tountoun66/melhome-bridge/issues).

## 📄 License

This project is open-source and distributed under the **MIT License**. Feel free to use, modify, and distribute it as you see fit.

## ⚠️ Disclaimer

This project is an independent, community-driven creation and is **not affiliated with, endorsed by, or associated with Mitsubishi Electric, MELCloud, or Google**. Use this software at your own risk. The author is not responsible for any damage, unexpected behavior, or warranty voidance that may occur to your HVAC units as a result of using this software.
