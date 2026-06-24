package com.jlxc.suicaautoreader;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {
    private static final String PREFS = "suica_auto_reader_prefs";
    private static final String KEY_AUTO_OPEN_NFC_SETTINGS = "auto_open_nfc_settings";
    private static final String KEY_ROOT_NFC_TOGGLE = "root_nfc_toggle";
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
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFFF7F8FA);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("设置");
        title.setTextSize(24);
        title.setGravity(Gravity.START);
        title.setTextColor(0xFF111111);
        root.addView(title);

        root.addView(makeSwitch(
                "NFC 关闭时自动打开系统 NFC 设置页",
                "普通第三方 App 无法直接开启 NFC。开启这个选项后，启动 App 发现 NFC 关闭时会自动跳到系统 NFC 设置。",
                KEY_AUTO_OPEN_NFC_SETTINGS,
                true
        ));

        root.addView(makeSwitch(
                "Root/系统权限模式：尝试自动开关 NFC",
                "开启后会尝试执行 su -c 'svc nfc enable/disable'。仅适合 root 或系统权限设备；失败不会影响正常读卡。",
                KEY_ROOT_NFC_TOGGLE,
                false
        ));

        root.addView(makeSwitch(
                "读取成功后暂停扫描",
                "推荐开启。你的 Suica 固定在手机背面，如果一直扫描，可能反复触发读取或耗电。",
                KEY_PAUSE_AFTER_READ,
                true
        ));

        TextView note = new TextView(this);
        note.setText("说明：本 App 只读取交通系 IC 卡公开的 SF 余额和最近履历，不写卡、不充值、不修改卡片内容。站名数据库未内置，所以目前显示原始线路/站点代码。 ");
        note.setTextSize(14);
        note.setTextColor(0xFF444444);
        note.setPadding(0, dp(18), 0, dp(18));
        root.addView(note);

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
}
