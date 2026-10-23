package io.github.rd227.facescan;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Validates untrusted scanner, clipboard and redirect input before any network access. */
public final class OfficialUrl {
    private OfficialUrl() { }

    public static URI parse(String input) {
        if (input == null || input.length() > 8192) throw invalid();
        try {
            URI uri = new URI(input.trim());
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) throw invalid();
            String host = uri.getHost();
            if (host == null || uri.getRawUserInfo() != null) throw invalid();
            host = host.toLowerCase(Locale.ROOT);
            if (!host.equals("chaoxing.com") && !host.endsWith(".chaoxing.com")) throw invalid();
            int port = uri.getPort();
            if (port != -1 && port != (scheme.equalsIgnoreCase("https") ? 443 : 80)) throw invalid();
            // Legacy QR codes use HTTP. Preserve the raw query, including its signature.
            if (scheme.equalsIgnoreCase("http")) {
                uri = new URI("https://" + host
                        + (uri.getRawPath() == null ? "" : uri.getRawPath())
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                        + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment()));
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw invalid();
        }
    }

    public static boolean isAllowed(String value) {
        try { parse(value); return true; }
        catch (IllegalArgumentException exception) { return false; }
    }

    public static String activityId(URI uri) {
        Map<String, String> query = new LinkedHashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null) return "";
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            String key = decode(pair[0]);
            if (!key.equals("activeId") && !key.equals("aid") && !key.equals("id")) continue;
            String value = pair.length == 2 ? decode(pair[1]) : "";
            if (query.containsKey(key) && !query.get(key).equals(value)) throw invalid();
            query.put(key, value);
        }
        for (String key : new String[]{"activeId", "aid", "id"}) {
            String value = query.get(key);
            if (value != null && value.matches("[0-9]{1,24}")) return value;
        }
        return "";
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("请使用有效的超星官方 HTTPS 链接；不支持第三方短链接。");
    }

    private static String decode(String value) {
        try { return URLDecoder.decode(value, "UTF-8"); }
        catch (UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
}
