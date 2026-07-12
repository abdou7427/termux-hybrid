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
     * Called from JavaScript: window.TermuxBridge.exec("ls -la");
     */
    @JavascriptInterface
    public void exec(String command) {
        if (command == null || command.isEmpty()) return;

        mActivity.runOnUiThread(() -> {
            TerminalSession session = mActivity.getCurrentSession();
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
            TerminalSession session = mActivity.getCurrentSession();
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
        TerminalSession session = mActivity.getCurrentSession();
        return (session != null) ? session.getShellPid() : -1;
    }
}
