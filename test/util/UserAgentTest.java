package util;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
public class UserAgentTest {

	@Test
	public void userAgentConnectionTest() throws Exception {
		String urls[] = {
				"https://marumaru.in",
				"https://wasabisyrup.com"
		};

		Field userAgentField = UserAgent.class.getDeclaredField("USER_AGENTS");
		userAgentField.setAccessible(true);

		String userAgents[] = (String []) userAgentField.get("USER_AGENTS");

		for(String userAgent : userAgents) {
			for (String url : urls) {
				int resposeCode = 0;
				try {
					log.info(userAgent);
					resposeCode = getResponseCode(url, userAgent);
					Assert.assertEquals(resposeCode, 200);
					log.info("PASS: {}", url);
				} catch (AssertionError ae) {
					log.error("Actual response code of {} = {}", url, resposeCode);
				}
			}
		}
	}

	private int getResponseCode(String url, String userAgent) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("User-Agent", userAgent);
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(60000);


		return conn.getResponseCode();
	}
}
