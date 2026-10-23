package io.github.rd227.facescan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Native, bounded HTTP transport. Cookies never pass through JavaScript. */
public final class OfficialHttp {
    public interface Cookies {
        String get(String url);
        void set(String url, String value);
        void flush();
    }

    interface Connections { HttpURLConnection open(URI uri) throws IOException; }

    public static final class Response {
        public final int status;
        public final String body;
        Response(int status, String body) { this.status = status; this.body = body; }
    }

    private final Cookies cookies;
    private final String userAgent;
    private final Connections connections;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    public OfficialHttp(Cookies cookies, String userAgent) {
        this(cookies, userAgent, uri -> (HttpURLConnection) uri.toURL().openConnection());
    }

    OfficialHttp(Cookies cookies, String userAgent, Connections connections) {
        this.cookies = cookies;
        this.userAgent = userAgent;
        this.connections = connections;
    }

    public Response get(String url) throws IOException {
        URI target = OfficialUrl.parse(url);
        for (int redirects = 0; redirects <= 5; redirects++) {
            HttpURLConnection connection = connections.open(target);
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json, text/html;q=0.8");
                connection.setRequestProperty("User-Agent", userAgent);
                String cookie = cookies.get(target.toString());
                if (cookie != null && !cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
                int status = connection.getResponseCode();
                for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                    if (header.getKey() != null && header.getKey().equalsIgnoreCase("Set-Cookie")) {
                        for (String value : header.getValue()) cookies.set(target.toString(), value);
                    }
                }
                cookies.flush();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("官方接口返回了无效跳转。");
                    URI next;
                    try {
                        URI redirect = target.resolve(location);
                        if (!"https".equalsIgnoreCase(redirect.getScheme())) {
                            throw new IOException("已停止不安全的网络跳转。");
                        }
                        next = OfficialUrl.parse(redirect.toString());
                    }
                    catch (IllegalArgumentException exception) { throw new IOException("已停止访问非超星的跳转地址。"); }
                    target = next;
                    continue;
                }
                InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (stream == null) return new Response(status, "");
                try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (output.size() + count > MAX_BYTES) throw new IOException("官方接口响应过大。");
                        output.write(buffer, 0, count);
                    }
                    return new Response(status, new String(output.toByteArray(), StandardCharsets.UTF_8));
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("官方接口跳转过多，请重新登录。");
    }
}
