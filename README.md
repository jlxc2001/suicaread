# Suica Auto Reader

极简 Android NFC-F / FeliCa / Suica 余额与公开履历读取 Demo。

## v4 说明

本版本改为“系统 NFC App 选择器模式”：

1. 用户先在系统里手动开启 NFC。
2. 手机扫到 Suica / FeliCa 标签时，系统弹出 App 选择器。
3. 选择 `Suica Auto Reader`。
4. App 从系统 Intent 中取得 `NfcAdapter.EXTRA_TAG`，然后重新读取余额和最近 20 条公开履历。

普通第三方 App 不能直接打开或关闭系统 NFC 开关，所以 v4 不再尝试自动开关 NFC，也不再使用无障碍模拟点击 NFC 开关。

## 构建

仓库已包含 `.github/workflows/android.yml`。上传到 GitHub 后进入 Actions，运行 `Android Build`，构建产物在 Artifacts 中。

## 注意

- 只读取公开 SF 余额/履历，不写卡、不充值、不修改卡片。
- 站名数据库未内置，目前显示线路/站点原始代码。
- 如果选择 App 后提示读取失败，可以让卡片稍微离开 NFC 感应区再贴回，或点“重新读取”。
