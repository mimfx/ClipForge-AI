package com.mimfx.ffpanel;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView web;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "MimFX");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    public class Bridge {
        @JavascriptInterface public String memoryInfo() {
            ActivityManager am=(ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
            return String.format(Locale.US,"Available: %.0f MB / Total: %.0f MB%s",mi.availMem/1048576.0,mi.totalMem/1048576.0,mi.lowMemory?" (low memory)":"");
        }
        @JavascriptInterface public String launchFreeFire() {
            String[] ids={"com.dts.freefireth","com.dts.freefiremax"};
            for(String id:ids) { Intent i=getPackageManager().getLaunchIntentForPackage(id); if(i!=null){runOnUiThread(()->startActivity(i)); return "Launching "+id;} }
            return "Free Fire was not found. Install the game first.";
        }
        @JavascriptInterface public void toast(String msg) { runOnUiThread(()->Toast.makeText(MainActivity.this,msg,Toast.LENGTH_SHORT).show()); }
    }
    @Override public void onBackPressed() { if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
