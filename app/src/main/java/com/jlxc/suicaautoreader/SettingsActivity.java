package com.jlxc.suicaautoreader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
    }

    private void buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad + getStatusBarHeight(), pad, pad);
        root.setBackgroundColor(0xFFF7F8FA);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("设置");
        title.setTextSize(24);
        title.setGravity(Gravity.START);
        title.setTextColor(0xFF111111);
        root.addView(title);

        root.addView(makeSwitch(
                "读取成功后暂停前台扫描",
                "推荐开启。你的 Suica 固定在手机背面，如果一直用前台 Reader Mode 反复扫描，可能反复触发读取。暂停后，下次重新扫卡并在系统 App 选择器里选择本软件时，仍会自动读取。",
                KEY_PAUSE_AFTER_READ,
                true
        ));

        TextView note = new TextView(this);
        note.setText("当前版本使用“系统 NFC App 选择器模式”：\n\n1. 先在系统里手动开启 NFC。\n2. 手机扫到背后的 Suica 后，系统会弹出 App 选择器。\n3. 选择 Suica Auto Reader。\n4. 本软件会接收系统传入的 NFC-F / FeliCa 标签，并重新读取余额和最近履历。\n\n说明：普通第三方 App 不能直接打开或关闭系统 NFC 开关，所以本版本不再尝试自动开关 NFC，也不会弹无障碍控制。\n\n本 App 只读取交通系 IC 卡公开的 SF 余额和最近履历，不写卡、不充值、不修改卡片内容。站名数据库未内置，所以目前显示原始线路/站点代码。 ");
        note.setTextSize(14);
        note.setTextColor(0xFF444444);
        note.setPadding(0, dp(18), 0, dp(18));
        root.addView(note);

        Button nfcSettingsButton = new Button(this);
        nfcSettingsButton.setText("打开系统 NFC 设置");
        nfcSettingsButton.setAllCaps(false);
        nfcSettingsButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        root.addView(nfcSettingsButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        Button back = new Button(this);
        back.setText("返回");
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());
        root.addView(back, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        setContentView(scroll);
    }

    private LinearLayout makeSwitch(String title, String desc, String key, boolean defValue) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackgroundColor(0xFFFFFFFF);

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(17);
        sw.setTextColor(0xFF111111);
        sw.setChecked(prefs.getBoolean(key, defValue));
        sw.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                prefs.edit().putBoolean(key, isChecked).apply());
        box.addView(sw);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(13);
        d.setTextColor(0xFF666666);
        d.setPadding(0, dp(6), 0, 0);
        box.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(12), 0, 0);
        box.setLayoutParams(lp);
        return box;
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
