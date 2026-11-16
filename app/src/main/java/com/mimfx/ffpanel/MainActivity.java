package com.mimfx.ffpanel;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
        @JavascriptInterface public String batteryTemperature() {
            Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if(i==null) return "Battery temperature unavailable";
            int t=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);
            return t<0?"Battery temperature unavailable":String.format(Locale.US,"Battery: %.1f °C",t/10.0);
        }
        @JavascriptInterface public String overlayStatus() {
            return Settings.canDrawOverlays(MainActivity.this)?"Overlay permission granted":"Overlay permission not granted";
        }
        @JavascriptInterface public void enableFloatingPanel() {
            runOnUiThread(()->{
                if(!Settings.canDrawOverlays(MainActivity.this)){
                    Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()));
                    startActivity(i);
                } else {
                    startService(new Intent(MainActivity.this,FloatingPanelService.class));
                }
            });
        }
        @JavascriptInterface public void stopFloatingPanel() {
            runOnUiThread(()->stopService(new Intent(MainActivity.this,FloatingPanelService.class)));
        }
        @JavascriptInterface public String launchFreeFire() {
            String[] ids={"com.dts.freefireth","com.dts.freefiremax"};
            for(String id:ids) { Intent i=getPackageManager().getLaunchIntentForPackage(id); if(i!=null){runOnUiThread(()->startActivity(i)); return "Launching "+id;} }
            return "Free Fire was not found. Install the game first.";
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if(web!=null) web.evaluateJavascript("if(window.refreshOverlayStatus)refreshOverlayStatus()",null);
    }
    @Override public void onBackPressed() { if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}