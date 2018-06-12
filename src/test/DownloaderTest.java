package test;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

public class DownloaderTest {
	
	@Test
	public void refererTest() {
		try {
			String imgURL = "http://wasabisyrup.com/storage/gallery/fTF1QkrSaJ4/P0001_9nmjZa2886s.jpg";
			String host = StringUtils.substringBetween(imgURL, "http://", "/");
			String referer = StringUtils.substringBeforeLast(imgURL, "/");
			
			Assert.assertArrayEquals(new String[]{ host, referer },
					new String[]{ "wasabisyrup.com", "http://wasabisyrup.com/storage/gallery/fTF1QkrSaJ4" });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
