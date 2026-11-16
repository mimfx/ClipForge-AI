package com.mimfx.ffpanel;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class FloatingPanelService extends Service {
    private WindowManager wm;
    private View panel;
    private WindowManager.LayoutParams params;
    private TextView memoryText, batteryText;
    @Override public IBinder onBind(Intent intent){ return null; }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        if(panel==null) showPanel();
        refreshStats();
        return START_NOT_STICKY;
    }
    private void refreshStats(){
        ActivityManager am=(ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
        if(memoryText!=null) memoryText.setText(String.format(Locale.US,"Available RAM: %.0f / %.0f MB",mi.availMem/1048576.0,mi.totalMem/1048576.0));
        Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temp=b==null?-1:b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);
        if(batteryText!=null) batteryText.setText(temp<0?"Battery: unavailable":String.format(Locale.US,"Battery: %.1f °C",temp/10.0));
    }
    private void showPanel(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(14,10,14,10);
        box.setBackgroundColor(0xEE181818);
        TextView title=new TextView(this); title.setText("MIM FX  •  DRAG TO MOVE"); title.setTextColor(0xFFFF5555); title.setTextSize(13); box.addView(title);
        memoryText=new TextView(this); memoryText.setTextColor(0xFFFFFFFF); memoryText.setTextSize(12); box.addView(memoryText);
        batteryText=new TextView(this); batteryText.setTextColor(0xFFFFFFFF); batteryText.setTextSize(12); box.addView(batteryText);
        Button refresh=new Button(this); refresh.setText("REFRESH DEVICE STATUS"); refresh.setOnClickListener(v->refreshStats()); box.addView(refresh);
        Button launch=new Button(this); launch.setText("OPEN FREE FIRE"); launch.setOnClickListener(v->launchGame()); box.addView(launch);
        Button close=new Button(this); close.setText("CLOSE PANEL"); close.setOnClickListener(v->stopSelf()); box.addView(close);
        panel=box;
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        params=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.END; params.x=16; params.y=120;
        title.setOnTouchListener(new View.OnTouchListener(){
            int sx,sy; float tx,ty;
            public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=params.x;sy=params.y;tx=e.getRawX();ty=e.getRawY();return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){params.x=sx+(int)(tx-e.getRawX());params.y=sy+(int)(e.getRawY()-ty);wm.updateViewLayout(panel,params);return true;}
                return e.getAction()==MotionEvent.ACTION_UP;
            }
        });
        wm.addView(panel,params);
    }
    private void launchGame(){
        String[] ids={"com.dts.freefireth","com.dts.freefiremax"};
        for(String id:ids){Intent i=getPackageManager().getLaunchIntentForPackage(id);if(i!=null){i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return;}}
    }
    @Override public void onDestroy(){if(panel!=null&&wm!=null){wm.removeView(panel);panel=null;}super.onDestroy();}
}