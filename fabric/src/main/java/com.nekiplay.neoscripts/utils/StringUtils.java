package com.nekiplay.neoscripts.utils;

public class StringUtils {
    public static String removeAllCodes(String s) {
        while (s.contains("§")) {
            s = s.replace("§"+s.charAt(s.indexOf("§") + 1),"");
        }
        return s;
    }
}
