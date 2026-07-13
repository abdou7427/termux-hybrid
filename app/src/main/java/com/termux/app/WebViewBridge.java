package com.termux.app;

import android.webkit.JavascriptInterface;
import com.termux.terminal.TerminalSession;

public class WebViewBridge {
    private TermuxActivity mActivity;

    public WebViewBridge(TermuxActivity activity) {
        this.mActivity = activity;
    }

    @JavascriptInterface
    public void exec(String command) {
        if (command == null || command.isEmpty()) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) session.write(command + "\n");
    }

    @JavascriptInterface
    public void type(String text) {
        if (text == null) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) session.write(text);
    }

    @JavascriptInterface
    public void openBrowser(String url) {
        if (url != null && !url.isEmpty()) {
            mActivity.openBrowserPopup(url);
        }
    }

    @JavascriptInterface
    public void openSystemSettings(String action) {
        android.content.Intent intent = new android.content.Intent();
        if (action == null || action.isEmpty()) {
            intent.setAction(android.provider.Settings.ACTION_SETTINGS);
        } else {
            intent.setAction(action);
        }
        mActivity.startActivity(intent);
    }

    @JavascriptInterface
    public void toast(String message) {
        android.widget.Toast.makeText(mActivity, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
