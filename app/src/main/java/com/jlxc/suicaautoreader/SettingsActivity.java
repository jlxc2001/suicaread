package com.jlxc.suicaautoreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final String PREFS = "suica_auto_reader_prefs";
    private static final String KEY_PAUSE_AFTER_READ = "pause_after_read";
    private static final String KEY_SHOW_DEBUG = "show_debug";
    private static final String KEY_LANGUAGE = "language";

    private static final int BG = 0xFFF6F7F9;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT_MAIN = 0xFF101114;
    private static final int TEXT_SUB = 0xFF777A80;
    private static final int ACCENT = 0xFF06A9C8;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
    }

    private void buildUi() {
        int pad = dp(14);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(8) + getStatusBarHeight(), pad, pad);
        root.setBackgroundColor(BG);
        scroll.addView(root);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(12));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34);
        back.setGravity(Gravity.CENTER);
        back.setTextColor(TEXT_MAIN);
        back.setOnClickListener(v -> finish());
        topBar.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = new TextView(this);
        title.setText(L.t(this, "settings_title"));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT_MAIN);
        topBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(topBar);

        root.addView(makeLanguageCard());

        root.addView(makeSwitch(
                L.t(this, "pause_after_read"),
                L.t(this, "pause_after_read_desc"),
                KEY_PAUSE_AFTER_READ,
                true
        ));

        root.addView(makeSwitch(
                L.t(this, "show_debug"),
                L.t(this, "show_debug_desc"),
                KEY_SHOW_DEBUG,
                false
        ));

        TextView note = new TextView(this);
        note.setText(L.t(this, "usage_note"));
        note.setTextSize(14);
        note.setTextColor(TEXT_SUB);
        note.setPadding(dp(14), dp(14), dp(14), dp(14));
        note.setBackground(makeRoundBg(CARD, dp(16)));
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        noteLp.setMargins(0, dp(12), 0, dp(12));
        root.addView(note, noteLp);

        Button nfcSettingsButton = new Button(this);
        nfcSettingsButton.setText(L.t(this, "open_nfc_settings"));
        nfcSettingsButton.setTextColor(TEXT_MAIN);
        nfcSettingsButton.setAllCaps(false);
        nfcSettingsButton.setOnClickListener(v -> openNfcSettings());
        root.addView(nfcSettingsButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        Button close = new Button(this);
        close.setText(L.t(this, "back_main"));
        close.setTextColor(TEXT_MAIN);
        close.setAllCaps(false);
        close.setOnClickListener(v -> finish());
        root.addView(close, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        setContentView(scroll);
    }

    private LinearLayout makeLanguageCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(makeRoundBg(CARD, dp(16)));
        box.setOnClickListener(v -> showLanguageDialog());

        TextView t = new TextView(this);
        t.setText(L.t(this, "language_title"));
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT_MAIN);
        box.addView(t);

        TextView d = new TextView(this);
        d.setText(L.languageName(this));
        d.setTextSize(14);
        d.setTextColor(ACCENT);
        d.setPadding(0, dp(6), 0, 0);
        box.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);
        return box;
    }

    private void showLanguageDialog() {
        final String[] labels = new String[]{
                L.t(this, "language_system"),
                "简体中文",
                "繁體中文",
                "English",
                "日本語",
                "한국어"
        };
        final String[] values = new String[]{"system", "zh-CN", "zh-TW", "en", "ja", "ko"};
        String current = prefs.getString(KEY_LANGUAGE, "system");
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(L.t(this, "language_title"))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    prefs.edit().putString(KEY_LANGUAGE, values[which]).apply();
                    dialog.dismiss();
                    buildUi();
                })
                .show();
    }

    private void openNfcSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private LinearLayout makeSwitch(String title, String desc, String key, boolean defValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackground(makeRoundBg(CARD, dp(16)));

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(16);
        sw.setTypeface(Typeface.DEFAULT_BOLD);
        sw.setTextColor(TEXT_MAIN);
        sw.setChecked(prefs.getBoolean(key, defValue));
        sw.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        box.addView(sw);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(13);
        d.setTextColor(TEXT_SUB);
        d.setPadding(0, dp(6), 0, 0);
        box.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);
        return box;
    }

    private GradientDrawable makeRoundBg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private int getStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) return getResources().getDimensionPixelSize(resId);
        return 0;
    }
}
