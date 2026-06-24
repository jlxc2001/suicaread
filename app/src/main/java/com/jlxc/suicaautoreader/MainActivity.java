package com.jlxc.suicaautoreader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
    private static final String PREFS = "suica_auto_reader_prefs";
    private static final String KEY_PAUSE_AFTER_READ = "pause_after_read";

    private NfcAdapter nfcAdapter;
    private SharedPreferences prefs;
    private final AtomicBoolean reading = new AtomicBoolean(false);

    private TextView statusText;
    private TextView balanceText;
    private TextView idmText;
    private LinearLayout historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaultPrefs();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
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
        startNfcReading(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopNfcReading();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        readTag(tag, "Reader Mode 已扫到卡片，正在读取 Suica 余额和履历…");
    }

    private void handleIntent(Intent intent, boolean fromSystemDispatch) {
        if (intent == null) return;
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String action = intent.getAction();
            String source = fromSystemDispatch
                    ? "由系统 NFC 选择器启动：" + (action == null ? "" : action) + "。正在重新读取卡内数据…"
                    : "正在读取卡内数据…";
            readTag(tag, source);
        }
    }

    private void readTag(Tag tag, String message) {
        if (tag == null) return;
        if (!reading.compareAndSet(false, true)) return;
        runOnUiThread(() -> setStatus(message));
        try {
            final SuicaReader.SuicaData data = SuicaReader.read(tag);
            runOnUiThread(() -> {
                renderSuicaData(data);
                if (prefs.getBoolean(KEY_PAUSE_AFTER_READ, true)) {
                    stopNfcReading();
                    setStatus("读取完成。已暂停前台连续扫描。下次可以直接重新贴近/重新扫卡，在系统弹出的 App 选择器里选择本软件；也可以点“重新读取”。");
                } else {
                    setStatus("读取完成，继续等待下一次 NFC 扫描。下次从系统 App 选择器打开本软件时，也会自动重新读取。 ");
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> setStatus("读取失败：" + e.getMessage() + "\n\n可以点“重新读取”，或退出后重新扫卡，在系统弹出的 App 选择器里再次选择本软件。"));
        } finally {
            reading.set(false);
        }
    }

    private void startNfcReading(boolean userRequested) {
        if (nfcAdapter == null) {
            setStatus("这台设备没有 NFC 硬件，无法读取 Suica。");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            setStatus("NFC 当前关闭。普通第三方 App 不能直接打开 NFC。\n\n本版本采用系统选择器模式：请先手动打开 NFC；之后扫到背后的 Suica 时，在系统弹出的 App 选择器里选择 Suica Auto Reader，本软件会收到系统传入的卡片对象并重新读取余额/履历。");
            if (userRequested) openNfcSettings();
            return;
        }

        Bundle extras = new Bundle();
        extras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        int flags = NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
        try {
            nfcAdapter.enableReaderMode(this, this, flags, extras);
            if (userRequested) {
                setStatus("已重新进入前台读取模式。请保持 Suica 靠近手机 NFC 感应区；如果没有反应，可以稍微移开再贴回。 ");
            } else {
                setStatus("等待 NFC：你也可以直接从系统 NFC App 选择器启动本软件，启动后会自动读取系统传入的 Suica 标签。 ");
            }
        } catch (Exception e) {
            setStatus("启动 NFC Reader Mode 失败：" + e.getMessage());
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
        Toast.makeText(this, "已退出，不关闭系统 NFC", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();
        else finish();
    }

    private void ensureDefaultPrefs() {
        if (!prefs.contains(KEY_PAUSE_AFTER_READ)) {
            prefs.edit().putBoolean(KEY_PAUSE_AFTER_READ, true).apply();
        }
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(10) + getStatusBarHeight(), pad, pad);
        root.setBackgroundColor(0xFFF7F8FA);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Suica Auto Reader");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF111111);
        topBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button menu = new Button(this);
        menu.setText("⋮");
        menu.setTextSize(22);
        menu.setAllCaps(false);
        menu.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        topBar.addView(menu, new LinearLayout.LayoutParams(dp(52), dp(48)));
        root.addView(topBar);

        Button exitButton = new Button(this);
        exitButton.setText("停止读取并退出");
        exitButton.setAllCaps(false);
        exitButton.setOnClickListener(v -> exitApp());
        root.addView(exitButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        Button refreshButton = new Button(this);
        refreshButton.setText("重新读取");
        refreshButton.setAllCaps(false);
        refreshButton.setOnClickListener(v -> startNfcReading(true));
        root.addView(refreshButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        Button nfcSettingsButton = new Button(this);
        nfcSettingsButton.setText("打开系统 NFC 设置");
        nfcSettingsButton.setAllCaps(false);
        nfcSettingsButton.setOnClickListener(v -> openNfcSettings());
        root.addView(nfcSettingsButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        balanceText = new TextView(this);
        balanceText.setText("余额：等待读取");
        balanceText.setTextSize(34);
        balanceText.setTypeface(Typeface.DEFAULT_BOLD);
        balanceText.setTextColor(0xFF0B6F68);
        balanceText.setPadding(0, dp(18), 0, dp(4));
        root.addView(balanceText);

        idmText = new TextView(this);
        idmText.setText("IDm：-");
        idmText.setTextSize(13);
        idmText.setTextColor(0xFF666666);
        root.addView(idmText);

        statusText = new TextView(this);
        statusText.setText("正在初始化 NFC…");
        statusText.setTextSize(14);
        statusText.setTextColor(0xFF333333);
        statusText.setPadding(0, dp(12), 0, dp(12));
        root.addView(statusText);

        ScrollView scroll = new ScrollView(this);
        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(historyList);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void renderSuicaData(SuicaReader.SuicaData data) {
        NumberFormat yen = NumberFormat.getCurrencyInstance(Locale.JAPAN);
        yen.setMaximumFractionDigits(0);
        balanceText.setText("余额：" + yen.format(data.balanceYen));
        idmText.setText("IDm：" + data.idmHex + "    System：0x0003    Records：" + data.records.size());
        historyList.removeAllViews();

        for (int i = 0; i < data.records.size(); i++) {
            SuicaReader.HistoryRecord r = data.records.get(i);
            Integer delta = null;
            if (i + 1 < data.records.size()) {
                delta = r.balanceYen - data.records.get(i + 1).balanceYen;
            }

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(12), dp(14), dp(12));
            card.setBackgroundColor(0xFFFFFFFF);

            TextView line1 = new TextView(this);
            line1.setText(String.format(Locale.JAPAN, "#%02d  %s    余额 %s", i + 1, r.dateText, yen.format(r.balanceYen)));
            line1.setTextSize(17);
            line1.setTypeface(Typeface.DEFAULT_BOLD);
            line1.setTextColor(0xFF111111);
            card.addView(line1);

            TextView line2 = new TextView(this);
            String deltaText = delta == null ? "未知" : (delta >= 0 ? "+" : "") + yen.format(delta).replace("￥", "¥");
            line2.setText("类型：" + r.machineText + " / " + r.processText + "    推算变化：" + deltaText);
            line2.setTextSize(14);
            line2.setTextColor(0xFF333333);
            card.addView(line2);

            TextView line3 = new TextView(this);
            line3.setText("入场/出场代码：" + r.areaLineStationText + "    Seq：" + r.sequence + "    Raw：" + r.rawHex);
            line3.setTextSize(12);
            line3.setTextColor(0xFF666666);
            card.addView(line3);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(10));
            historyList.addView(card, lp);
        }
    }

    private void setStatus(String text) {
        if (statusText != null) statusText.setText(text);
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
