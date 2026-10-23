package io.github.rd227.facescan;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class OfficialHttpTest {
    static class Jar implements OfficialHttp.Cookies {
        final List<String> writes = new ArrayList<>();
        @Override public String get(String url) { return "session=example"; }
        @Override public void set(String url, String cookie) { writes.add(url + " " + cookie); }
        @Override public void flush() { }
    }
    static class Connection extends HttpURLConnection {
        final int code;
        final byte[] bytes;
        final Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        boolean closed;
        Connection(int code, byte[] bytes) throws Exception {
            super(new URL("https://sso.chaoxing.com/")); this.code = code; this.bytes = bytes;
        }
        @Override public int getResponseCode() { return code; }
        @Override public Map<String, List<String>> getHeaderFields() { return responseHeaders; }
        @Override public String getHeaderField(String name) {
            List<String> values = responseHeaders.get(name); return values == null ? null : values.get(0);
        }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public InputStream getErrorStream() { return getInputStream(); }
        @Override public void disconnect() { closed = true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
    }
    @Test public void sharesCookiesAndReturnsOfficialBody() throws Exception {
        Jar jar = new Jar();
        Connection connection = new Connection(200, "{\"msg\":{\"uid\":123}}".getBytes(StandardCharsets.UTF_8));
        connection.responseHeaders.put("Set-Cookie", List.of("newSession=value; Secure; HttpOnly"));
        OfficialHttp client = new OfficialHttp(jar, "FaceScan-test", uri -> connection);
        OfficialHttp.Response result = client.get("https://sso.chaoxing.com/account");
        assertEquals(200, result.status);
        assertTrue(result.body.contains("123"));
        assertEquals("session=example", connection.getRequestProperty("Cookie"));
        assertEquals(1, jar.writes.size());
        assertTrue(connection.closed);
    }
    @Test public void neverFollowsRedirectToAnotherService() throws Exception {
        Connection connection = new Connection(302, new byte[0]);
        connection.responseHeaders.put("Location", List.of("https://unrelated.example/"));
        int[] calls = {0};
        OfficialHttp client = new OfficialHttp(new Jar(), "test", uri -> { calls[0]++; return connection; });
        assertThrows(IOException.class, () -> client.get("https://sso.chaoxing.com/account"));
        assertEquals(1, calls[0]);
        assertTrue(connection.closed);
    }
    @Test public void refusesCleartextRedirect() throws Exception {
        Connection connection = new Connection(302, new byte[0]);
        connection.responseHeaders.put("Location", List.of("http://sso.chaoxing.com/next"));
        OfficialHttp client = new OfficialHttp(new Jar(), "test", uri -> connection);
        assertThrows(IOException.class, () -> client.get("https://sso.chaoxing.com/account"));
    }
    @Test public void followsRelativeOfficialRedirect() throws Exception {
        Connection first = new Connection(302, new byte[0]);
        first.responseHeaders.put("Location", List.of("/complete"));
        Connection second = new Connection(200, "ok".getBytes(StandardCharsets.UTF_8));
        Deque<Connection> queue = new ArrayDeque<>(List.of(first, second));
        List<URI> visited = new ArrayList<>();
        OfficialHttp client = new OfficialHttp(new Jar(), "test", uri -> { visited.add(uri); return queue.removeFirst(); });
        assertEquals("ok", client.get("https://sso.chaoxing.com/start").body);
        assertEquals("https://sso.chaoxing.com/complete", visited.get(1).toString());
        assertTrue(first.closed && second.closed);
    }
    @Test public void capsResponseSize() throws Exception {
        Connection connection = new Connection(200, new byte[2 * 1024 * 1024 + 1]);
        OfficialHttp client = new OfficialHttp(new Jar(), "test", uri -> connection);
        assertThrows(IOException.class, () -> client.get("https://sso.chaoxing.com/account"));
        assertTrue(connection.closed);
    }
    @Test public void keepsFailureStatusInsteadOfInventingSuccess() throws Exception {
        Connection connection = new Connection(401, "unauthorized".getBytes(StandardCharsets.UTF_8));
        OfficialHttp client = new OfficialHttp(new Jar(), "test", uri -> connection);
        assertEquals(401, client.get("https://sso.chaoxing.com/account").status);
    }
    @Test public void boundsRedirectLoops() throws Exception {
        Connection connection = new Connection(302, new byte[0]);
        connection.responseHeaders.put("Location", List.of("/loop"));
        int[] calls = {0};
        OfficialHttp client = new OfficialHttp(new Jar(), "test", uri -> { calls[0]++; return connection; });
        assertThrows(IOException.class, () -> client.get("https://sso.chaoxing.com/start"));
        assertEquals(6, calls[0]);
    }
}
