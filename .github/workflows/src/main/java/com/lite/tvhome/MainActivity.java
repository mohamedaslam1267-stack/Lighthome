package com.lite.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private LinearLayout row;
    private float d;

    private int dp(int v) { return (int) (v * d + 0.5f); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;

        FrameLayout root = new FrameLayout(this);
        // Static dark gradient wallpaper (no images, no animation)
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF2C2C2E, 0xFF000000}));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(56), dp(40), dp(56), dp(40));

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm a");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(40);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setGravity(Gravity.END);
        page.addView(clock, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer = new View(this);
        page.addView(spacer, new LinearLayout.LayoutParams(0, 0, 1f));

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setClipChildren(false);
        hsv.setClipToPadding(false);
        hsv.setSmoothScrollingEnabled(false);
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false);
        hsv.addView(row);
        page.addView(hsv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View spacer2 = new View(this);
        page.addView(spacer2, new LinearLayout.LayoutParams(0, 0, 1f));

        root.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadApps();
    }

    private void loadApps() {
        row.removeAllViews();
        PackageManager pm = getPackageManager();
        List<ResolveInfo> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        seen.add(getPackageName());

        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String c : cats) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(c);
            for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
                if (seen.add(ri.activityInfo.packageName)) all.add(ri);
            }
        }
        final PackageManager fpm = pm;
        Collections.sort(all, (a, b2) -> a.loadLabel(fpm).toString()
                .compareToIgnoreCase(b2.loadLabel(fpm).toString()));

        View first = null;
        for (ResolveInfo ri : all) {
            final Intent launch = new Intent(Intent.ACTION_MAIN)
                    .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final String pkg = ri.activityInfo.packageName;
            View t = makeTile(ri.loadIcon(pm), ri.loadLabel(pm).toString(), launch, pkg);
            row.addView(t);
            if (first == null) first = t;
        }
        // Settings shortcut
        row.addView(makeTile(null, "Settings",
                new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null));
        if (first != null) first.requestFocus();
    }

    private View makeTile(android.graphics.drawable.Drawable icon, String label,
                          final Intent launch, final String pkg) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(dp(10), dp(14), dp(10), dp(10));
        tile.setFocusable(true);
        tile.setClickable(true);

        ImageView iv = new ImageView(this);
        if (icon != null) iv.setImageDrawable(icon);
        else iv.setBackground(round(0xFF3A3A3C));
        tile.addView(iv, new LinearLayout.LayoutParams(dp(88), dp(88)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setSingleLine(true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(8), 0, 0);
        tile.addView(tv, new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(16), dp(10), dp(16));
        tile.setLayoutParams(lp);

        final GradientDrawable focused = round(0x33FFFFFF);
        tile.setOnFocusChangeListener((v, has) -> {
            // instant, no animation: light highlight + small scale
            v.setBackground(has ? focused : null);
            v.setScaleX(has ? 1.1f : 1f);
            v.setScaleY(has ? 1.1f : 1f);
        });
        tile.setOnClickListener(v -> {
            try { startActivity(launch); } catch (Exception ignored) { }
        });
        if (pkg != null) {
            tile.setOnLongClickListener(v -> {
                // long-press OK = app info (clear cache / force stop)
                try {
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception ignored) { }
                return true;
            });
        }
        return tile;
    }

    private GradientDrawable round(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(18));
        return g;
    }

    @Override
    public void onBackPressed() { /* stay on home */ }
}
