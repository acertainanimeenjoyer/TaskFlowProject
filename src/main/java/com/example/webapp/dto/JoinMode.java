package com.example.webapp.dto;

public enum JoinMode {
    EITHER(1),
    BOTH(2),
    ONLY_EMAIL(3),
    ONLY_ID(4);

    private final int code;

    JoinMode(int code) { this.code = code; }

    public int getCode() { return code; }

    public static JoinMode fromCode(int code) {
        for (JoinMode m : values()) if (m.code == code) return m;
        return EITHER;
    }
}
