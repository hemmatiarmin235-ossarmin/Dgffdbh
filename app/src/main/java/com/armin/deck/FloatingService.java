package com.armin.deck;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.NotificationManager.Policy;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class FloatingService extends Service {
    private WindowManager wm;
    private View bubble, panel;
    private WindowManager.LayoutParams bubbleParams, panelParams;
    private boolean open = false;
    private final int purple = Color.rgb(164, 92, 255);
    private final int dark = Color.rgb(20, 20, 30);

    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        startAsForeground();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        makeBubble();
    }
    private void startAsForeground() {
        String channel = "armin_deck_tools";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(channel, "ARMIN DECK floating tools", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
            Notification n = new Notification.Builder(this, channel).setContentTitle("ARMIN DECK is ready").setContentText("Floating game controls are active").setSmallIcon(android.R.drawable.ic_media_play).build();
            startForeground(7124, n);
        }
    }
    private int type() { return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(2, purple); return d; }
    private void makeBubble() {
        TextView b = new TextView(this); b.setText("DECK"); b.setTextColor(Color.WHITE); b.setTextSize(12); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setGravity(Gravity.CENTER); b.setBackground(shape(dark, 100));
        bubble = b;
        bubbleParams = new WindowManager.LayoutParams(dp(62), dp(62), type(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.END; bubbleParams.x = dp(12); bubbleParams.y = dp(180);
        b.setOnClickListener(v -> togglePanel());
        b.setOnTouchListener(new View.OnTouchListener() { float sx, sy; int ox, oy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; moved=false; return false; }
                if (e.getAction() == MotionEvent.ACTION_MOVE) { int dx=(int)(e.getRawX()-sx), dy=(int)(e.getRawY()-sy); if (Math.abs(dx)+Math.abs(dy)>12) moved=true; if(moved){bubbleParams.x=Math.max(0,ox-dx); bubbleParams.y=Math.max(0,oy+dy); wm.updateViewLayout(bubble,bubbleParams);} return moved; }
                if (e.getAction()==MotionEvent.ACTION_UP && moved) return true; return false;
            }});
        wm.addView(bubble, bubbleParams);
    }
    private void togglePanel() { if (open) { if(panel!=null){wm.removeView(panel); panel=null;} open=false; return; } showPanel(); }
    private void showPanel() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(14),dp(16),dp(14)); box.setBackground(shape(dark,dp(20)));
        TextView title = new TextView(this); title.setText("ARMIN DECK  /  QUICK TOOLS"); title.setTextColor(Color.WHITE); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setTextSize(14); box.addView(title);
        AudioManager audio = (AudioManager)getSystemService(AUDIO_SERVICE);
        TextView volLabel = label("Media volume"); box.addView(volLabel);
        SeekBar volume = new SeekBar(this); int max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC); volume.setMax(max); volume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){if(u)audio.setStreamVolume(AudioManager.STREAM_MUSIC,p,0);} public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}}); box.addView(volume);
        Button dnd = btn("TOGGLE DO NOT DISTURB"); dnd.setOnClickListener(v -> {
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 23 && !nm.isNotificationPolicyAccessGranted()) { Intent i=new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return; }
            if (Build.VERSION.SDK_INT >= 23) { int current=nm.getCurrentInterruptionFilter(); nm.setInterruptionFilter(current==NotificationManager.INTERRUPTION_FILTER_ALL ? NotificationManager.INTERRUPTION_FILTER_NONE : NotificationManager.INTERRUPTION_FILTER_ALL); dnd.setText(current==NotificationManager.INTERRUPTION_FILTER_ALL ? "DND: ON" : "DND: OFF"); }
        }); box.addView(dnd);
        TextView fps = label("FPS: Android doesn't expose another game's true FPS to ordinary apps. No fake FPS value is shown."); fps.setPadding(0,dp(10),0,dp(10)); box.addView(fps);
        Button close=btn("CLOSE PANEL"); close.setOnClickListener(v->togglePanel()); box.addView(close);
        panel=box; panelParams=new WindowManager.LayoutParams(dp(280),-2,type(),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        panelParams.gravity=Gravity.TOP|Gravity.END; panelParams.x=dp(12); panelParams.y=bubbleParams.y+dp(70);
        try { wm.addView(panel,panelParams); open=true; } catch(Exception e){panel=null; open=false;}
    }
    private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.LTGRAY);t.setTextSize(12);t.setPadding(0,dp(8),0,dp(3));return t;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(shape(purple,dp(12)));return b;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
    @Override public void onDestroy(){try{if(panel!=null)wm.removeView(panel);if(bubble!=null)wm.removeView(bubble);}catch(Exception ignored){} super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
