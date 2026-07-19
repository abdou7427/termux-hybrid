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
import java.util.regex.Pattern;

public class WebViewBridge {
    private TermuxActivity mActivity;
    private WebView mActiveWebView;
    private SharedPreferences mPrefs;

    // قائمة بيضاء لأسماء دوال الـ callback المسموح استدعاؤها في JS
    private static final Pattern SAFE_CALLBACK = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    public WebViewBridge(TermuxActivity activity) {
        this.mActivity = activity;
        this.mPrefs = activity.getSharedPreferences("hybrid_prefs", Activity.MODE_PRIVATE);
    }

    public void setActiveWebView(WebView webView) {
        this.mActiveWebView = webView;
    }

    // ==========================================
    // 1. أوامر الطرفية (Execution)
    // ==========================================

    @JavascriptInterface
    public void exec(String command) {
        if (command == null || command.isEmpty()) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) session.write(command + "\n");
    }

    // تنفيذ أمر عام (يبقى مقصوراً على الواجهات الموثوقة control.html/settings.html فقط
    // بما أن الجسر لم يعد يُضاف إلا للواجهات الموثوقة - انظر TermuxActivity)
    @JavascriptInterface
    public void runCommand(final String command, final String callbackFunction) {
        if (command == null || command.isEmpty()) return;
        if (!isSafeCallback(callbackFunction)) return;
        runShell(command, callbackFunction, false);
    }

    @JavascriptInterface
    public void runCommandStream(final String command, final String callbackFunction) {
        if (command == null || command.isEmpty()) return;
        if (!isSafeCallback(callbackFunction)) return;
        runShell(command, callbackFunction, true);
    }

    // مسار مخصص لرسائل الدردشة: النص يُمرَّر كوسيطة عبر بيئة العملية
    // وليس مركّباً داخل سطر أوامر شل -> يقضي على shell injection نهائياً
    @JavascriptInterface
    public void sendAgentMessage(final String prompt, final String translateChoice,
                                  final String filePath, final String callbackFunction) {
        if (!isSafeCallback(callbackFunction)) return;
        new Thread(() -> {
            try {
                String shellPath = "/data/data/" + mActivity.getPackageName() + "/files/usr/bin/sh";
                String home = "/data/data/" + mActivity.getPackageName() + "/files/home";
                String script = home + "/webui/run_agent.sh";

                // الوسائط تُمرَّر كعناصر منفصلة في المصفوفة -> لا يمر عبر "sh -c" مطلقاً
                // بذلك $()، backticks، ; ، | تبقى نصاً حرفياً ولا تُفسَّر أبداً
                ProcessBuilder pb = new ProcessBuilder(
                        script,
                        prompt == null ? "" : prompt,
                        "y".equals(translateChoice) ? "y" : "n",
                        filePath == null ? "" : filePath
                );
                pb.directory(new File(home));
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

    // جلب سجل الدردشة: chatId يُتحقق أنه رقم فقط قبل أي استخدام
    @JavascriptInterface
    public void getChatHistory(final String callbackFunction) {
        if (!isSafeCallback(callbackFunction)) return;
        String home = "/data/data/" + mActivity.getPackageName() + "/files/home";
        String cmd = home + "/webui/db_reader.py list";
        runShell(cmd, callbackFunction, false);
    }

    @JavascriptInterface
    public void getChatById(final String chatId, final String callbackFunction) {
        if (!isSafeCallback(callbackFunction)) return;
        if (chatId == null || !chatId.matches("^[0-9]+$")) {
            sendToWebView(callbackFunction, "Error: invalid chat id");
            return;
        }
        String home = "/data/data/" + mActivity.getPackageName() + "/files/home";
        String cmd = home + "/webui/db_reader.py get " + chatId; // آمن الآن: chatId رقمي مضمون
        runShell(cmd, callbackFunction, false);
    }

    private void runShell(final String command, final String callbackFunction, final boolean stream) {
        new Thread(() -> {
            try {
                String shellPath = "/data/data/" + mActivity.getPackageName() + "/files/usr/bin/sh";
                ProcessBuilder pb = new ProcessBuilder(shellPath, "-c", command);
                pb.directory(new File("/data/data/" + mActivity.getPackageName() + "/files/home"));
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                if (stream) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sendToWebView(callbackFunction, line);
                    }
                    process.waitFor();
                    reader.close();
                    sendToWebView(callbackFunction, "__EXECUTION_DONE__");
                } else {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    process.waitFor();
                    reader.close();
                    sendToWebView(callbackFunction, output.toString());
                }
            } catch (Exception e) {
                sendToWebView(callbackFunction, "Error: " + e.getMessage());
                if (stream) sendToWebView(callbackFunction, "__EXECUTION_DONE__");
            }
        }).start();
    }

    // ==========================================
    // 2. الإعدادات والتخزين (SharedPreferences)
    // ==========================================

    @JavascriptInterface
    public void savePreference(String key, String value) {
        if (key == null) return;
        mPrefs.edit().putString(key, value).apply();
    }

    @JavascriptInterface
    public String getPreference(String key) {
        if (key == null) return "";
        return mPrefs.getString(key, "");
    }

    // ==========================================
    // 3. التفاعل مع النظام والواجهة (System & UI)
    // ==========================================

    @JavascriptInterface
    public void openBrowser(String url) {
        if (url != null && !url.isEmpty()) {
            mActivity.openBrowserPopup(url); // الفلترة الآن داخل openBrowserPopup نفسها
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
        if ("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS".equals(action) ||
            "android.settings.APPLICATION_DETAILS_SETTINGS".equals(action)) {
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
        if (message == null) return;
        mActivity.runOnUiThread(() ->
            android.widget.Toast.makeText(mActivity, message, android.widget.Toast.LENGTH_SHORT).show());
    }

    // ==========================================
    // 4. إرفاق الملفات (File Picker)
    // ==========================================

    @JavascriptInterface
    public void pickFile(String callbackFunction) {
        if (!isSafeCallback(callbackFunction)) return;
        mActivity.requestFilePick(callbackFunction);
    }

    // ==========================================
    // دوال مساعدة داخلية (Helper Methods)
    // ==========================================

    private boolean isSafeCallback(String callbackFunction) {
        return callbackFunction != null && SAFE_CALLBACK.matcher(callbackFunction).matches();
    }

    private void sendToWebView(final String callbackFunction, final String result) {
        if (mActiveWebView == null || !isSafeCallback(callbackFunction)) return;
        mActivity.runOnUiThread(() -> {
            try {
                String escapedResult = result.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                        .replace("</", "<\\/"); // يمنع كسر </script> لو وُلّد HTML لاحقاً
                String js = "if(window." + callbackFunction + ") window." + callbackFunction + "(\"" + escapedResult + "\");";
                mActiveWebView.evaluateJavascript(js, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void sendToWebViewDirect(final String callbackFunction, final String result) {
        if (mActiveWebView == null || !isSafeCallback(callbackFunction) || result == null) return;
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
