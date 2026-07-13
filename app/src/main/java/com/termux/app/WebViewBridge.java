package com.termux.app;

import android.webkit.JavascriptInterface;
import com.termux.terminal.TerminalSession;
import java.nio.charset.Charset;

/**
 * JavascriptInterface bridge that pipes web commands into the active Termux shell.
 */
public class WebViewBridge {
    private TermuxActivity mActivity;

    public WebViewBridge(TermuxActivity activity) {
        this.mActivity = activity;
    }

    /**
     * Helper method to safely retrieve the active terminal session from TermuxActivity
     */
    private TerminalSession getActiveSession() {
        if (mActivity == null) return null;
        
        // 1. Try to get it via TerminalView if available
        if (mActivity.getTerminalView() != null) {
            return mActivity.getTerminalView().getCurrentSession();
        }
        
        // 2. Fallback to direct field/method if accessible (Custom forks setup)
        try {
            java.lang.reflect.Method method = mActivity.getClass().getMethod("getCurrentSession");
            return (TerminalSession) method.invoke(mActivity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Called from JavaScript: window.TermuxBridge.exec("ls -la");
     */
    @JavascriptInterface
    public void exec(String command) {
        if (command == null || command.isEmpty()) return;

        mActivity.runOnUiThread(() -> {
            TerminalSession session = getActiveSession();
            if (session != null) {
                // Append newline so the shell executes immediately
                String payload = command + "\n";
                byte[] bytes = payload.getBytes(Charset.defaultCharset());
                session.write(bytes, bytes.length);
            }
        });
    }

    /**
     * Called from JavaScript: window.TermuxBridge.type("pkg install ");
     * (sends text without pressing Enter)
     */
    @JavascriptInterface
    public void type(String text) {
        if (text == null) return;

        mActivity.runOnUiThread(() -> {
            TerminalSession session = getActiveSession();
            if (session != null) {
                byte[] bytes = text.getBytes(Charset.defaultCharset());
                session.write(bytes, bytes.length);
            }
        });
    }

        /**
     * Returns the current shell PID to the web layer.
     */
    @JavascriptInterface
    public int getShellPid() {
        return -1;
    }

