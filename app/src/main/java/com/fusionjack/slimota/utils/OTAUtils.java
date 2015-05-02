package com.fusionjack.slimota.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.fusionjack.slimota.R;
import com.fusionjack.slimota.parser.OTADevice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fusionjack on 01.05.15.
 */
public final class OTAUtils {

    private static final String TAG = "SlimOTA";
    private static final boolean DEBUG = true;

    private static String mCurrentVersion = "";
    private static String mDeviceName = "";

    private OTAUtils() {
    }

    public static void logError(Exception e) {
        if (DEBUG) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static void logInfo(String message) {
        if (DEBUG) {
            Log.i(TAG, message);
        }
    }

    public static String getReleaseType(Context context) {
        if (context != null) {
            return context.getResources().getString(R.string.release_type);
        }
        return "";
    }

    public static String getCurrentVersion(Context context) {
        if (context != null && mCurrentVersion.isEmpty()) {
            final String propName = context.getResources().getString(R.string.build_name);
            mCurrentVersion = getProperty(propName);
        }
        return mCurrentVersion;
    }

    public static String getDeviceName(Context context) {
        if (context != null && mDeviceName.isEmpty()) {
            final String propName = context.getResources().getString(R.string.device_name);
            mDeviceName = getProperty(propName);
        }
        return mDeviceName;
    }

    public static boolean checkVersion(OTADevice device, Context context) {
        if (device == null || context == null) {
            return false;
        }

        String serverBuildname = device.getFilename();
        String localBuildname = getCurrentVersion(context);
        OTAUtils.logInfo("serverBuildname: " + serverBuildname);
        OTAUtils.logInfo("localBuildname: " + localBuildname);
        if (serverBuildname.isEmpty() || localBuildname.isEmpty()) {
            return false;
        }

        return checkVersion(localBuildname, serverBuildname, context);
    }

    public static boolean checkVersion(String localBuildname, String serverBuildname, Context context) {
        OTAVersion version = new OTAVersion(context);
        final String delimiter = version.getDelimiter();
        final int position = version.getPosition();
        final SimpleDateFormat format = version.getFormat();

        String[] localTokens = localBuildname.split(delimiter);
        String[] serverTokens = serverBuildname.split(delimiter);
        if (position > -1 && position < localTokens.length && position < serverTokens.length) {
            String currentVersion = localTokens[position];
            String serverVersion = serverTokens[position];
            return isVersionNewer(serverVersion, currentVersion, format);
        }
        return false;
    }

    private static boolean isVersionNewer(String serverVersion, String currentVersion,
                                          final SimpleDateFormat format) {
        boolean versionIsNew = false;
        if (format == null || serverVersion.isEmpty() || currentVersion.isEmpty()) {
            return versionIsNew;
        }
        try {
            Date serverDate = format.parse(serverVersion);
            Date currentDate = format.parse(currentVersion);
            versionIsNew = serverDate.after(currentDate);
        } catch (ParseException e) {
            logError(e);
        }
        return versionIsNew;
    }

    public static String getProperty(String property) {
        Process process = null;
        BufferedReader buff = null;
        try {
            process = Runtime.getRuntime().exec("getprop " + property);
            buff = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return buff.readLine();
        } catch (IOException e) {
            logError(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
            try {
                if (buff != null) {
                    buff.close();
                }
            } catch (IOException e) {
                logError(e);
            }
        }
        return "";
    }

    public static InputStream downloadURL(String link) throws IOException {
        URL url = new URL(link);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        logInfo("downloadStatus: " + conn.getResponseCode());
        return conn.getInputStream();
    }

    public static void launchUrl(String url, Context context) {
        if (!url.isEmpty() && context != null) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }
}