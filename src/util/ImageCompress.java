package util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 다운받은 만화들을 압축하는 유틸
 * @author occidere
 */
public class ImageCompress {
	private ImageCompress() {}
	public static void main(String[] args) throws Exception {
//		compress("/Users/occidere/Desktop/Twice.zip", "/Users/occidere/Twice");
		compress("/Users/occidere/Twice");
	}
	
	/**
	 * 지정한 디렉토리 내부 모든 파일을 .zip으로 압축. 
	 * 대상 디렉토리명으로 자동 압축파일 이름을 지정한다. 
	 * @param targetDirectory 압축할 파일들이 있는 디렉토리 
	 * @throws Exception
	 */
	public static void compress(String targetDirectory) throws Exception {
		compress(targetDirectory+".zip", targetDirectory);
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
			String canonicalPath = StringUtils.substringAfterLast(image.getAbsolutePath(), getLastDirectory(targetDirectory));
			
			zipOutputStream.putNextEntry(new ZipEntry(canonicalPath));
			zipOutputStream.write(FileUtils.readFileToByteArray(image));
			zipOutputStream.closeEntry();
		}
		zipOutputStream.close();
	}
	
	/**
	 * 특정 경로가 주어졌을 때, 마지막 디렉토리 명을 반환한다. 
	 * ex) /Users/occidere/Twice -> Twice
	 * @param absolutePathd 마지막 디렉토리명을 추출하고자 하는 절대경로 
	 * @return 마지막 디렉토리 명 
	 */
	private static String getLastDirectory(String absolutePath) {
		String separator = File.separator;
		if(StringUtils.containsOnly(absolutePath, separator)) {
			return absolutePath;
		} else if(StringUtils.endsWith(absolutePath, separator)) {
			absolutePath = StringUtils.stripEnd(absolutePath, separator);
		}
		return StringUtils.substringAfterLast(absolutePath, separator);
	}
}
