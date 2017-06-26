package sys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * 환경설정을 담당하는 클래스
 * @author occidere
 *
 */
public class Configuration {
	//환경설정 파일 위치: Marumaru/MMDownloader.conf로 고정
	private static final String CONF_PATH = SystemInfo.DEFAULT_PATH + SystemInfo.fileSeparator + "MMDownloader.conf";
	
	/**
	 * path에 있는 파일을 읽어서 라인별로 쪼개 String[] 으로 반환
	 * @param path 읽어들일 파일
	 * @return 읽어들인 파일을 라인별로 쪼개놓은 String[] 배열
	 * @throws FileNotFoundException path의 파일이 없는 경우
	 * @throws IOException 파일이 있으나 읽어들이는데 실패한 경우
	 */
	private static String[] read(String path) throws FileNotFoundException, IOException {
		File file = new File(path);
		if(file.exists() == false){
			throw new FileNotFoundException(String.format("[Error] %s가 존재하지 않습니다.\n", path));
		}
		
		List<String> confList = new LinkedList<>();
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line;
		while((line = in.readLine())!=null){
			line = removeBOM(line); //BOM 문자 제거
			confList.add(line);
		}
		in.close();
		int i = 0, size = confList.size();
		
		/* 필요없을듯
		if(size == 0){
			System.err.println("[Warning] 빈 파일입니다.");
		}
		*/
		
		String confArr[] = new String[size];
		for(String confLine : confList){
			confArr[i++] = confLine;
		}
		return confArr;
	}
	
	/**
	 * <b>경로 변경 메서드</b></br>
	 * 내부에서 writeConf(String, String)을 호출하여 변경내역을 conf파일에 쓰기까지 시도한다.
	 * @param newPath 변경할 경로
	 * @return 성공하면 true, 실패하면 false
	 */
	public static boolean changePath(String newPath){
		File file = new File(newPath);
		//존재하지도 않으면서 생성도 실패하면 잘못된 디렉토리
		if(file.exists() == false && file.mkdirs() == false){
			SystemInfo.printError(String.format("%s는 올바른 디렉토리가 아닙니다.", newPath), false);
			return false;
		}
		boolean res = writeConf("PATH", newPath);
		if(res) SystemInfo.PATH = newPath + SystemInfo.fileSeparator;;
		return res;
	}
	
	/**
	 * <b>conf파일을 읽어서 올바른 설정값들을 적용하는 메서드</b></br>
	 * 파일이 없는 경우 그냥 종료</br>
	 * 있는 경우 내부에서 read(String)을 호출하여 String[] 을 획득한다.</br>
	 * 이후 이 배열을 순차탐색 하며 적절한 설정값을 찾아 적용한다.</br>
	 */
	public static void applyConf(){
		File file = new File(CONF_PATH);
		if(file.exists() == false){
			return;
		}
		
		String confArr[] = null;
		try{
			confArr = read(CONF_PATH);
		}
		catch(IOException e){
			e.printStackTrace();
			return; //에러 나면 설정 적용 자체가 불가하므로 종료
		}
		
		String trimmedConfLine, nameValue[];
		for(String confLine : confArr){
			trimmedConfLine = confLine.replaceAll(" ", "");
			if(isComment(trimmedConfLine)==false && isCorrectFormat(trimmedConfLine)){
				nameValue = trimmedConfLine.split("=");//nameValue[0] = name, nameValue[1] = value
				
				System.out.println(nameValue[0]);
				System.out.println(nameValue[1]);
				
				/* 설정값 적용 부분 */
				if(nameValue[0].equals("PATH")){
					SystemInfo.PATH = nameValue[1] + SystemInfo.fileSeparator; //PATH 설정 적용
				}
				else; //추가적인 설정 적용
				
				
			}
		}
		
	}
	
