package com.mimfx.ffpanel;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
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

public class FloatingPanelService extends Service {
    private WindowManager wm;
    private View panel;
    private WindowManager.LayoutParams params;
    @Override public IBinder onBind(Intent intent){ return null; }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        if(panel==null) showPanel();
        return START_NOT_STICKY;
    }
    private void showPanel(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(18,14,18,14);
        box.setBackgroundColor(0xEE181818);
        TextView title=new TextView(this); title.setText("MIM FX  •  GAMING PANEL"); title.setTextColor(0xFFFF5555); title.setTextSize(14); box.addView(title);
        TextView note=new TextView(this); note.setText("Quick overlay active. Game controls remain unchanged."); note.setTextColor(0xFFFFFFFF); note.setTextSize(11); box.addView(note);
        Button close=new Button(this); close.setText("CLOSE PANEL"); close.setOnClickListener(v->stopSelf()); box.addView(close);
        panel=box;
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        params=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.END; params.x=20; params.y=140;
        box.setOnTouchListener(new View.OnTouchListener(){
            int sx,sy; float tx,ty;
            public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=params.x;sy=params.y;tx=e.getRawX();ty=e.getRawY();return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){params.x=sx+(int)(tx-e.getRawX());params.y=sy+(int)(e.getRawY()-ty);wm.updateViewLayout(panel,params);return true;}
                return false;
            }
        });
        wm.addView(panel,params);
    }
    @Override public void onDestroy(){if(panel!=null&&wm!=null){wm.removeView(panel);panel=null;}super.onDestroy();}
}