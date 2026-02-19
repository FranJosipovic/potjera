package com.fran.dev.potjerawebscraper.utils;

import java.util.regex.Pattern;

public final class EpisodeUtils {

    private static final Pattern EP_PATTERN = Pattern.compile("ep-(\\d+)");

    private EpisodeUtils() {}

    public static Integer extractEpisodeNumber(String url) {
        var matcher = EP_PATTERN.matcher(url);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }
}

