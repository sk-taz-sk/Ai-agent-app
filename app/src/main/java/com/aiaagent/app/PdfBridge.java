package com.aiaagent.app;

import android.webkit.JavascriptInterface;

public class PdfBridge {
    private final MainActivity activity;

    public PdfBridge(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void saveTextAsPdf(String text, String filename) {
        activity.createPdf(text, filename);
    }
}
