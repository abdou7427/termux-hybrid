package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.termux.terminal.TerminalSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class WebViewBridge {
    private TermuxActivity mActivity;
    private WebView mActiveWebView;
    private SharedPreferences mPrefs;

    public WebViewBridge(TermuxActivity activity) {
        this.mActivity = activity;
        this.mPrefs = activity.getSharedPreferences("hybrid_prefs", Activity.MODE_PRIVATE);
    }

    // تحديد الويب فيو النشط لإرسال النتائج إليه
    public void setActiveWebView(WebView webView) {
        this.mActiveWebView = webView;
    }

    // ==========================================
    // 1. أوامر الطرفية (Execution)
    // ==========================================

    // كتابة مباشرة في الطرفية الأصلية (للأوامر التفاعلية أو إغلاق الجلسات)
    @JavascriptInterface
    public void exec(String command) {
        if (command == null || command.isEmpty()) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) session.write(command + "\n");
    }

    // تنفيذ أمر وجلب النتيجة دفعة واحدة (للشات والإعدادات وقراءة قواعد البيانات)
    @JavascriptInterface
    public void runCommand(final String command, final String callbackFunction) {
        new Thread(() -> {
            try {
                String shellPath = "/data/data/" + mActivity.getPackageName() + "/files/usr/bin/sh";
                ProcessBuilder pb = new ProcessBuilder(shellPath, "-c", command);
                pb.directory(new File("/data/data/" + mActivity.getPackageName() + "/files/home"));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                process.waitFor();
                reader.close();

                sendToWebView(callbackFunction, output.toString());
            } catch (Exception e) {
                sendToWebView(callbackFunction, "Error: " + e.getMessage());
            }
        }).start();
    }

    // تنفيذ أمر مع البث المباشر للسطور (لواجهة Control - Terminal Emulator)
    @JavascriptInterface
    public void runCommandStream(final String command, final String callbackFunction) {
        new Thread(() -> {
            try {
                String shellPath = "/data/data/" + mActivity.getPackageName() + "/files/usr/bin/sh";
                ProcessBuilder pb = new ProcessBuilder(shellPath, "-c", command);
                pb.directory(new File("/data/data/" + mActivity.getPackageName() + "/files/home"));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    sendToWebView(callbackFunction, line);
                }
                process.waitFor();
                reader.close();
                // إشعار بنهاية التنفيذ لواجهة الويب
                sendToWebView(callbackFunction, "__EXECUTION_DONE__");
            } catch (Exception e) {
                sendToWebView(callbackFunction, "Error: " + e.getMessage());
                sendToWebView(callbackFunction, "__EXECUTION_DONE__");
            }
        }).start();
    }

    // ==========================================
    // 2. الإعدادات والتخزين (SharedPreferences)
    // ==========================================

    @JavascriptInterface
    public void savePreference(String key, String value) {
        mPrefs.edit().putString(key, value).apply();
    }

    @JavascriptInterface
    public String getPreference(String key) {
        return mPrefs.getString(key, "");
    }

    // ==========================================
    // 3. التفاعل مع النظام والواجهة (System & UI)
    // ==========================================

    @JavascriptInterface
    public void openBrowser(String url) {
        if (url != null && !url.isEmpty()) {
            mActivity.openBrowserPopup(url);
        }
    }

    @JavascriptInterface
    public void openSystemSettings(String action) {
        Intent intent = new Intent();
        if (action == null || action.isEmpty()) {
            intent.setAction(android.provider.Settings.ACTION_SETTINGS);
        } else {
            intent.setAction(action);
        }
        // إضافة حزمة التطبيق لإعدادات البطارية والتطبيق في أندرويد الحديث
        if (action.equals("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") || 
            action.equals("android.settings.APPLICATION_DETAILS_SETTINGS")) {
            intent.setData(android.net.Uri.parse("package:" + mActivity.getPackageName()));
        }
        mActivity.startActivity(intent);
    }

    @JavascriptInterface
    public void openNativeTerminal() {
        mActivity.runOnUiThread(() -> mActivity.switchToView(null));
    }

    @JavascriptInterface
    public void toast(String message) {
        android.widget.Toast.makeText(mActivity, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    // ==========================================
    // 4. إرفاق الملفات (File Picker)
    // ==========================================

    @JavascriptInterface
    public void pickFile(String callbackFunction) {
        mActivity.requestFilePick(callbackFunction);
    }

    // ==========================================
    // دوال مساعدة داخلية (Helper Methods)
    // ==========================================

    // إرسال بيانات عامة (مع Escape للحروف لتجنب كسر الجافاسكريبت)
    private void sendToWebView(final String callbackFunction, final String result) {
        if (mActiveWebView == null) return;
        mActivity.runOnUiThread(() -> {
            try {
                String escapedResult = result.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");

                String js = "if(window." + callbackFunction + ") window." + callbackFunction + "(\"" + escapedResult + "\");";
                mActiveWebView.evaluateJavascript(js, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // إرسال مسار ملف (بدون Escape معقد حتى لا يتلف المسار)
    public void sendToWebViewDirect(final String callbackFunction, final String result) {
        if (mActiveWebView == null) return;
        mActivity.runOnUiThread(() -> {
            try {
                String escapedResult = result.replace("\\", "\\\\").replace("\"", "\\\"");
                String js = "if(window." + callbackFunction + ") window." + callbackFunction + "(\"" + escapedResult + "\");";
                mActiveWebView.evaluateJavascript(js, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
