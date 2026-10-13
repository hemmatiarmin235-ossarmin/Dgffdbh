package com.armin.deck;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 10, 16);
    private final int purple = Color.rgb(164, 92, 255);
    private LinearLayout list;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(bg);
        scroll.setFillViewport(true);
        scroll.addView(root);

        TextView logo = new TextView(this);
        logo.setText("ARMIN DECK"); logo.setTextSize(30); logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setTextColor(Color.WHITE); logo.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(logo, matchWrap());
        TextView subtitle = new TextView(this);
        subtitle.setText("GAME LAUNCHER  •  TOOLKIT"); subtitle.setTextColor(purple); subtitle.setTextSize(12);
        subtitle.setPadding(0, dp(4), 0, dp(20)); root.addView(subtitle);

        Button overlay = button("ENABLE FLOATING TOOL  ↗");
        overlay.setOnClickListener(v -> enableFloating()); root.addView(overlay, matchWrap());
        TextView note = new TextView(this);
        note.setText("The floating panel provides working media-volume control and Do Not Disturb access. Android does not let ordinary apps change another game's graphics settings or reliably read its true FPS.");
        note.setTextColor(Color.LTGRAY); note.setTextSize(13); note.setPadding(dp(2), dp(12), dp(2), dp(18)); root.addView(note);

        TextView heading = new TextView(this); heading.setText("YOUR INSTALLED GAMES"); heading.setTextColor(Color.WHITE);
        heading.setTextSize(16); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD); heading.setPadding(0, dp(4), 0, dp(12)); root.addView(heading);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list, matchWrap());
        setContentView(scroll);
        loadGames();
    }

    private void loadGames() {
        list.removeAllViews();
        Intent query = new Intent(Intent.ACTION_MAIN, null); query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        Collections.sort(apps, Comparator.comparing(a -> String.valueOf(a.loadLabel(pm)).toLowerCase()));
        int count = 0;
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String label = String.valueOf(info.loadLabel(pm));
            LinearLayout card = new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(14), dp(12), dp(10), dp(12));
            card.setBackground(round(Color.rgb(25,25,36), dp(16))); card.setOrientation(LinearLayout.HORIZONTAL);
            TextView icon = new TextView(this); icon.setText("▶"); icon.setTextColor(purple); icon.setTextSize(20); icon.setGravity(Gravity.CENTER);
            card.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView name = new TextView(this); name.setText(label); name.setTextColor(Color.WHITE); name.setTextSize(15); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1); np.leftMargin = dp(8); card.addView(name, np);
            Button play = button("PLAY"); play.setTextSize(12);
            play.setOnClickListener(v -> {
                Intent launch = pm.getLaunchIntentForPackage(pkg);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                    if (Settings.canDrawOverlays(this)) {
                        Intent svc = new Intent(this, FloatingService.class); svc.putExtra("game", label);
                        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc); else startService(svc);
                    }
                } else new AlertDialog.Builder(this).setMessage("Android didn't provide a launch action for this app.").setPositiveButton("OK", null).show();
            });
            card.addView(play); LinearLayout.LayoutParams cp = matchWrap(); cp.bottomMargin = dp(9); list.addView(card, cp); count++;
        }
        if (count == 0) { TextView empty = new TextView(this); empty.setText("No launchable apps found."); empty.setTextColor(Color.LTGRAY); list.addView(empty); }
    }

    private void enableFloating() {
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this).setTitle("Allow floating panel")
                .setMessage("Enable 'Display over other apps' for ARMIN DECK, then return here.")
                .setPositiveButton("OPEN SETTINGS", (d,w) -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))))
                .setNegativeButton("CANCEL", null).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (!nm.isNotificationPolicyAccessGranted()) {
                new AlertDialog.Builder(this).setTitle("Optional: Do Not Disturb")
                    .setMessage("To let the panel toggle Do Not Disturb, grant notification policy access. You can skip this and still use volume control.")
                    .setPositiveButton("OPEN SETTINGS", (d,w) -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
                    .setNegativeButton("SKIP", (d,w) -> startFloating()).show();
                return;
            }
        }
        startFloating();
    }
    private void startFloating() {
        Intent i = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        new AlertDialog.Builder(this).setMessage("Floating control enabled. Return to a game and tap the purple DECK bubble.").setPositiveButton("OK", null).show();
    }
    private Button button(String text) { Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackground(round(purple, dp(12))); b.setPadding(dp(14), dp(7), dp(14), dp(7)); return b; }
    private GradientDrawable round(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
}
