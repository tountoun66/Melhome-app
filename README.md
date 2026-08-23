# 🏠 Melhome Bridge: Better MELCloud Home Integration for Google Home

This project provides a custom Android app and a self-hosted Node.js bridge to connect your Mitsubishi Air Conditioners (using MELCloud Home) to Google Home, overcoming the limitations of the official integration.

If you are tired of only being able to turn your AC on/off and change the temperature, this project is for you.

## ✨ Features

*   **Full Google Home Control:** Finally use Google Assistant to control fan speeds (e.g., *"Ok Google, set AC fan speed to 2"*).
*   **Custom Android App:** A fast, lightweight alternative to the official app, offering native control over Vanes (Vertical & Horizontal direction).
*   **Privacy-Focused & Self-Hosted:** You run your own bridge on a free Render account. Your MELCloud credentials never leave your control.
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

1.  Download the **ZIP file** containing the Melhome Android app directly from this GitHub repository. Extract the `.zip` file, and install the included `.apk` on your phone.
2.  Open the app and **log in to your MELCloud Home account** using your official credentials. *(Your login details remain on your phone and are never sent to our servers).*
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

## ⚠️ Crucial Optimization: Preventing Server Sleep & Data Loss

If you are hosting this bridge on **Render**'s free tier, the server will automatically go to sleep after 15 minutes of inactivity.

### Why this is critical:
* **Google Home Timeouts (Cold Start):** Waking up a sleeping instance takes ~30 seconds. Google Home times out after ~5 seconds, reporting: *"An error occurred"* or *"Melhome is unavailable"* on your first command.
* **HTTP 500 Errors & Unlinking Issues:** Because session data and pairing tokens are held in volatile memory, shutting down or restarting the instance clears this cache. If this happens, your mobile app may run into **500 Internal Server Errors**, and you will be forced to **unlink and reconnect your Google Home account** to restore functionality.

### The Solution: Keep the Server Awake
To maintain an instant response time and avoid session resets, use a free "ping" service to keep your Render instance awake 24/7.

**Configuration using UptimeRobot (Takes 2 minutes):**

1. Create a free account on [UptimeRobot.com](https://uptimerobot.com).
2. From your dashboard, click the green **+ Add New Monitor** button.
3. Fill out the form with the following settings:
   * **Monitor Type**: `HTTP(s)`
   * **Friendly Name**: `Melhome Bridge Render` (or any preferred name)
   * **URL (or IP)**: `https://YOUR_RENDER_ADDRESS.onrender.com/oauth/auth` *(Make sure to replace this with your actual Render URL!)*
   * **Monitoring Interval**: `10 minutes` *(Must be strictly set below 15 minutes).*
4. Scroll down and click **Create Monitor**.

That's it! UptimeRobot will ping your server every 10 minutes, keeping your instance permanently active, eliminating voice command delays, and preventing session drops.

---

## 💻 Server Files (For reference)
This repository contains the necessary files to deploy the Node.js server (`index.js` and `package.json`). 

**Note: You do not need to download or manually edit these files.** By providing the link to this public repository directly in Render (Step 1), the platform will automatically fetch and run the code for you!
