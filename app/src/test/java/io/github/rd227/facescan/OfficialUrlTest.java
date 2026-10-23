package io.github.rd227.facescan;

import org.junit.Test;
import java.net.URI;
import static org.junit.Assert.*;

public class OfficialUrlTest {
    @Test public void legacyQrKeepsEncodedSignature() {
        URI uri = OfficialUrl.parse("http://mobilelearn.chaoxing.com/widget/sign?aid=123&enc=a%2Bb%3D%3D");
        assertEquals("https", uri.getScheme());
        assertEquals("aid=123&enc=a%2Bb%3D%3D", uri.getRawQuery());
        assertEquals("123", OfficialUrl.activityId(uri));
    }
    @Test public void standardHttpPortIsUpgraded() {
        assertEquals("https://mobilelearn.chaoxing.com/path?enc=%2B",
                OfficialUrl.parse("http://mobilelearn.chaoxing.com:80/path?enc=%2B").toString());
    }
    @Test public void permitsOfficialHostsAndWhitespaceAroundPaste() {
        assertTrue(OfficialUrl.isAllowed(" https://passport2.chaoxing.com/login "));
        assertTrue(OfficialUrl.isAllowed("https://chaoxing.com/"));
        assertTrue(OfficialUrl.isAllowed("HTTPS://MOBILELEARN.CHAOXING.COM:443/path"));
    }
    @Test public void rejectsLookalikesAndThirdPartyShortLinks() {
        for (String url : new String[]{"https://chaoxing.com.evil.test/", "https://notchaoxing.com/",
                "https://su.short.edu.bfmzdx.cn/code/example", "https://127.0.0.1/",
                "https://chaoxing.com./", "https://chaoxing.com:8443/"}) {
            assertFalse(url, OfficialUrl.isAllowed(url));
        }
    }
    @Test public void rejectsCredentialsAndNonWebSchemes() {
        for (String url : new String[]{"https://user:password@chaoxing.com/", "javascript:alert(1)",
                "file:///etc/passwd", "content://provider/photo", "//chaoxing.com/", "chaoxing://sign"}) {
            assertFalse(url, OfficialUrl.isAllowed(url));
        }
    }
    @Test public void acceptsAllObservedActivityFieldNames() {
        for (String key : new String[]{"activeId", "aid", "id"}) {
            assertEquals("789", OfficialUrl.activityId(OfficialUrl.parse("https://mobilelearn.chaoxing.com/?" + key + "=789")));
        }
    }
    @Test public void missingOrNonNumericActivityIsNotInvented() {
        assertEquals("", OfficialUrl.activityId(OfficialUrl.parse("https://chaoxing.com/")));
        assertEquals("", OfficialUrl.activityId(OfficialUrl.parse("https://chaoxing.com/?id=invalid")));
    }
    @Test public void rejectsConflictingActivityFields() {
        assertThrows(IllegalArgumentException.class, () -> OfficialUrl.activityId(
                OfficialUrl.parse("https://chaoxing.com/?aid=1&aid=2")));
    }
    @Test public void rejectsOversizedAndMalformedInput() {
        assertFalse(OfficialUrl.isAllowed(null));
        assertFalse(OfficialUrl.isAllowed("https://chaoxing.com/?x=" + new String(new char[9000]).replace('\0', 'a')));
        assertFalse(OfficialUrl.isAllowed("https://chaoxing.com/?aid=%ZZ"));
        assertFalse(OfficialUrl.isAllowed("https://chaoxing.com/\nmalformed"));
    }
}
