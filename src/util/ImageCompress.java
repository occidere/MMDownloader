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
	
	/**
	 * 지정한 디렉토리 내부 모든 파일을 .zip으로 압축. 
	 * 대상 디렉토리명으로 자동 압축파일 이름을 지정한다. 
	 * @param targetDirectory 압축할 파일들이 있는 디렉토리 (.zip을 붙이지 않는다.) 
	 * @throws Exception
	 */
	public static void compress(String targetDirectory) throws Exception {
		compress(targetDirectory, targetDirectory);
	}
	
	/**
	 * 지정한 디렉토리 내부 모든 파일을 .zip으로 압축 
	 * @param saveFileFullName 저장할 압축 파일의 경로 (.zip을 붙이지 않는다.) 
	 * @param targetDirectory 압축할 파일들이 있는 디렉토리 
	 * @throws Exception
	 */
	public static void compress(String saveFileFullName, String targetDirectory) throws Exception {
		saveFileFullName = removeLastSeparator(saveFileFullName) + ".zip";
		targetDirectory = removeLastSeparator(targetDirectory);
		
		System.out.printf("이미지 압축중 ... ");
		
		Collection<File> fileList = FileUtils.listFiles(new File(targetDirectory), null, true);
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(saveFileFullName));

		for(File image : fileList) {
			String canonicalPath = StringUtils.substringAfter(image.getAbsolutePath(), getLastDirectory(targetDirectory));
			
			zipOutputStream.putNextEntry(new ZipEntry(canonicalPath));
			zipOutputStream.write(FileUtils.readFileToByteArray(image));
			zipOutputStream.closeEntry();
		}
		zipOutputStream.close();
		
		System.out.println("성공");
	}
	
	/**
	 * 특정 경로가 주어졌을 때, 마지막 디렉토리 명을 반환한다. 
	 * ex) /Users/occidere/Twice -> Twice
	 * @param absolutePathd 마지막 디렉토리명을 추출하고자 하는 절대경로 
	 * @return 마지막 디렉토리 명 
	 */
	private static String getLastDirectory(String absolutePath) {
		return StringUtils.substringAfterLast(removeLastSeparator(absolutePath), File.separator);
	}
	
	/**
	 * 특정 경로가 주어졌을 때, 마지막 경로 구분자(/ 또는 \)가 있으면 제거한 결과를 반환한다. 
	 * @param path 마지막 경로 구분자를 제거할 경로  
	 * @return 마지막에 경로 구분자가 있었다면 제거된 값  
	 */
	private static String removeLastSeparator(String path) {
		String separator = File.separator;
		return StringUtils.containsOnly(path, separator) ? path : StringUtils.stripEnd(path, separator);
	}
}