	/**
	 * <b>conf파일에 값을 쓰는 메서드</b></br>
	 * 만약 쓸 값이 기존에 이미 입력되어 있던 값이라면 해당 부분 찾아서 수정후 쓴다.</br>
	 * 쓸 값이 새로 추가된 경우라면 기존 파일의 맨 아랫줄에 추가적으로 쓴다.</br>
	 * @param name 설정 변수
	 * @param value 설정 값
	 * @return 쓰기에 성공하면 true, 실패하면 false
	 */
	public static boolean writeConf(String name, String value) {
		String confArr[] = null, newConfLine = name+"="+value;//설정파일 라인은 반드시 name=value 형태
		File file = null;
		try {
			/* conf파일에 써야 되기 때문에 만약 conf파일이 없으면 새로 생성한다. */
			file = new File(CONF_PATH);
			if(file.exists() == false) file.createNewFile();
			
			//conf파일이 있었다면 기존 내용이 저장되고, 없어서 새로 만든거면 길이 0짜리가 입력됨
			confArr = read(CONF_PATH);
			
			String confLine, trimmedConfLine;
			boolean isWrote = false;
			int i, n = confArr.length;
			
			for(i=0;i<n;i++) {
				confLine = confArr[i];
				//주석이 아니면서 정상적인 포맷의 설정 커맨드이면
				if(isComment(confLine) == false && isCorrectFormat(confLine)){
					trimmedConfLine = confLine.replaceAll(" ", ""); //공백 다 제거
					if(trimmedConfLine.startsWith(name)){ //설정 변수와 일치하는 라인 찾으면
						confArr[i] = newConfLine; //새로운 값으로 수정
						isWrote = true; //수정 완료 체크
						break;
					}
				}
			}
			
			BufferedWriter out;
			if(isWrote){ //기존 설정부분 찾아 바꾼 경우면 배열 내용(설정파일 내용) 통째로 덮어쓰기
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)));
				for(String line : confArr){
					out.write(line+SystemInfo.lineSeparator);
				}
			}
			else{ //기존 설정부분이 없는 경우(못찾은 경우) 맨 아래에 이어쓰기
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
				out.write(newConfLine+SystemInfo.lineSeparator);
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * <b>라인 내용이 주석인지 판별하는 메서드</b></br>
	 * 공백 다 제거한 뒤, 길이가 0보다 크면서 && 맨 첫번째가 #로 시작하면 주석임.
	 * @param confLine 주석인지 판별할 설정파일 라인
	 * @return 주석이면 true, 주석이 아니면 false
	 */
	private static boolean isComment(String confLine){
		String trimmedConfLine = confLine.replaceAll(" ", "");
		return trimmedConfLine.length()>0 && trimmedConfLine.charAt(0) == '#';
	}
	
	/**
	 * <b>설정파일 라인 내용이 올바른 형식인지 확인하는 메서드</b></br>
	 * <li>1. name=value 형식으로 이루어져 있어야 한다.
	 * <li>2. = 기호가 반드시 1개만 들어가 있어야 한다.
	 * <li>3. 공백 여부는 크게 상관 없다.
	 * <p>
	 * 위의 기준에 근거하여 =를 기준으로 split한 결과가 2개로 쪼개지면 정상 포맷으로 판별.
	 * @param confLine 올바른 설정값 형식인지 검증할 라인
	 * @return 올바른 설정값이면 true, 아니면 false
	 */
	private static boolean isCorrectFormat(String confLine){
		String trimmedConfLine = confLine.replaceAll(" ", "");
		String conf[] = trimmedConfLine.split("=");
		return conf.length==2;
	}
	
	/**
	 * UTF-8 사용 시, 일부 텍스트 에디터에선 문서 제일 처음에 BOM(Byte Order Mark)을 붙인다. ex) 메모장</br>
	 * 이를 걸러서 순수 스트링만 리턴하기 위한 메서드
	 * @param str BOM이 포함되어 있는지 검사할 스트링
	 * @return BOM이 있다면 제거한 스트링
	 */
	private static String removeBOM(String str){
		char BOM = (char)65279;
		if(str.length()<=0) return str;
		else if(str.charAt(0) == BOM) str = str.substring(1);
		return str;
	}
}
