# Suica Auto Reader

一个极简 Android NFC-F / FeliCa 读取 Demo，用于读取实体 Suica / 交通系 IC 卡的公开余额和最近 20 条 SF 使用履历。

## 功能

- 打开 App 后自动进入 NFC Reader Mode。
- 扫到 Suica 后自动读取余额和最近 20 条公开履历。
- 右上角三个点进入设置界面。
- 三个点下方有“关闭 NFC 并退出软件”按钮。
- 默认读取成功后暂停扫描，避免背贴卡一直反复触发。
- 可选 root/系统权限模式：尝试执行 `su -c 'svc nfc enable/disable'` 自动开关 NFC。

## 重要限制

普通第三方 Android App 不能直接打开或关闭系统 NFC 开关，只能检查 NFC 状态并跳转到系统 NFC 设置页。要真正自动开关 NFC，需要 root、系统签名、设备所有者/定制 ROM 等更高权限方案。

## 构建

### GitHub Actions

把本项目上传到 GitHub 后，进入 Actions，手动运行 `Android Build`。构建产物会在 Artifacts 里生成 debug APK。

### 本地

```bash
gradle assembleDebug
```

## 说明

本 App 只读卡，不写卡，不充值，不修改卡片内容。当前版本没有内置完整日本站名数据库，因此履历中显示的是原始线路/站点代码和 raw hex。
