package com.armin.deck;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(12, 15, 17);
    private final int PANEL = Color.rgb(25, 30, 32);
    private final int GREEN = Color.rgb(128, 255, 75);
    private final int MUTED = Color.rgb(155, 167, 166);
    private LinearLayout root, grid;
    private EditText search;
    private TextView count;
    private final List<AppEntry> allApps = new ArrayList<>();
    private final List<View> cards = new ArrayList<>();
    private int selected = 0;

    private static class AppEntry {
        String label, packageName;
        android.graphics.drawable.Drawable icon;
        AppEntry(String l, String p, android.graphics.drawable.Drawable i) {
            label=l; packageName=p; icon=i;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        loadApps();
        renderApps("");
    }

    private GradientDrawable shape(int color, int strokeColor, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif" : "sans-serif-light",
                bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(14), dp(22), dp(12));
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView brand = text("ARMIN", 24, Color.WHITE, true);
        TextView deck = text(" DECK", 24, GREEN, true);
        top.addView(brand); top.addView(deck);
        TextView mode = text("  /  GAME LIBRARY", 12, MUTED, true);
        mode.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(mode, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = text("● READY", 12, GREEN, true);
        top.addView(live);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(38)));

        TextView heading = text("YOUR GAMES. YOUR DECK.", 18, Color.WHITE, true);
        heading.setPadding(0, dp(10), 0, dp(3));
        root.addView(heading);
        TextView sub = text("Select an app to launch  •  Controller / keyboard supported", 12, MUTED, false);
        root.addView(sub);

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, dp(12), 0, dp(10));
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(MUTED);
        search.setHint("Search installed apps...");
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(shape(PANEL, Color.rgb(49,59,58), dp(10)));
        tools.addView(search, new LinearLayout.LayoutParams(0, dp(42), 1));
        count = text("  LOADING", 11, GREEN, true);
        tools.addView(count);
        root.addView(tools);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { renderApps(s.toString()); }
            public void afterTextChanged(android.text.Editable e) {}
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, dp(2), 0, dp(8));
        scroll.addView(grid);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView footer = text("ARMIN DECK  •  ANDROID GAME HUB                                  A / ENTER: OPEN     •     ESC: BACK", 10, MUTED, true);
        footer.setPadding(0, dp(8), 0, 0);
        root.addView(footer);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> results = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : results) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            try {
                allApps.add(new AppEntry(
                    ri.loadLabel(pm).toString(), pkg, ri.loadIcon(pm)));
            } catch (Exception ignored) {}
        }
        Collections.sort(allApps, new Comparator<AppEntry>() {
            public int compare(AppEntry a, AppEntry b) {
                return a.label.toLowerCase(Locale.ROOT).compareTo(b.label.toLowerCase(Locale.ROOT));
            }
        });
    }

    private void renderApps(String query) {
        if (grid == null) return;
        grid.removeAllViews(); cards.clear(); selected = 0;
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<AppEntry> shown = new ArrayList<>();
        for (AppEntry app : allApps) {
            if (app.label.toLowerCase(Locale.ROOT).contains(q) ||
                app.packageName.toLowerCase(Locale.ROOT).contains(q)) shown.add(app);
        }
        count.setText(String.format(Locale.ROOT, "%02d APPS  ", shown.size()));
        if (shown.isEmpty()) {
            TextView empty = text(allApps.isEmpty() ? "No launchable apps found." : "No apps match your search.", 15, MUTED, false);
            empty.setPadding(dp(10), dp(25), dp(10), dp(25));
            grid.addView(empty);
            return;
        }
        int columns = getResources().getDisplayMetrics().widthPixels >= dp(900) ? 5 : 4;
        LinearLayout row = null;
        for (int i=0; i<shown.size(); i++) {
            if (i % columns == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.TOP);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            final AppEntry app = shown.get(i);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setFocusableInTouchMode(false);
            card.setPadding(dp(8), dp(10), dp(8), dp(8));
            card.setBackground(shape(PANEL, 0, dp(12)));
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(42), dp(42));
            ip.bottomMargin = dp(7);
            card.addView(icon, ip);
            TextView label = text(app.label, 11, Color.WHITE, true);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            card.addView(label, new LinearLayout.LayoutParams(-1, dp(32)));
            card.setOnClickListener(v -> launch(app));
            card.setOnFocusChangeListener((v, focused) -> {
                v.setBackground(shape(focused ? Color.rgb(35,52,34) : PANEL,
                    focused ? GREEN : 0, dp(12)));
            });
            card.setOnKeyListener((v, key, event) -> {
                if (event.getAction() == KeyEvent.ACTION_UP &&
                    (key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_DPAD_CENTER ||
                     key == KeyEvent.KEYCODE_BUTTON_A || key == KeyEvent.KEYCODE_SPACE)) {
                    launch(app); return true;
                }
                return false;
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(104), 1);
            cp.setMargins(dp(3), dp(3), dp(3), dp(5));
            row.addView(card, cp);
            cards.add(card);
        }
        int rem = shown.size() % columns;
        if (rem != 0 && row != null) {
            for (int i=rem; i<columns; i++) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        }
        if (!cards.isEmpty()) {
            cards.get(0).setBackground(shape(Color.rgb(35,52,34), GREEN, dp(12)));
            cards.get(0).setFocusable(true);
        }
    }

    private void launch(AppEntry app) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (i != null) startActivity(i);
            else Toast.makeText(this, "This app cannot be launched.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not open " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && !cards.isEmpty()) {
            int key = event.getKeyCode();
            if (key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_DPAD_LEFT ||
                key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                View current = getCurrentFocus();
                int idx = cards.indexOf(current);
                if (idx < 0) idx = selected;
                int columns = getResources().getDisplayMetrics().widthPixels >= dp(900) ? 5 : 4;
                if (key == KeyEvent.KEYCODE_DPAD_RIGHT) idx = Math.min(cards.size()-1, idx+1);
                if (key == KeyEvent.KEYCODE_DPAD_LEFT) idx = Math.max(0, idx-1);
                if (key == KeyEvent.KEYCODE_DPAD_DOWN) idx = Math.min(cards.size()-1, idx+columns);
                if (key == KeyEvent.KEYCODE_DPAD_UP) idx = Math.max(0, idx-columns);
                selected = idx; cards.get(idx).requestFocus(); return true;
            }
        }
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            if (search != null && search.hasFocus()) {
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(search.getWindowToken(), 0);
                search.clearFocus(); return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
