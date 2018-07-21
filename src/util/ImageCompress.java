package util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

/**
 * 다운받은 만화들을 압축하는 유틸
 * @author occidere
 */
public class ImageCompress {
	public static void main(String[] args) throws Exception {
		compress("/Users/occidere/Desktop/Twice.zip", "/Users/occidere/Twice");
	}
	
	/**
	 * 지정한 디렉토리 내부 모든 파일을 .zip으로 압축 
	 * @param saveFileFullName 저장할 압축 파일의 이름 
	 * @param targetDirectory 압축할 파일들이 있는 디렉토리 
	 * @throws Exception
	 */
	public static void compress(String saveFileFullName, String targetDirectory) throws Exception {
		Collection<File> fileList = FileUtils.listFiles(new File(targetDirectory), null, true);
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(saveFileFullName));

		for(File image : fileList) {
			zipOutputStream.putNextEntry(new ZipEntry(image.getName())); // 압축파일의 루트 경로 조절 문제 미결 
			zipOutputStream.write(FileUtils.readFileToByteArray(image));
			zipOutputStream.closeEntry();
		}
		zipOutputStream.close();
	}
}
