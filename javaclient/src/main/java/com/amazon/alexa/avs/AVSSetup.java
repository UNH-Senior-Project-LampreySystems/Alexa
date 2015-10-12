/* Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved. */

package com.amazon.alexa.avs;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Timer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.apache.commons.io.IOUtils;

public class AVSSetup {
    private static final String AVSCONFIG = "config.json";
    private static final int DEFAULT_EXPIRES_IN_S = 3600;
    private static final int RETRY_INTERVAL_IN_S = 2;
    private static final int POLLING_INTERVAL_IN_MS = 1 * 1000;
    private static final int MAX_RETRIES = 3;
    private static final Timer mTimer = new Timer();
    private final AVSApp mGui;
    private String mProductId;
    private String mSerial;
    private String mUrl;
    private String mAccess;
    private String mRegcode;
    private String mDeviceSecret;

    public AVSSetup(AVSApp gui) {
        mGui = gui;

        Thread thread = new Thread() {
            @Override
            public void run() {
                startRegistration();
            }
        };
        thread.start();
    }

    private void startRegistration() {
        if (!readConfigFile()) {
            System.out.println("\nWarning: Unable to load config file. "
                    + "You will have to manually enter the authorization token");
            return;
        }

        if (mDeviceSecret == null && mRegcode == null) {
            doFreshRegistration();
        } else if (mDeviceSecret != null && mRegcode != null) {
            doUnfinishedRegistration();
        } else if (mDeviceSecret != null && mRegcode == null) {
            doFinishedRegistration();
        }
    }

    private void doFreshRegistration() {
        requestRegistrationCode();
        promptUserToRegister();
        requestAccessToken(true);
    }

    private void doUnfinishedRegistration() {
        promptUserToRegister();
        requestAccessToken(true);
    }

    private void doFinishedRegistration() {
        getNewAccessToken();
    }

    private void getNewAccessToken() {
        requestAccessToken(false);
        if (mAccess == null) {
            // We didn't get an accessToken with our DeviceSecret, this could happen for a couple reasons:
            // + It expired, which means the user never finished registration
            // + The server got restarted, and lost the value
            // Both are reasons to just delete it, and start registration fully over
            mDeviceSecret = null;
            updateConfigFile();
            doFreshRegistration();
        }
    }

    private void requestRegistrationCode() {
        System.out.println("Requesting Registration Code");

        int retries = MAX_RETRIES;
        int retryCount = 0;
        do {
            try {
                JsonReader reader = callService("/device/regcode/" + mProductId + "/" + mSerial);
                if (reader != null) {
                    JsonObject results = reader.readObject();
                    if (!isError(results)) {
                        mRegcode = results.getString("regCode");
                        mDeviceSecret = results.getString("deviceSecret");
                        updateConfigFile();
                        break;
                    } else {
                        String error = results.getString("error");
                        String message = results.getString("message");
                        System.out.println("There was an error getting the registration code: " + error + " - "
                                + message);
                        System.out.println("Retrying in " + RETRY_INTERVAL_IN_S + " seconds.");
                    }
                }
            } catch (IOException ie) {
                System.out
                        .println("There was a problem accessing the service. Please make sure the server is running.");
                System.out.println("Retrying in " + RETRY_INTERVAL_IN_S + " seconds.");
            }

            // Sleep 5 seconds before trying again
            sleep(RETRY_INTERVAL_IN_S * 1000);
            retryCount++;
        } while (retryCount < retries);

        if (retryCount == retries) {
            System.out.println("There were too many retries. Please restart the server and try again.");
            System.exit(1);
        }
    }

    private void promptUserToRegister() {
        String regUrl = mUrl + "/device/register/" + mRegcode;
        mGui.showDialog("Please register your device by visiting the following website on "
                + "any system and following the instructions:\n" + regUrl + "\n\n Hit OK once completed.");
    }

