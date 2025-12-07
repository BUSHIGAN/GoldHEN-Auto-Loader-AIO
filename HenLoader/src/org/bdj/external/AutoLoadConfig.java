package org.bdj.external;

import java.io.*;

public class AutoLoadConfig {

    private static final String CONFIG_PATH = "/data/bdj_autoload.cfg";

    public static boolean loadAutoLoadSetting() {
        File f = new File(CONFIG_PATH);
        if (!f.exists()) return false; // Par défaut : OFF

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            br.close();

            if (line != null && line.trim().equals("AUTOLOAD=1"))
                return true;

        } catch (Exception e) {}

        return false;
    }

    public static void saveAutoLoadSetting(boolean enabled) {
        try {
            File f = new File(CONFIG_PATH);

            
            File parent = f.getParentFile();
            if (!parent.exists()) parent.mkdirs();

            PrintWriter pw = new PrintWriter(new FileWriter(f));
            pw.println(enabled ? "AUTOLOAD=1" : "AUTOLOAD=0");
            pw.close();

        } catch (Exception e) {}
    }
}
