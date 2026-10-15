package com.aiaagent.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.net.Uri;
import java.util.ArrayList;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final int VOICE_REQUEST = 1001;
    private WebView webView;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, status -> { if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("bn", "IN")); });
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new VoiceBridge(), "AndroidVoice");
        webView.addJavascriptInterface(new PhoneBridge(), "PhoneControl");
        webView.addJavascriptInterface(new PdfBridge(this), "AndroidPDF");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 2001);
        }
    }

    public void createPdf(String text, String filename) {
        runOnUiThread(() -> {
            WebView printView = new WebView(MainActivity.this);
            String safeText = android.text.TextUtils.htmlEncode(text);
            String html = "<html><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family:sans-serif;padding:24px;white-space:pre-wrap\">"
                + safeText + "</body></html>";
            printView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            printView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                    String jobName = (filename == null || filename.trim().isEmpty())
                        ? "AI Agent Document" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
                    pm.print(jobName, view.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder().build());
                }
            });
        });
    }

    public class VoiceBridge {
        @JavascriptInterface public void startVoice() {
            runOnUiThread(() -> MainActivity.this.startVoice());
        }
        @JavascriptInterface public void speak(String text) {
            runOnUiThread(() -> {
                if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agent-voice");
            });
        }
    }

    public class PhoneBridge {
        private boolean action(String a) { AgentAccessibilityService x=AgentAccessibilityService.instance; return x!=null && x.doAction(a); }
        @JavascriptInterface public boolean back(){return action("back");}
        @JavascriptInterface public boolean home(){return action("home");}
        @JavascriptInterface public boolean recents(){return action("recents");}
        @JavascriptInterface public boolean notifications(){return action("notifications");}
        @JavascriptInterface public boolean quickSettings(){return action("quicksettings");}
        @JavascriptInterface public void openAccessibilitySettings(){runOnUiThread(() -> startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));}

        @JavascriptInterface public void openSettings() {
            runOnUiThread(() -> startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));
        }
        @JavascriptInterface public void openYouTube() {
            runOnUiThread(() -> {
                Intent i = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
                if (i != null) startActivity(i);
                else startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")));
            });
        }
        @JavascriptInterface public void openCamera() {
            runOnUiThread(() -> {
                Intent i = new Intent("android.media.action.IMAGE_CAPTURE");
                if (i.resolveActivity(getPackageManager()) != null) startActivity(i);
            });
        }
        @JavascriptInterface public void openBrowser() {
            runOnUiThread(() -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))));
        }
        @JavascriptInterface public void openDialer() {
            runOnUiThread(() -> startActivity(new Intent(Intent.ACTION_DIAL)));
        }
    }

    public void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-IN");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন...");
        try { startActivityForResult(i, VOICE_REQUEST); }
        catch (Exception e) {
            webView.evaluateJavascript("window.showAgentMessage('Voice service unavailable');", null);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> a = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (a != null && !a.isEmpty()) {
                webView.evaluateJavascript("window.setVoiceText(" +
                    org.json.JSONObject.quote(a.get(0)) + ");", null);
            }
        }
    }
}
