package fr.maisontv.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** A local, static family home screen. No network, tracking, services or web view. */
public final class HomeActivity extends Activity {
    private final String[] packages = {"com.netflix.ninja", "com.google.android.youtube.tv", "com.canal.android.canal", "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus"};
    private final String[] labels = {"Netflix", "YouTube", "CANAL+", "Prime Video", "Disney+"};
    private final int ivory = Color.rgb(255, 229, 195);
    private float scale;
    private FrameLayout root;
    private View firstTile;
    private Dialog appsDialog;
    private TextView date;
    private final Handler handler = new Handler();
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (date != null) date.setText(new SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(new java.util.Date()));
            handler.postDelayed(this, 60000L - System.currentTimeMillis() % 60000L);
        }
    };

    private int px(float value) { return Math.round(value * scale); }
    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setFontFeatureSettings("kern");
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        return t;
    }
    private GradientDrawable shape(boolean focus) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(focus ? 0xF02B3F45 : 0xC0142931);
        d.setCornerRadius(px(20));
        d.setStroke(px(focus ? 3 : 1), focus ? ivory : 0x406D858A);
        return d;
    }
    private void focusable(final View v) {
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setBackground(shape(false));
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean focused) {
                view.setBackground(shape(focused));
                view.animate().cancel();
                view.animate().scaleX(focused ? 1.045f : 1f).scaleY(focused ? 1.045f : 1f).setDuration(110).start();
                view.setElevation(focused ? px(12) : 0);
            }
        });
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemBars();
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 24, 34));
        setContentView(root);
        ImageView wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try (InputStream stream = getAssets().open("maison.png")) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap image = BitmapFactory.decodeStream(stream, null, options);
            wallpaper.setImageBitmap(image);
        } catch (Exception ignored) { /* The dark background remains usable. */ }
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        makeToolbar();
        makeClock();
        makeDock();
        firstTile.requestFocus();
    }
    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private void makeToolbar() {
        LinearLayout bar = new LinearLayout(this);
        FrameLayout.LayoutParams where = new FrameLayout.LayoutParams(-2, px(58), Gravity.TOP | Gravity.LEFT);
        where.leftMargin = px(78); where.topMargin = px(66);
        root.addView(bar, where);
        toolbarButton(bar, "Applications", new View.OnClickListener() { public void onClick(View v) { showApps(); }});
        toolbarButton(bar, "Réglages", new View.OnClickListener() { public void onClick(View v) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception e) { unavailable(); }
        }});
        toolbarButton(bar, "Télévision", new View.OnClickListener() { public void onClick(View v) { launch("com.mediatek.wwtv.tvcenter"); }});
    }
    private void toolbarButton(LinearLayout parent, String label, View.OnClickListener action) {
        TextView button = text(label, 23, ivory);
        button.setPadding(px(24), 0, px(24), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -1);
        lp.rightMargin = px(16);
        parent.addView(button, lp);
        focusable(button);
        button.setOnClickListener(action);
    }
    private void makeClock() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.RIGHT);
        FrameLayout.LayoutParams where = new FrameLayout.LayoutParams(px(400), px(170), Gravity.TOP | Gravity.RIGHT);
        where.rightMargin = px(82); where.topMargin = px(58);
        root.addView(column, where);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("HH:mm");
        clock.setTextColor(ivory);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(58));
        clock.setGravity(Gravity.RIGHT);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        column.addView(clock, new LinearLayout.LayoutParams(-1, px(75)));
        date = text("", 23, 0xDDEBD9BD); date.setGravity(Gravity.RIGHT);
        column.addView(date, new LinearLayout.LayoutParams(-1, px(36)));
        TextView cast = text("Chromecast intégré", 18, 0xB3DCE7DB); cast.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams castPosition = new LinearLayout.LayoutParams(-1, px(30));
        castPosition.topMargin = px(13); column.addView(cast, castPosition);
    }
    private void makeDock() {
        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false); dock.setClipToPadding(false);
        FrameLayout.LayoutParams where = new FrameLayout.LayoutParams(-2, px(214), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        where.bottomMargin = px(82);
        root.addView(dock, where);
        for (int i = 0; i < packages.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setClipChildren(false); item.setClipToPadding(false);
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(px(272), -1);
            slot.leftMargin = px(13); slot.rightMargin = px(13);
            dock.addView(item, slot);
            View tile = appTile(packages[i], labels[i], px(266), px(150));
            item.addView(tile, new LinearLayout.LayoutParams(px(266), px(150)));
            TextView caption = text(labels[i], 25, ivory);
            LinearLayout.LayoutParams labelPosition = new LinearLayout.LayoutParams(-1, px(42)); labelPosition.topMargin = px(15);
            item.addView(caption, labelPosition);
            if (i == 0) firstTile = tile;
        }
        TextView hint = text("À chacun son programme, ensemble à la maison.", 20, 0x99E7DCCB);
        FrameLayout.LayoutParams hintPosition = new FrameLayout.LayoutParams(-1, px(30), Gravity.BOTTOM);
        hintPosition.bottomMargin = px(38); root.addView(hint, hintPosition);
    }
    private View appTile(final String pkg, String label, int width, int height) {
        FrameLayout tile = new FrameLayout(this);
        tile.setContentDescription(label);
        tile.setPadding(px(5), px(5), px(5), px(5));
        Drawable art = null;
        try {
            art = getPackageManager().getApplicationBanner(pkg);
            if (art == null) art = getPackageManager().getApplicationIcon(pkg);
        } catch (Exception ignored) { }
        if (art != null) {
            ImageView logo = new ImageView(this); logo.setImageDrawable(art); logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(logo, new FrameLayout.LayoutParams(-1, -1));
        } else {
            tile.addView(text(label, 30, ivory), new FrameLayout.LayoutParams(-1, -1));
        }
        focusable(tile);
        tile.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { launch(pkg); }});
        return tile;
    }
    private void launch(String pkg) {
        try {
            Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) { unavailable(); return; }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) { unavailable(); }
    }
    private void unavailable() { Toast.makeText(this, "Cette application n'est pas disponible.", Toast.LENGTH_SHORT).show(); }
    private void showApps() {
        if (appsDialog != null && appsDialog.isShowing()) return;
        PackageManager pm = getPackageManager();
        List<ResolveInfo> found = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        found.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));
        Collections.sort(found, new ResolveInfo.DisplayNameComparator(pm));
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(px(48), px(30), px(48), px(30)); panel.setBackgroundColor(0xFF102731);
        TextView title = text("Toutes les applications", 34, ivory); panel.addView(title, new LinearLayout.LayoutParams(-1, px(75)));
        ScrollView scroll = new ScrollView(this); scroll.setClipToPadding(false); scroll.setPadding(px(16), px(16), px(16), px(16));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        GridLayout grid = new GridLayout(this); grid.setColumnCount(4); scroll.addView(grid);
        HashSet<String> seen = new HashSet<>(); View first = null;
        for (ResolveInfo info : found) {
            final String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || pkg.equals("com.spocky.projengmenu") || pkg.equals("com.mstar.android.tv.disclaimercustomization") || pkg.equals("com.mstar.netflixobserver") || !seen.add(pkg) || !info.activityInfo.enabled || !info.activityInfo.applicationInfo.enabled) continue;
            LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams cell = new GridLayout.LayoutParams(); cell.width = px(250); cell.height = px(195); cell.setMargins(px(16), px(12), px(16), px(12)); grid.addView(item, cell);
            String label = info.loadLabel(pm).toString(); View tile = appTile(pkg, label, px(240), px(135));
            item.addView(tile, new LinearLayout.LayoutParams(px(240), px(135)));
            item.addView(text(label, 20, ivory), new LinearLayout.LayoutParams(-1, px(48)));
            if (first == null) first = tile;
        }
        appsDialog = new Dialog(this); appsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE); appsDialog.setContentView(panel);
        appsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { public void onDismiss(DialogInterface dialog) { appsDialog = null; }});
        Window w = appsDialog.getWindow(); if (w != null) { w.setBackgroundDrawableResource(android.R.color.transparent); w.setDimAmount(0.65f); w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); }
        appsDialog.show(); if (w != null) w.setLayout(px(1300), px(850));
        if (first != null) first.requestFocus();
    }
    @Override protected void onResume() { super.onResume(); hideSystemBars(); handler.removeCallbacks(tick); tick.run(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); if (appsDialog != null) appsDialog.dismiss(); }
    @Override public void onBackPressed() { if (appsDialog != null && appsDialog.isShowing()) appsDialog.dismiss(); else firstTile.requestFocus(); }
    @Override public boolean onKeyDown(int key, KeyEvent event) {
        if (key == KeyEvent.KEYCODE_MENU) { showApps(); return true; }
        return super.onKeyDown(key, event);
    }
}
