package com.termux.app;

import android.webkit.JavascriptInterface;
import com.termux.terminal.TerminalSession;

/**
 * JavascriptInterface bridge that pipes web commands into the active Termux shell.
 * Compatible with Termux's TerminalSession.write(String) API.
 */
public class WebViewBridge {
    private TermuxActivity mActivity;

    public WebViewBridge(TermuxActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Called from JavaScript: window.TermuxBridge.exec("ls -la");
     * Appends a newline so the shell executes the command immediately.
     */
    @JavascriptInterface
    public void exec(String command) {
        if (command == null || command.isEmpty()) return;

        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) {
            session.write(command + "\n");
        }
    }

    /**
     * Called from JavaScript: window.TermuxBridge.type("pkg install ");
     * Sends raw text without pressing Enter (useful for partial input).
     */
    @JavascriptInterface
    public void type(String text) {
        if (text == null) return;

        TerminalSession session = mActivity.getCurrentSession();
        if (session != null) {
            session.write(text);
        }
    }
}
