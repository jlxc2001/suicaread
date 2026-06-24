package com.jlxc.suicaautoreader;

import android.nfc.Tag;
import android.nfc.tech.NfcF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SuicaReader {
    private static final int SYSTEM_CODE_SUICA = 0x0003;
    private static final int SERVICE_CODE_HISTORY = 0x090F;
    private static final int MAX_HISTORY_BLOCKS = 20;

    // 关键修复：不要一次读取 20 条。
    // 20 条履历响应大约 333 bytes，很多手机/ROM/NFC 芯片会截断或直接返回异常短响应。
    // 10 条响应约 173 bytes，兼容性明显更好；第二批再读 10 条。
    private static final int HISTORY_BATCH_BLOCKS = 10;

    private SuicaReader() {}

    public static SuicaData read(Tag tag) throws Exception {
        NfcF nfcF = NfcF.get(tag);
        if (nfcF == null) throw new IOException("这不是 NFC-F / FeliCa 卡片");

        byte[] idm;
        try {
            nfcF.connect();
            nfcF.setTimeout(3000);

            idm = polling(nfcF, SYSTEM_CODE_SUICA);
            if (idm == null || idm.length != 8) {
                idm = tag.getId();
            }
            if (idm == null || idm.length != 8) {
                throw new IOException("无法取得 FeliCa IDm");
            }

            List<byte[]> blocks = readHistoryBlocks(nfcF, idm);
            return parseBlocks(idm, blocks);
        } finally {
            try { nfcF.close(); } catch (Exception ignored) {}
        }
    }

    /** FeliCa Polling command: [len, 0x00, systemCodeHi, systemCodeLo, requestCode, timeSlot] */
    private static byte[] polling(NfcF nfcF, int systemCode) {
        byte[] command = new byte[]{
                0x06,
                0x00,
                (byte) ((systemCode >> 8) & 0xff),
                (byte) (systemCode & 0xff),
                0x01,
                0x0f
        };
        try {
            byte[] response = transceiveWithRetry(nfcF, command, 10, 2);
            if (response != null && response.length >= 10 && (response[1] & 0xff) == 0x01) {
                return Arrays.copyOfRange(response, 2, 10);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<byte[]> readHistoryBlocks(NfcF nfcF, byte[] idm) throws IOException {
        List<byte[]> blocks = new ArrayList<>();
        IOException lastError = null;

        for (int start = 0; start < MAX_HISTORY_BLOCKS; start += HISTORY_BATCH_BLOCKS) {
            int count = Math.min(HISTORY_BATCH_BLOCKS, MAX_HISTORY_BLOCKS - start);
            try {
                byte[] response = readWithoutEncryption(nfcF, idm, SERVICE_CODE_HISTORY, start, count);
                List<byte[]> batch = parseReadResponseToBlocks(response);
                blocks.addAll(batch);
                tinyDelay();
            } catch (IOException e) {
                lastError = e;

                // 如果 10 条仍然失败，就退化成 1 条 1 条读取。
                // 这样即使部分手机 NFC 栈比较挑，也能尽量至少拿到余额和近期几条。
                for (int i = start; i < start + count; i++) {
                    try {
                        byte[] response = readWithoutEncryption(nfcF, idm, SERVICE_CODE_HISTORY, i, 1);
                        List<byte[]> single = parseReadResponseToBlocks(response);
                        blocks.addAll(single);
                        tinyDelay();
                    } catch (IOException singleError) {
                        lastError = singleError;
                        break;
                    }
                }
            }
        }

        if (blocks.isEmpty()) {
            if (lastError != null) throw lastError;
            throw new IOException("没有读取到 Suica 公开履历块");
        }
        return blocks;
    }

    private static void tinyDelay() {
        try { Thread.sleep(60); } catch (InterruptedException ignored) {}
    }

    private static byte[] readWithoutEncryption(NfcF nfcF, byte[] idm, int serviceCode, int startBlock, int blockCount) throws IOException {
        if (blockCount < 1 || blockCount > 12) throw new IllegalArgumentException("blockCount must be 1..12");
        if (startBlock < 0 || startBlock > 255) throw new IllegalArgumentException("startBlock must be 0..255");

        int length = 1 + 1 + 8 + 1 + 2 + 1 + blockCount * 2;
        byte[] command = new byte[length];
        int p = 0;
        command[p++] = (byte) length;
        command[p++] = 0x06; // Read Without Encryption
        System.arraycopy(idm, 0, command, p, 8);
        p += 8;
        command[p++] = 0x01; // number of services
        command[p++] = (byte) (serviceCode & 0xff); // little endian
        command[p++] = (byte) ((serviceCode >> 8) & 0xff);
        command[p++] = (byte) blockCount;
        for (int i = 0; i < blockCount; i++) {
            command[p++] = (byte) 0x80; // 2-byte block list element, access mode 0
            command[p++] = (byte) (startBlock + i);
        }
        return transceiveWithRetry(nfcF, command, 13, 3);
    }

    private static byte[] transceiveWithRetry(NfcF nfcF, byte[] command, int minResponseLength, int maxAttempts) throws IOException {
        IOException lastError = null;
        byte[] lastResponse = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                byte[] response = nfcF.transceive(command);
                lastResponse = response;
                if (response != null && response.length >= minResponseLength) {
                    return response;
                }
                lastError = new IOException("FeliCa 响应过短 len=" + (response == null ? 0 : response.length)
                        + " raw=" + hexShort(response));
            } catch (IOException e) {
                lastError = e;
            }
            try { Thread.sleep(120); } catch (InterruptedException ignored) {}
        }

        if (lastError != null) throw lastError;
        throw new IOException("FeliCa 无响应 raw=" + hexShort(lastResponse));
    }

    private static List<byte[]> parseReadResponseToBlocks(byte[] response) throws IOException {
        if (response == null || response.length < 13) {
            throw new IOException("FeliCa 响应过短 len=" + (response == null ? 0 : response.length) + " raw=" + hexShort(response));
        }
        if ((response[1] & 0xff) != 0x07) {
            throw new IOException("不是 Read Without Encryption 响应：" + hexShort(response));
        }

        int status1 = response[10] & 0xff;
        int status2 = response[11] & 0xff;
        if (status1 != 0 || status2 != 0) {
            throw new IOException(String.format(Locale.US, "Suica 公开履历读取被拒绝：status=%02X%02X raw=%s", status1, status2, hexShort(response)));
        }

        int blockCount = response[12] & 0xff;
        int available = Math.min(blockCount, (response.length - 13) / 16);
        if (available <= 0) throw new IOException("没有可解析的履历块 raw=" + hexShort(response));

        List<byte[]> blocks = new ArrayList<>();
        for (int i = 0; i < available; i++) {
            int start = 13 + i * 16;
            blocks.add(Arrays.copyOfRange(response, start, start + 16));
        }
        return blocks;
    }

    private static SuicaData parseBlocks(byte[] idm, List<byte[]> blocks) throws IOException {
        List<HistoryRecord> records = new ArrayList<>();
        for (byte[] block : blocks) {
            if (block == null || block.length != 16) continue;
            if (isAllZero(block)) continue;
            records.add(parseBlock(block));
        }
        if (records.isEmpty()) throw new IOException("卡片没有公开履历，可能是新卡或未产生 SF 使用记录");

        SuicaData data = new SuicaData();
        data.idmHex = hex(idm);
        data.balanceYen = records.get(0).balanceYen;
        data.records = records;
        return data;
    }

    private static HistoryRecord parseBlock(byte[] b) {
        HistoryRecord r = new HistoryRecord();
        int machine = b[0] & 0xff;
        int process = b[1] & 0xff;
        int year = 2000 + ((b[4] & 0xfe) >> 1);
        int month = ((b[4] & 0x01) << 3) | ((b[5] & 0xe0) >> 5);
        int day = b[5] & 0x1f;
        int balance = u16le(b[10], b[11]);
        int sequence = u16le(b[12], b[13]);

        r.machineCode = machine;
        r.processCode = process;
        r.machineText = machineText(machine);
        r.processText = processText(process);
        r.dateText = validDate(year, month, day) ? String.format(Locale.JAPAN, "%04d/%02d/%02d", year, month, day) : "日期未知";
        r.balanceYen = balance;
        r.sequence = sequence;
        r.areaLineStationText = String.format(Locale.US,
                "R%02X In[%02X-%02X] Out[%02X-%02X]",
                b[15] & 0xff, b[6] & 0xff, b[7] & 0xff, b[8] & 0xff, b[9] & 0xff);
        r.rawHex = hex(b);
        return r;
    }

    private static boolean validDate(int y, int m, int d) {
        return y >= 2000 && y <= 2099 && m >= 1 && m <= 12 && d >= 1 && d <= 31;
    }

    private static boolean isAllZero(byte[] data) {
        for (byte b : data) if (b != 0) return false;
        return true;
    }

    private static int u16le(byte lo, byte hi) {
        return (lo & 0xff) | ((hi & 0xff) << 8);
    }

    private static String machineText(int code) {
        switch (code) {
            case 0x03: return "精算机/窗口类";
            case 0x04: return "便携终端";
            case 0x05: return "车载/巴士类";
            case 0x07: return "自动售票机";
            case 0x08: return "自动改札机";
            case 0x09: return "入金机/充值机";
            case 0x12: return "自动贩卖机";
            case 0x14: return "站务终端";
            case 0xc7: return "物贩/电子钱包";
            case 0xc8: return "物贩/电子钱包";
            default: return String.format(Locale.US, "设备0x%02X", code);
        }
    }

    private static String processText(int code) {
        switch (code) {
            case 0x01: return "运费支出";
            case 0x02: return "充值";
            case 0x03: return "购票/购买";
            case 0x04: return "精算";
            case 0x05: return "入场";
            case 0x06: return "出场";
            case 0x0d: return "巴士/路面电车";
            case 0x0f: return "巴士";
            case 0x11: return "再发行/相关处理";
            case 0x13: return "新规/发行";
            case 0x14: return "自动充值";
            case 0x46: return "物贩";
            case 0x48: return "特典/积分相关";
            default: return String.format(Locale.US, "处理0x%02X", code);
        }
    }

    private static String hex(byte[] data) {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format(Locale.US, "%02X", b & 0xff));
        return sb.toString();
    }

    private static String hexShort(byte[] data) {
        if (data == null) return "null";
        int limit = Math.min(data.length, 64);
        byte[] part = Arrays.copyOfRange(data, 0, limit);
        return hex(part) + (data.length > limit ? "..." : "");
    }

    public static class SuicaData {
        public String idmHex;
        public int balanceYen;
        public List<HistoryRecord> records = new ArrayList<>();
    }

    public static class HistoryRecord {
        public int machineCode;
        public int processCode;
        public String machineText;
        public String processText;
        public String dateText;
        public int balanceYen;
        public int sequence;
        public String areaLineStationText;
        public String rawHex;
    }
}
