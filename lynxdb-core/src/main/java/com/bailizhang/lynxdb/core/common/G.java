package com.bailizhang.lynxdb.core.common;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class G {
    public static final G I = new G();

    private final HashMap<String, Long> records = new HashMap<>();

    private Converter converter;

    private G() {

    }

    public void converter(Converter cvt) {
        if(converter == null) {
            converter = cvt;
        }
    }

    public void incrementRecord(String name, long increment) {
        long value = records.getOrDefault(name, 0L);
        records.put(name, value + increment);
    }

    public void printRecord() {
        records.keySet().forEach(key -> {
            if(key.endsWith("Count")) {
                return;
            }

            long millis = TimeUnit.MILLISECONDS.convert(
                    records.get(key),
                    TimeUnit.NANOSECONDS
            );
            records.put(key, millis);
        });
        System.out.println(records);
    }

    public byte[] toBytes(String src) {
        return converter.toBytes(src);
    }

    public String toString(byte[] src) {
        return src == null ? null : converter.toString(src);
    }
}
