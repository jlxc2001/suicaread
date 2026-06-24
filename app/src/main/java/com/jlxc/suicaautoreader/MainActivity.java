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
import android.view.View;
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
    private static final String KEY_AUTO_OPEN_NFC_SETTINGS = "auto_open_nfc_settings";
    private static final String KEY_ROOT_NFC_TOGGLE = "root_nfc_toggle";
    private static final String KEY_PAUSE_AFTER_READ = "pause_after_read";

    private NfcAdapter nfcAdapter;
    private SharedPreferences prefs;
    private final AtomicBoolean reading = new AtomicBoolean(false);
    private boolean openedNfcSettingsThisResume = false;

    private TextView statusText;
    private TextView balanceText;
    private TextView idmText;
    private LinearLayout historyList;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaultPrefs();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        buildUi();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openedNfcSettingsThisResume = false;
        startNfcReading();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopNfcReading();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (!reading.compareAndSet(false, true)) return;
        runOnUiThread(() -> setStatus("已扫到卡片，正在读取 Suica 余额和履历…"));
        try {
            final SuicaReader.SuicaData data = SuicaReader.read(tag);
            runOnUiThread(() -> {
                renderSuicaData(data);
                if (prefs.getBoolean(KEY_PAUSE_AFTER_READ, true)) {
                    stopNfcReading();
                    setStatus("读取完成。因为卡片贴在手机背面，已暂停扫描，避免反复触发。需要更新时点“重新读取”。");
                } else {
                    setStatus("读取完成，继续等待下一次 NFC 扫描。");
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> setStatus("读取失败：" + e.getMessage()));
        } finally {
            reading.set(false);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            onTagDiscovered(tag);
        }
    }

    private void startNfcReading() {
        if (nfcAdapter == null) {
            setStatus("这台设备没有 NFC 硬件，无法读取 Suica。 ");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            if (prefs.getBoolean(KEY_ROOT_NFC_TOGGLE, false)) {
                boolean ok = RootNfc.tryToggle("enable");
                if (ok) {
                    setStatus("已尝试通过 root 开启 NFC，正在等待系统刷新状态…");
                    try { Thread.sleep(700); } catch (InterruptedException ignored) {}
                }
            }

            if (!nfcAdapter.isEnabled()) {
                setStatus("NFC 当前关闭。普通第三方 App 不能直接开启 NFC，只能跳转到系统 NFC 设置页。打开 NFC 后回到本 App 会自动读取。 ");
                if (prefs.getBoolean(KEY_AUTO_OPEN_NFC_SETTINGS, true) && !openedNfcSettingsThisResume) {
                    openedNfcSettingsThisResume = true;
                    openNfcSettings();
                }
                return;
            }
        }

        Bundle extras = new Bundle();
        extras.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
        int flags = NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
        try {
            nfcAdapter.enableReaderMode(this, this, flags, extras);
            setStatus("等待读取：请保持背后的 Suica 贴近手机 NFC 感应区。你的场景里卡片已经固定在背面，通常打开 App 后会自动扫到。 ");
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

    private void closeNfcAndExit() {
        stopNfcReading();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            if (prefs.getBoolean(KEY_ROOT_NFC_TOGGLE, false)) {
                boolean ok = RootNfc.tryToggle("disable");
                Toast.makeText(this, ok ? "已尝试通过 root 关闭 NFC" : "root 关闭 NFC 失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "普通 App 不能直接关闭 NFC，已打开系统 NFC 设置页", Toast.LENGTH_LONG).show();
                openNfcSettings();
            }
        }
        finishAndRemoveTaskCompat();
    }

    private void finishAndRemoveTaskCompat() {
        if (Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();
        else finish();
    }

    private void ensureDefaultPrefs() {
        if (!prefs.contains(KEY_AUTO_OPEN_NFC_SETTINGS)) {
            prefs.edit()
                    .putBoolean(KEY_AUTO_OPEN_NFC_SETTINGS, true)
                    .putBoolean(KEY_ROOT_NFC_TOGGLE, false)
                    .putBoolean(KEY_PAUSE_AFTER_READ, true)
                    .apply();
        }
    }

    private void buildUi() {
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, dp(10), pad, pad);
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
        exitButton.setText("关闭 NFC 并退出软件");
        exitButton.setAllCaps(false);
        exitButton.setOnClickListener(v -> closeNfcAndExit());
        root.addView(exitButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        refreshButton = new Button(this);
        refreshButton.setText("重新读取");
        refreshButton.setAllCaps(false);
        refreshButton.setOnClickListener(v -> startNfcReading());
        root.addView(refreshButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

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
        statusText.setText(text);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
