package com.aidaily.ai;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class MessageNoiseFilter {

    private static final Set<String> NOISE_EXACT = new HashSet<String>(Arrays.asList(
            "收到", "好的", "好", "ok", "OK", "Ok", "嗯", "哦", "谢谢", "感谢",
            "哈哈", "哈哈哈", "?", "？", "1", "11", "111", "666", "+1"
    ));

    private static final Pattern NOISE_PATTERN = Pattern.compile("^[\\s\\p{Punct}\\d]+$");

    public boolean isNoise(String content) {
        if (content == null) {
            return true;
        }
        String text = content.trim();
        if (text.length() < 2) {
            return true;
        }
        if (NOISE_EXACT.contains(text)) {
            return true;
        }
        return NOISE_PATTERN.matcher(text).matches();
    }
}
