package com.instrasoft.csp.ccs.utils;


public class VersionParser {

    public static Integer fromString(String version) {
        version = version.replace(".", "");
        return Integer.parseInt(version);
    }

    public static String toString(Integer version) {
        String v = version.toString();
        if (v.length() == 1) {
            return v;
        }
        else if (v.length() == 2) {
            return v.charAt(0) + "." + v.charAt(1);
        }
        else {
            return v.charAt(0) + "." + v.charAt(1) + "." + v.substring(2, v.length());
        }
    }
}