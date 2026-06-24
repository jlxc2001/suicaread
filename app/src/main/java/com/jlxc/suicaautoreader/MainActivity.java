package com.jlxc.suicaautoreader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
    private static final String PREFS = "suica_auto_reader_prefs";
    private static final String KEY_PAUSE_AFTER_READ = "pause_after_read";
    private static final String KEY_SHOW_DEBUG = "show_debug";

    private static final int BG = 0xFFF6F7F9;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT_MAIN = 0xFF101114;
    private static final int TEXT_SUB = 0xFF777A80;
    private static final int ACCENT = 0xFF06A9C8;
    private static final int ACCENT_DARK = 0xFF007E93;

    private NfcAdapter nfcAdapter;
    private SharedPreferences prefs;
    private final AtomicBoolean reading = new AtomicBoolean(false);

    private LinearLayout rootLayout;
    private TextView statusText;
    private TextView balanceText;
    private TextView updatedText;
    private LinearLayout historyList;
    private LinearLayout debugBox;
    private TextView debugText;

    private volatile Tag lastTag;
    private volatile SuicaReader.SuicaData lastData;
    private String currentLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaultPrefs();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        currentLang = L.lang(this);
        buildUi();
        handleIntent(getIntent(), true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String lang = L.lang(this);
        if (currentLang == null || !currentLang.equals(lang)) {
            currentLang = lang;
            buildUi();
        }
        startNfcReading(false);
        if (lastData != null) renderSuicaData(lastData);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopNfcReading();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        readTagAsync(tag, L.t(this, "status_detected"));
    }

    private void handleIntent(Intent intent, boolean fromSystemDispatch) {
        if (intent == null) return;
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String action = intent.getAction();
            String source = fromSystemDispatch
                    ? L.t(this, "status_from_chooser")
                    : L.t(this, "status_reading");
            readTagAsync(tag, source);
        }
    }

    private void readTagAsync(Tag tag, String message) {
        if (tag == null) return;
        lastTag = tag;
        if (!reading.compareAndSet(false, true)) return;
        runOnUiThread(() -> setStatus(message));
        new Thread(() -> {
            try {
                final SuicaReader.SuicaData data = SuicaReader.read(tag);
                lastData = data;
                runOnUiThread(() -> {
                    renderSuicaData(data);
                    if (prefs.getBoolean(KEY_PAUSE_AFTER_READ, true)) {
                        stopNfcReading();
                        setStatus(L.t(this, "status_done"));
                    } else {
                        setStatus(L.t(this, "status_done_wait"));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> setStatus(L.t(this, "status_read_failed_prefix") + e.getMessage() + L.t(this, "status_read_failed_suffix")));
            } finally {
                reading.set(false);
            }
        }, "suica-read-thread").start();
    }

    private void forceRefresh() {
        setStatus(L.t(this, "status_refreshing"));
        if (lastTag != null) {
            readTagAsync(lastTag, L.t(this, "status_refreshing_card"));
        }
        startNfcReading(true);
    }

    private void startNfcReading(boolean userRequested) {
        if (nfcAdapter == null) {
            setStatus(L.t(this, "status_no_nfc_hw"));
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            setStatus(L.t(this, "status_nfc_off"));
            if (userRequested) openNfcSettings();
            return;
        }

        Bundle extras = new Bundle();
        extras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        int flags = NfcAdapter.FLAG_READER_NFC_F
                | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;
        try {
            nfcAdapter.enableReaderMode(this, this, flags, extras);
            if (userRequested) {
                setStatus(L.t(this, "status_reader_mode"));
            } else if (lastData == null) {
                setStatus(L.t(this, "status_wait_nfc"));
            }
        } catch (Exception e) {
            setStatus(L.t(this, "status_reader_failed") + e.getMessage());
        }
    }

    private void stopNfcReading() {
        if (nfcAdapter == null) return;
        try {
            nfcAdapter.disableReaderMode(this);
        } catch (Exception ignored) {
        }
    }

    private void openNfcSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void exitApp() {
        stopNfcReading();
        Toast.makeText(this, L.t(this, "toast_exit"), Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();
        else finish();
    }

    private void ensureDefaultPrefs() {
        if (!prefs.contains(KEY_PAUSE_AFTER_READ)) {
            prefs.edit().putBoolean(KEY_PAUSE_AFTER_READ, true).apply();
        }
        if (!prefs.contains(KEY_SHOW_DEBUG)) {
            prefs.edit().putBoolean(KEY_SHOW_DEBUG, false).apply();
        }
    }

    private void buildUi() {
        int pad = dp(14);
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(pad, dp(8) + getStatusBarHeight(), pad, 0);
        rootLayout.setBackgroundColor(BG);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, dp(8));

        TextView menu = new TextView(this);
        menu.setText("⋮");
        menu.setTextSize(25);
        menu.setGravity(Gravity.CENTER);
        menu.setTextColor(TEXT_MAIN);
        menu.setBackground(makeRoundBg(0x00FFFFFF, dp(18)));
        menu.setOnClickListener(v -> showDrawer(menu));
        topBar.addView(menu, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView title = new TextView(this);
        title.setText(L.t(this, "app_short_title"));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT_MAIN);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.setMargins(dp(8), 0, 0, 0);
        topBar.addView(title, titleLp);

        TextView nfcHint = new TextView(this);
        nfcHint.setText("NFC");
        nfcHint.setTextSize(12);
        nfcHint.setGravity(Gravity.CENTER);
        nfcHint.setTextColor(ACCENT_DARK);
        nfcHint.setTypeface(Typeface.DEFAULT_BOLD);
        nfcHint.setBackground(makeRoundBg(0xFFE9FBFE, dp(16)));
        topBar.addView(nfcHint, new LinearLayout.LayoutParams(dp(54), dp(30)));
        rootLayout.addView(topBar);

        LinearLayout balanceCard = new LinearLayout(this);
        balanceCard.setOrientation(LinearLayout.VERTICAL);
        balanceCard.setPadding(dp(18), dp(16), dp(18), dp(14));
        balanceCard.setBackground(makeRoundBg(CARD, dp(18)));

        TextView balanceLabel = new TextView(this);
        balanceLabel.setText(L.t(this, "balance"));
        balanceLabel.setTextSize(17);
        balanceLabel.setTextColor(TEXT_MAIN);
        balanceCard.addView(balanceLabel);

        balanceText = new TextView(this);
        balanceText.setText(L.t(this, "waiting_read"));
        balanceText.setTextSize(34);
        balanceText.setTypeface(Typeface.DEFAULT_BOLD);
        balanceText.setTextColor(TEXT_MAIN);
        balanceText.setPadding(0, dp(4), 0, dp(4));
        balanceCard.addView(balanceText);

        updatedText = new TextView(this);
        updatedText.setText(L.t(this, "scan_suica"));
        updatedText.setTextSize(16);
        updatedText.setTypeface(Typeface.DEFAULT_BOLD);
        updatedText.setTextColor(ACCENT_DARK);
        balanceCard.addView(updatedText);

        LinearLayout.LayoutParams balanceLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        balanceLp.setMargins(0, 0, 0, dp(12));
        rootLayout.addView(balanceCard, balanceLp);

        statusText = new TextView(this);
        statusText.setText(L.t(this, "initializing"));
        statusText.setTextSize(13);
        statusText.setTextColor(TEXT_SUB);
        statusText.setPadding(dp(2), 0, dp(2), dp(10));
        rootLayout.addView(statusText);

        debugBox = new LinearLayout(this);
        debugBox.setOrientation(LinearLayout.VERTICAL);
        debugBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        debugBox.setBackground(makeRoundBg(0xFFEFF2F5, dp(12)));
        debugText = new TextView(this);
        debugText.setTextSize(12);
        debugText.setTextColor(0xFF555A60);
        debugBox.addView(debugText);
        rootLayout.addView(debugBox, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        debugBox.setVisibility(View.GONE);

        ScrollView scroll = new ScrollView(this);
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(historyList);
        rootLayout.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(rootLayout);
        renderEmptyState();
    }

    private void showDrawer(View anchor) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(makeRoundBg(CARD, dp(18)));

        TextView header = new TextView(this);
        header.setText(L.t(this, "drawer_title"));
        header.setTextSize(18);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(TEXT_MAIN);
        header.setPadding(dp(6), dp(4), dp(6), dp(8));
        panel.addView(header);

        panel.addView(drawerItem(L.t(this, "refresh"), L.t(this, "refresh_desc"), () -> forceRefresh()));
        panel.addView(drawerItem(L.t(this, "open_nfc_settings"), L.t(this, "open_nfc_settings_desc"), () -> openNfcSettings()));
        panel.addView(drawerItem(L.t(this, "settings"), L.t(this, "settings_desc"), () -> startActivity(new Intent(this, SettingsActivity.class))));
        panel.addView(drawerItem(L.t(this, "exit"), L.t(this, "exit_desc"), () -> exitApp()));

        final PopupWindow popup = new PopupWindow(panel, dp(286), LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= 21) popup.setElevation(dp(10));

        // 让点击菜单项后自动收起
        int childCount = panel.getChildCount();
        for (int i = 1; i < childCount; i++) {
            final View item = panel.getChildAt(i);
            final View.OnClickListener original = getStoredClick(item);
            if (original != null) {
                item.setOnClickListener(v -> {
                    popup.dismiss();
                    original.onClick(v);
                });
            }
        }
        popup.showAtLocation(rootLayout, Gravity.TOP | Gravity.START, dp(10), getStatusBarHeight() + dp(48));
    }

    private View.OnClickListener getStoredClick(View item) {
        Object tag = item.getTag();
        if (tag instanceof View.OnClickListener) return (View.OnClickListener) tag;
        return null;
    }

    private LinearLayout drawerItem(String title, String desc, final Runnable action) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(makeRoundBg(0xFFF8F9FA, dp(12)));
        box.setTag((View.OnClickListener) v -> action.run());
        box.setOnClickListener((View.OnClickListener) box.getTag());

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(TEXT_MAIN);
        box.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(12);
        d.setTextColor(TEXT_SUB);
        d.setPadding(0, dp(2), 0, 0);
        box.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        box.setLayoutParams(lp);
        return box;
    }

    private void renderEmptyState() {
        historyList.removeAllViews();
        TextView empty = new TextView(this);
        empty.setText(L.t(this, "empty_state"));
        empty.setTextSize(15);
        empty.setTextColor(TEXT_SUB);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(18), dp(42), dp(18), dp(42));
        historyList.addView(empty, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void renderSuicaData(SuicaReader.SuicaData data) {
        NumberFormat yen = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        yen.setMaximumFractionDigits(0);
        balanceText.setText(normalizeYen(yen.format(data.balanceYen)));
        updatedText.setText(L.f(this, "updated_fmt", data.records.size()));
        historyList.removeAllViews();

        boolean showDebug = prefs.getBoolean(KEY_SHOW_DEBUG, false);
        if (showDebug) {
            debugBox.setVisibility(View.VISIBLE);
            debugText.setText(L.f(this, "debug_fmt", data.idmHex, data.records.size()));
        } else {
            debugBox.setVisibility(View.GONE);
        }

        for (int i = 0; i < data.records.size(); i++) {
            SuicaReader.HistoryRecord r = data.records.get(i);
            Integer change = null;
            if (i + 1 < data.records.size()) {
                change = r.balanceYen - data.records.get(i + 1).balanceYen;
            }
            historyList.addView(makeHistoryRow(r, change, yen, showDebug), rowLp());
        }
    }

    private LinearLayout makeHistoryRow(SuicaReader.HistoryRecord r, Integer change, NumberFormat yen, boolean showDebug) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(makeRoundBg(CARD, dp(14)));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText(iconText(r, change));
        icon.setTextSize(24);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(Color.WHITE);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setBackground(makeRoundBg(ACCENT, dp(12)));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        top.addView(icon, iconLp);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(dp(12), 0, dp(8), 0);

        TextView title = new TextView(this);
        title.setText(friendlyTitle(r, change));
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(TEXT_MAIN);
        left.addView(title);

        TextView date = new TextView(this);
        date.setText(shortDate(r.dateText));
        date.setTextSize(15);
        date.setTextColor(TEXT_MAIN);
        date.setPadding(0, dp(2), 0, 0);
        left.addView(date);
        top.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView place = new TextView(this);
        place.setText(friendlyPlace(r, change));
        place.setTextSize(14);
        place.setTextColor(TEXT_MAIN);
        place.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(place, new LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView amount = new TextView(this);
        amount.setText(amountText(r, change, yen));
        amount.setTextSize(22);
        amount.setTypeface(Typeface.DEFAULT_BOLD);
        amount.setTextColor(TEXT_MAIN);
        amount.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        top.addView(amount, new LinearLayout.LayoutParams(dp(118), LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(28);
        arrow.setTextColor(0xFFB4B7BB);
        arrow.setGravity(Gravity.CENTER);
        top.addView(arrow, new LinearLayout.LayoutParams(dp(20), LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(top);

        if (showDebug) {
            TextView debug = new TextView(this);
            debug.setText(L.f(this, "debug_line_fmt", normalizeYen(yen.format(r.balanceYen)), r.machineText, r.processText, r.areaLineStationText, r.sequence, r.rawHex));
            debug.setTextSize(11);
            debug.setTextColor(TEXT_SUB);
            debug.setPadding(dp(64), dp(8), 0, 0);
            row.addView(debug);
        }
        return row;
    }

    private LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        return lp;
    }

    private String friendlyTitle(SuicaReader.HistoryRecord r, Integer change) {
        if (change != null && change > 0) return L.t(this, "charge_amount");
        if (r.processCode == 0x02 || r.processCode == 0x14 || r.machineCode == 0x09 || r.machineCode == 0x1F) return L.t(this, "charge_amount");
        if (r.machineCode == 0x07 || r.processCode == 0x03) return L.t(this, "ticket_purchase");
        if (r.machineCode == 0x05 || r.machineCode == 0x08 || r.processCode == 0x01 || r.processCode == 0x0d || r.processCode == 0x0f) return L.t(this, "public_transport");
        if (r.machineCode == 0xC7 || r.machineCode == 0xC8 || r.processCode == 0x46) return L.t(this, "purchase");
        if (change != null && change < 0) return L.t(this, "purchase");
        return L.t(this, "record");
    }

    private String friendlyPlace(SuicaReader.HistoryRecord r, Integer change) {
        if (change != null && change > 0) return L.t(this, "charge");
        if (r.processCode == 0x02 || r.processCode == 0x14 || r.machineCode == 0x09 || r.machineCode == 0x1F) return L.t(this, "charge");
        if (r.machineCode == 0x07) return L.t(this, "station");
        if (r.machineCode == 0x05 || r.machineCode == 0x08 || r.processCode == 0x01 || r.processCode == 0x0d || r.processCode == 0x0f) return L.t(this, "transport");
        if (r.machineCode == 0xC7 || r.machineCode == 0xC8 || r.processCode == 0x46) return L.t(this, "wallet");
        return "";
    }

    private String iconText(SuicaReader.HistoryRecord r, Integer change) {
        if (change != null && change > 0) return "+";
        if (r.processCode == 0x02 || r.processCode == 0x14 || r.machineCode == 0x09 || r.machineCode == 0x1F) return "+";
        if (r.machineCode == 0x07 || r.processCode == 0x03) return "券";
        if (r.machineCode == 0x05 || r.machineCode == 0x08 || r.processCode == 0x01 || r.processCode == 0x0d || r.processCode == 0x0f) return "電";
        return "買";
    }

    private String amountText(SuicaReader.HistoryRecord r, Integer change, NumberFormat yen) {
        if (change == null) return normalizeYen(yen.format(r.balanceYen));
        String s = normalizeYen(yen.format(Math.abs(change)));
        if (change > 0) return "+" + s;
        if (change < 0) return s;
        return s;
    }

    private String shortDate(String full) {
        if (full != null && full.length() >= 10 && full.charAt(4) == '/') {
            return full.substring(5);
        }
        return full == null ? "" : full;
    }

    private String normalizeYen(String s) {
        if (s == null) return "";
        return s.replace('￥', '¥');
    }

    private void setStatus(String text) {
        if (statusText != null) statusText.setText(text);
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
