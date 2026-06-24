package com.jlxc.suicaautoreader;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class L {
    public static final String PREFS = "suica_auto_reader_prefs";
    public static final String KEY_LANGUAGE = "language";

    private L() {}

    public static String lang(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String pref = prefs.getString(KEY_LANGUAGE, "system");
        if (pref != null && !"system".equals(pref)) return pref;
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        if ("ja".equals(language)) return "ja";
        if ("ko".equals(language)) return "ko";
        if ("en".equals(language)) return "en";
        if ("zh".equals(language)) {
            String country = locale.getCountry();
            String script = locale.getScript();
            if ("Hant".equalsIgnoreCase(script) || "TW".equalsIgnoreCase(country)
                    || "HK".equalsIgnoreCase(country) || "MO".equalsIgnoreCase(country)) {
                return "zh-TW";
            }
            return "zh-CN";
        }
        return "en";
    }

    public static String languageName(Context c) {
        String pref = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, "system");
        if ("en".equals(pref)) return "English";
        if ("ja".equals(pref)) return "日本語";
        if ("ko".equals(pref)) return "한국어";
        if ("zh-TW".equals(pref)) return "繁體中文";
        if ("zh-CN".equals(pref)) return "简体中文";
        return t(c, "language_system");
    }

    public static String t(Context c, String key) {
        String l = lang(c);
        switch (key) {
            case "language_system": return pick(l, "跟随系统", "System", "システムに合わせる", "시스템 언어", "跟隨系統");
            case "language_title": return pick(l, "语言", "Language", "言語", "언어", "語言");
            case "status_detected": return pick(l, "已检测到 Suica，正在读取…", "Suica detected. Reading…", "Suicaを検出しました。読み取り中…", "Suica가 감지되었습니다. 읽는 중…", "已偵測到 Suica，正在讀取…");
            case "status_from_chooser": return pick(l, "从系统 NFC 选择器进入，正在读取卡片…", "Opened from the NFC app chooser. Reading card…", "NFCアプリ選択画面から起動しました。カードを読み取り中…", "NFC 앱 선택기에서 실행되었습니다. 카드를 읽는 중…", "從系統 NFC 選擇器進入，正在讀取卡片…");
            case "status_reading": return pick(l, "正在读取卡片…", "Reading card…", "カードを読み取り中…", "카드를 읽는 중…", "正在讀取卡片…");
            case "status_done": return pick(l, "读取完成", "Read complete", "読み取り完了", "읽기 완료", "讀取完成");
            case "status_done_wait": return pick(l, "读取完成，继续等待下一次扫卡", "Read complete. Waiting for the next card scan.", "読み取り完了。次のスキャンを待機中。", "읽기 완료. 다음 스캔을 기다리는 중입니다.", "讀取完成，繼續等待下一次掃卡");
            case "status_refreshing": return pick(l, "正在重新读取…如果卡片一直贴在手机背面，这次会优先复用上一次系统传入的标签；失败时请稍微移开再贴回。", "Refreshing… If the card stays on the back of the phone, the last NFC tag will be reused first. If it fails, move the card away and tap again.", "再読み取り中…カードを背面に付けたままの場合、前回のNFCタグを優先して再利用します。失敗したら少し離して再度かざしてください。", "다시 읽는 중… 카드가 휴대폰 뒷면에 계속 붙어 있으면 마지막 NFC 태그를 먼저 재사용합니다. 실패하면 카드를 살짝 떼었다가 다시 대 주세요.", "正在重新讀取…如果卡片一直貼在手機背面，會優先復用上一次系統傳入的標籤；失敗時請稍微移開再貼回。");
            case "status_refreshing_card": return pick(l, "正在重新读取卡内数据…", "Refreshing card data…", "カード情報を再読み取り中…", "카드 데이터를 다시 읽는 중…", "正在重新讀取卡內資料…");
            case "status_no_nfc_hw": return pick(l, "这台设备没有 NFC 硬件，无法读取 Suica。", "This device has no NFC hardware, so it cannot read Suica.", "この端末にはNFCがないため、Suicaを読み取れません。", "이 기기에는 NFC 하드웨어가 없어 Suica를 읽을 수 없습니다.", "這台裝置沒有 NFC 硬體，無法讀取 Suica。");
            case "status_nfc_off": return pick(l, "NFC 当前关闭。请先手动打开 NFC，然后扫到卡片时在系统 App 选择器里选择本软件。", "NFC is off. Turn on NFC manually, then choose this app from the system chooser when the card is detected.", "NFCがオフです。手動でNFCをオンにして、カード検出時にシステムのアプリ選択画面でこのアプリを選んでください。", "NFC가 꺼져 있습니다. NFC를 수동으로 켠 뒤 카드가 감지되면 시스템 앱 선택기에서 이 앱을 선택하세요.", "NFC 目前關閉。請先手動開啟 NFC，掃到卡片時在系統 App 選擇器中選擇本軟體。");
            case "status_reader_mode": return pick(l, "已进入重新读取模式。请保持 Suica 靠近 NFC 区域；如果没有反应，轻微移开再贴回。", "Refresh mode is active. Keep Suica near the NFC area. If nothing happens, move it away slightly and tap again.", "再読み取りモードです。SuicaをNFCエリアに近づけてください。反応しない場合は少し離して再度かざしてください。", "다시 읽기 모드입니다. Suica를 NFC 영역 가까이에 두세요. 반응이 없으면 살짝 떼었다가 다시 대 주세요.", "已進入重新讀取模式。請保持 Suica 靠近 NFC 區域；如果沒有反應，請輕微移開再貼回。");
            case "status_wait_nfc": return pick(l, "等待 NFC：也可以从系统弹出的 App 选择器里选择本软件。", "Waiting for NFC. You can also choose this app from the system NFC chooser.", "NFC待機中：システムのアプリ選択画面からこのアプリを選ぶこともできます。", "NFC 대기 중: 시스템 NFC 앱 선택기에서 이 앱을 선택할 수도 있습니다.", "等待 NFC：也可以從系統彈出的 App 選擇器中選擇本軟體。");
            case "status_reader_failed": return pick(l, "启动 NFC 读取模式失败：", "Failed to start NFC reader mode: ", "NFC読み取りモードの起動に失敗：", "NFC 읽기 모드 시작 실패: ", "啟動 NFC 讀取模式失敗：");
            case "status_read_failed_prefix": return pick(l, "读取失败：", "Read failed: ", "読み取り失敗：", "읽기 실패: ", "讀取失敗：");
            case "status_read_failed_suffix": return pick(l, "。请点左上角菜单里的“重新读取”，或把卡片稍微移开再贴回。", ". Tap Refresh in the top-left menu, or move the card away and tap again.", "。左上メニューの「再読み取り」を押すか、カードを少し離して再度かざしてください。", ". 왼쪽 위 메뉴의 '다시 읽기'를 누르거나 카드를 살짝 떼었다가 다시 대 주세요.", "。請點左上角選單裡的「重新讀取」，或把卡片稍微移開再貼回。");
            case "toast_exit": return pick(l, "已停止本软件读取，不关闭系统 NFC", "Stopped this app's reader. System NFC remains on.", "このアプリの読み取りを停止しました。システムNFCはオフにしません。", "이 앱의 읽기를 중지했습니다. 시스템 NFC는 꺼지지 않습니다.", "已停止本軟體讀取，不關閉系統 NFC");
            case "app_short_title": return "Suica";
            case "balance": return pick(l, "余额", "Balance", "残高", "잔액", "餘額");
            case "waiting_read": return pick(l, "等待读取", "Waiting", "読み取り待ち", "읽기 대기", "等待讀取");
            case "scan_suica": return pick(l, "请扫 Suica 卡片", "Scan your Suica card", "Suicaをかざしてください", "Suica 카드를 스캔하세요", "請掃 Suica 卡片");
            case "initializing": return pick(l, "正在初始化 NFC…", "Initializing NFC…", "NFCを初期化中…", "NFC 초기화 중…", "正在初始化 NFC…");
            case "drawer_title": return pick(l, "功能", "Actions", "操作", "기능", "功能");
            case "refresh": return pick(l, "重新读取", "Refresh", "再読み取り", "다시 읽기", "重新讀取");
            case "refresh_desc": return pick(l, "重新连接当前卡片并重新刷新余额/履历", "Reconnect the current card and refresh balance/history", "現在のカードに再接続して残高/履歴を更新", "현재 카드에 다시 연결해 잔액/내역 새로고침", "重新連接目前卡片並刷新餘額/履歷");
            case "open_nfc_settings": return pick(l, "打开系统 NFC 设置", "Open system NFC settings", "システムNFC設定を開く", "시스템 NFC 설정 열기", "開啟系統 NFC 設定");
            case "open_nfc_settings_desc": return pick(l, "手动开启/关闭系统 NFC", "Turn system NFC on/off manually", "システムNFCを手動でオン/オフ", "시스템 NFC를 수동으로 켜기/끄기", "手動開啟/關閉系統 NFC");
            case "settings": return pick(l, "设置", "Settings", "設定", "설정", "設定");
            case "settings_desc": return pick(l, "语言、显示调试信息、读取策略等", "Language, debug info, reading behavior", "言語、デバッグ情報、読み取り設定など", "언어, 디버그 정보, 읽기 방식 등", "語言、顯示除錯資訊、讀取策略等");
            case "exit": return pick(l, "停止读取并退出", "Stop reading and exit", "読み取りを停止して終了", "읽기 중지 후 종료", "停止讀取並退出");
            case "exit_desc": return pick(l, "只停止本 App 的读取，不关闭系统 NFC", "Stops only this app's reader; does not turn off NFC", "このアプリの読み取りのみ停止し、NFCはオフにしません", "이 앱의 읽기만 중지하며 시스템 NFC는 끄지 않습니다", "只停止本 App 的讀取，不關閉系統 NFC");
            case "empty_state": return pick(l,
                    "暂无记录\n\n打开系统 NFC 后，把 Suica 靠近手机 NFC 区域；系统弹出 App 选择器时选择 Suica Auto Reader。读取成功后，这里只显示余额和可读懂的使用记录。",
                    "No records yet\n\nTurn on system NFC and place Suica near the phone's NFC area. When the system app chooser appears, choose Suica Auto Reader. After a successful read, only the balance and readable history are shown here.",
                    "履歴はまだありません\n\nシステムNFCをオンにして、Suicaを端末のNFCエリアに近づけてください。アプリ選択画面が出たらSuica Auto Readerを選択します。読み取り成功後、残高と分かりやすい履歴だけを表示します。",
                    "아직 내역이 없습니다\n\n시스템 NFC를 켠 뒤 Suica를 휴대폰 NFC 영역에 가까이 대세요. 시스템 앱 선택기가 뜨면 Suica Auto Reader를 선택하세요. 읽기 성공 후 잔액과 이해하기 쉬운 내역만 표시됩니다.",
                    "暫無記錄\n\n開啟系統 NFC 後，把 Suica 靠近手機 NFC 區域；系統彈出 App 選擇器時選擇 Suica Auto Reader。讀取成功後，這裡只顯示餘額和容易理解的使用記錄。");
            case "updated_fmt": return pick(l, "↻ 今天 · %d 条记录", "↻ Today · %d records", "↻ 今日 · %d件の履歴", "↻ 오늘 · %d개 내역", "↻ 今天 · %d 筆記錄");
            case "debug_fmt": return "IDm: %s    System: 0x0003    Records: %d";
            case "debug_line_fmt": return pick(l,
                    "余额 %s · %s / %s · %s · Seq %d\nRaw: %s",
                    "Balance %s · %s / %s · %s · Seq %d\nRaw: %s",
                    "残高 %s · %s / %s · %s · Seq %d\nRaw: %s",
                    "잔액 %s · %s / %s · %s · Seq %d\nRaw: %s",
                    "餘額 %s · %s / %s · %s · Seq %d\nRaw: %s");
            case "charge_amount": return pick(l, "充值金额", "Top-up", "チャージ金額", "충전 금액", "儲值金額");
            case "ticket_purchase": return pick(l, "券购买", "Ticket", "券購入", "승차권 구매", "票券購買");
            case "public_transport": return pick(l, "公共交通", "Transit", "公共交通", "대중교통", "公共交通");
            case "purchase": return pick(l, "消费", "Purchase", "利用", "소비", "消費");
            case "record": return pick(l, "使用记录", "History", "利用履歴", "사용 기록", "使用記錄");
            case "charge": return pick(l, "充值", "Top-up", "チャージ", "충전", "儲值");
            case "station": return pick(l, "车站", "Station", "駅", "역", "車站");
            case "transport": return pick(l, "交通", "Transit", "交通", "교통", "交通");
            case "wallet": return pick(l, "钱包", "Wallet", "電子マネー", "전자지갑", "錢包");
            case "settings_title": return pick(l, "设置", "Settings", "設定", "설정", "設定");
            case "pause_after_read": return pick(l, "读取成功后暂停前台扫描", "Pause foreground scanning after reading", "読み取り成功後に前面スキャンを一時停止", "읽기 성공 후 전면 스캔 일시정지", "讀取成功後暫停前台掃描");
            case "pause_after_read_desc": return pick(l,
                    "推荐开启。你的 Suica 固定在手机背面，读取完成后暂停本软件自己的连续扫描，避免反复刷新；下次从系统 NFC App 选择器进入时仍会自动读。",
                    "Recommended. Since your Suica is fixed to the back of the phone, this pauses this app's continuous foreground scan after reading to avoid repeated refreshes. It will still read automatically when opened from the system NFC chooser next time.",
                    "推奨。Suicaを端末背面に固定している場合、読み取り後にこのアプリの連続スキャンを一時停止し、繰り返し更新を防ぎます。次回システムNFC選択画面から起動した場合は自動で読み取ります。",
                    "권장. Suica가 휴대폰 뒷면에 고정되어 있으므로 읽기 후 이 앱의 연속 전면 스캔을 멈춰 반복 새로고침을 방지합니다. 다음에 시스템 NFC 선택기에서 실행하면 자동으로 읽습니다.",
                    "建議開啟。你的 Suica 固定在手機背面，讀取完成後會暫停本軟體自己的連續掃描，避免反覆刷新；下次從系統 NFC App 選擇器進入時仍會自動讀取。");
            case "show_debug": return pick(l, "显示调试信息", "Show debug info", "デバッグ情報を表示", "디버그 정보 표시", "顯示除錯資訊");
            case "show_debug_desc": return pick(l,
                    "默认关闭。开启后主界面会显示 IDm、原始站点代码、Raw 数据，方便后续排查，但平时会显得很专业。",
                    "Off by default. When enabled, the main screen shows IDm, raw station codes and raw data for troubleshooting, but it looks technical for daily use.",
                    "初期設定はオフ。オンにするとIDm、駅コード、Rawデータを表示して調査しやすくなりますが、普段は専門的に見えます。",
                    "기본값은 꺼짐입니다. 켜면 메인 화면에 IDm, 원시 역 코드, Raw 데이터가 표시되어 문제 확인에 도움이 되지만 평소에는 전문적으로 보일 수 있습니다.",
                    "預設關閉。開啟後主畫面會顯示 IDm、原始站點代碼、Raw 資料，方便後續排查，但平時會顯得較專業。");
            case "usage_note": return pick(l,
                    "使用方式\n\n1. 手动打开系统 NFC。\n2. 手机扫到背后的 Suica 后，在系统弹出的 App 选择器里选择 Suica Auto Reader。\n3. 本软件会读取余额和最近履历。\n\n说明\n\n普通第三方 App 不能直接打开或关闭系统 NFC 总开关，所以本版本采用系统选择器 + 前台读取模式。主界面默认隐藏 IDm、Raw、线路/站点代码，只显示更容易理解的余额、充值、消费、公共交通等记录。\n\n站名显示需要内置日本交通系 IC 卡站名数据库，目前先隐藏原始代码；后续可以单独加站名库。",
                    "How to use\n\n1. Turn on system NFC manually.\n2. When the phone detects the Suica on the back, choose Suica Auto Reader from the system app chooser.\n3. This app reads the balance and recent history.\n\nNotes\n\nNormal third-party apps cannot directly turn system NFC on or off, so this version uses the system chooser plus foreground reader mode. By default, the main screen hides IDm, raw data, and line/station codes, and only shows readable balance, top-up, purchase and transit records.\n\nStation names require a Japanese transit IC station database. Raw codes are hidden for now; a station database can be added later.",
                    "使い方\n\n1. システムNFCを手動でオンにします。\n2. 端末背面のSuicaを検出したら、システムのアプリ選択画面でSuica Auto Readerを選びます。\n3. 残高と最近の履歴を読み取ります。\n\n説明\n\n通常のサードパーティアプリはシステムNFCのオン/オフを直接変更できないため、このバージョンはシステム選択画面＋前面読み取りモードを使用します。メイン画面ではIDm、Rawデータ、路線/駅コードを初期状態で非表示にし、残高、チャージ、利用、公共交通など分かりやすい記録だけを表示します。\n\n駅名表示には日本の交通系ICカード駅名データベースが必要です。現時点では原始コードを非表示にし、後で駅名データベースを追加できます。",
                    "사용 방법\n\n1. 시스템 NFC를 수동으로 켭니다.\n2. 휴대폰이 뒷면의 Suica를 감지하면 시스템 앱 선택기에서 Suica Auto Reader를 선택합니다.\n3. 앱이 잔액과 최근 내역을 읽습니다.\n\n설명\n\n일반 서드파티 앱은 시스템 NFC 전체 스위치를 직접 켜거나 끌 수 없으므로, 이 버전은 시스템 선택기 + 전면 읽기 모드를 사용합니다. 메인 화면은 기본적으로 IDm, Raw 데이터, 노선/역 코드를 숨기고 잔액, 충전, 소비, 대중교통 등 이해하기 쉬운 기록만 표시합니다.\n\n역명 표시에는 일본 교통계 IC 카드 역명 데이터베이스가 필요합니다. 현재는 원시 코드를 숨기고, 나중에 역명 DB를 별도로 추가할 수 있습니다.",
                    "使用方式\n\n1. 手動開啟系統 NFC。\n2. 手機掃到背後的 Suica 後，在系統彈出的 App 選擇器中選擇 Suica Auto Reader。\n3. 本軟體會讀取餘額和最近履歷。\n\n說明\n\n普通第三方 App 不能直接開啟或關閉系統 NFC 總開關，所以本版本採用系統選擇器 + 前台讀取模式。主畫面預設隱藏 IDm、Raw、路線/站點代碼，只顯示更容易理解的餘額、儲值、消費、公共交通等記錄。\n\n站名顯示需要內建日本交通系 IC 卡站名資料庫，目前先隱藏原始代碼；後續可以單獨加站名庫。");
            case "back_main": return pick(l, "返回主界面", "Back to main screen", "メイン画面に戻る", "메인 화면으로 돌아가기", "返回主畫面");
            default: return key;
        }
    }

    public static String f(Context c, String key, Object... args) {
        return String.format(Locale.getDefault(), t(c, key), args);
    }

    private static String pick(String lang, String zh, String en, String ja, String ko, String zht) {
        if ("ja".equals(lang)) return ja;
        if ("ko".equals(lang)) return ko;
        if ("zh-TW".equals(lang)) return zht;
        if ("zh-CN".equals(lang)) return zh;
        return en;
    }
}
