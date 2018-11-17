package util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

@Slf4j
public class ImageMergeTest {

	private File root = new File("/Users/occidere/Marumaru/변경의 노기사 -발드 로엔-/변경의 노기사 -발드 로엔- 23화");
	private String imgExt = ".+\\.(?i)(jpe?g|bmp|png|gif)$"; //확장자 정규식

	@Test
	public void imageMergeTest() throws Exception {
		File output = new File(root + "/output.jpg");
		if(output.exists()) {
			output.delete();
		}

		File images[] = root.listFiles(f -> f.getName().matches(imgExt));
		Arrays.sort(images);


		FileOutputStream fos = new FileOutputStream(output);

		for(File f : images) {
			log.info(f.getAbsolutePath());
			fos.write(FileUtils.readFileToByteArray(f));
		}
		fos.close();
	}
}
