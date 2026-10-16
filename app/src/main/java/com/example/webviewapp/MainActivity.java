package com.example.webviewapp;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private boolean pendingAdd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pendingAdd = getIntent() != null && getIntent().getBooleanExtra("quick_add", false);

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);      // keeps localStorage (your expense data)
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        // JS -> Android bridge (used for the home screen widget)
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Only allow the app's own local files; block everything else
                return !url.startsWith("file:///android_asset/");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (pendingAdd) {
                    pendingAdd = false;
                    view.evaluateJavascript("window.openAdd&&openAdd()", null);
                }
            }
        });

        webView.loadUrl("file:///android_asset/html/index.html");
        setContentView(webView);

        // Back button: close sheet / go to Today tab first, then exit
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                webView.evaluateJavascript("(window.onBack&&onBack())?'1':'0'", value -> {
                    if (!"\"1\"".equals(value)) finish();
                });
            }
        });
    }

    // Widget "+" button while the app is already open
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra("quick_add", false)) {
            webView.evaluateJavascript("window.openAdd&&openAdd()", null);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void setWidgetData(String amount, String sub, String dateKey, int state,
                                  String zero, String zeroSub) {
            SharedPreferences.Editor e =
                    getSharedPreferences(SpendWidget.PREFS, MODE_PRIVATE).edit();
            e.putString("amount", amount)
                    .putString("sub", sub)
                    .putString("date", dateKey)
                    .putInt("state", state)
                    .putString("zero", zero)
                    .putString("zeroSub", zeroSub)
                    .apply();
            SpendWidget.refreshAll(getApplicationContext());
        }

        @JavascriptInterface
        public void pinWidget() {
            runOnUiThread(() -> {
                AppWidgetManager m = AppWidgetManager.getInstance(MainActivity.this);
                ComponentName cn = new ComponentName(MainActivity.this, SpendWidget.class);
                if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported()) {
                    m.requestPinAppWidget(cn, null, null);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Long-press your home screen → Widgets → Spend Pool Daily",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
        
    }
}