    private void requestAccessToken(boolean polling) {
        System.out.println("Requesting Access Token");

        int retries = polling ? Integer.MAX_VALUE : MAX_RETRIES;

        int retryCount = 0;
        do {
            try {
                JsonReader reader =
                        callService("/device/accesstoken/" + mProductId + "/" + mSerial + "/" + mDeviceSecret);
                if (reader != null) {
                    JsonObject results = reader.readObject();
                    if (!isError(results)) {
                        String pollStatus = results.getString("poll_status", null);
                        if (pollStatus != null) {
                            sleep(POLLING_INTERVAL_IN_MS);
                            continue;
                        }

                        // Null out Regcode now that the customer is fully registered
                        mRegcode = null;
                        mAccess = results.getString("access");
                        updateConfigFile();

                        // Refresh the token quicker than required
                        int expires = results.getInt("expires", DEFAULT_EXPIRES_IN_S);
                        expires = (int) (0.75f * expires);

                        mTimer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("Refreshing...");
                                getNewAccessToken();
                            }
                        }, expires * 1000); // expires is in seconds, so make ms

                        break;
                    } else {
                        String error = results.getString("error");
                        String message = results.getString("message");
                        System.out.println("There was an error getting the Access Token: " + error + " - "
                                + message);
                        System.out.println("Retrying in " + RETRY_INTERVAL_IN_S + " seconds.");
                    }
                }
            } catch (IOException ie) {
                System.out
                        .println("There was a problem accessing the service. Please make sure the server is running.");
                System.out.println("Retrying in " + RETRY_INTERVAL_IN_S + " seconds.");
            }

            sleep(RETRY_INTERVAL_IN_S * 1000);
            retryCount++;
        } while (retryCount < retries);

        if (retryCount == retries) {
            mAccess = null;
        }

        if (mAccess != null) {
            mGui.setToken(mAccess);
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isError(JsonObject object) {
        return object.getString("error", null) != null;
    }

    private boolean readConfigFile() {
        FileInputStream file = null;
        try {
            file = new FileInputStream(AVSCONFIG);
            JsonReader json = Json.createReader(file);
            JsonObject topObj = json.readObject();
            mProductId = topObj.getString("pid");
            mSerial = topObj.getString("serial");
            mUrl = topObj.getString("url");
            mRegcode = topObj.getString("regcode", null);
            mDeviceSecret = topObj.getString("deviceSecret", null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            IOUtils.closeQuietly(file);
        }
        return true;
    }

    private synchronized void updateConfigFile() {
        FileOutputStream fos = null;
        try {
            JsonObjectBuilder builder =
                    Json.createObjectBuilder().add("pid", mProductId).add("serial", mSerial).add("url", mUrl);
            if (mDeviceSecret != null) {
                builder.add("deviceSecret", mDeviceSecret);
            }

            if (mRegcode != null) {
                builder.add("regcode", mRegcode);
            }

            fos = new FileOutputStream(AVSCONFIG);
            JsonWriter jsonWriter = Json.createWriter(fos);
            jsonWriter.writeObject(builder.build());
            jsonWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private JsonReader callService(String path) throws IOException {
        InputStream response = null;
        try {
            String[] parts = path.split("/");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                sb.append(URLEncoder.encode(part, StandardCharsets.UTF_8.displayName()));
                sb.append("/");
            }

            URL obj = new URL(mUrl + sb.toString());
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            if (con.getResponseCode() == 200) {
                response = con.getInputStream();
            } else {
                response = con.getErrorStream();
            }

            String responsestring = IOUtils.toString(response);
            System.out.println(responsestring);
            String charSet =
                    AVSClient.getHeaderParameter(con.getHeaderField("Content-Type"), "charset",
                            StandardCharsets.UTF_8.displayName());
            JsonReader reader = Json.createReader(new ByteArrayInputStream(responsestring.getBytes(charSet)));
            return reader;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(response);
        }
        return null;
    }

}
