package com.jlxc.suicaautoreader;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootNfc {
    private RootNfc() {}

    public static boolean tryToggle(String action) {
        if (!"enable".equals(action) && !"disable".equals(action)) return false;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "svc nfc " + action});
            int code = process.waitFor();
            if (code == 0) return true;

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = br.readLine();
            return line == null || line.trim().isEmpty();
        } catch (Exception ignored) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
