package ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import common.DownloadMod;
import common.ErrorHandling;
import downloader.Preprocess;
import sys.SystemInfo;
import sys.Configuration;

public class UI implements DownloadMod {
	private final int EXIT = 0;
	
	private UI(){
		SystemInfo.makeDir(); //시작과 동시에 디폴트 폴더 생성.
		SystemInfo.makeDir(SystemInfo.PATH); //시작과 동시에 사용자 지정 다운로드 폴더 생성
		Configuration.init(); //설정파일(MMDownloader.properties 읽기 & 적용 & 저장) 수행

		SystemInfo.printProgramInfo();//버전 출력
	}
	
	/* 싱글톤 */
	private static UI instance;
	public static UI getInstance(){
		if(instance==null) instance = new UI();
		return instance;
	}

	public void showMenu() throws Exception {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Preprocess preprocess = Preprocess.getInstance();
		String comicAddress;
		int menuNum = Integer.MAX_VALUE;

		while (menuNum != EXIT) {
			printMenu(); //메뉴 출력
			menuNum = Integer.parseInt(in.readLine());

			switch (menuNum) {
			case 1: //일반 다운로드
				System.out.print("주소를 입력하세요: ");
				comicAddress = in.readLine().trim();
				preprocess.connector(comicAddress, ALL_DOWNLOAD, in);
				preprocess.close();
				break;

			case 2: //선택적 다운로드
				System.out.print("전체보기 주소를 입력하세요: ");
				comicAddress = in.readLine().trim();
				preprocess.connector(comicAddress, SELETIVE_DOWNLOAD, in);
				preprocess.close();
				break;
				
			case 3: //저장 폴더 열기
				SystemInfo.makeDir(SystemInfo.PATH);
				SystemInfo.openDir(SystemInfo.PATH);
				break;
			
			case 4: //마루마루 사이트 열기
				SystemInfo.openBrowser();
				break;
				
			case 8: //환경설정
				printSettingMenu();
				menuNum = Integer.parseInt(in.readLine().trim());
				String input = ""; //환경설정 입력에 이용할 변수
				
				/* 환경설정 메뉴 */
				switch(menuNum){
				
				case 1: //업데이트 확인
					SystemInfo.printLatestVersionInfo(in);
					break;
					
				case 2: //저장경로 변경
					System.out.printf("현재 저장경로: %s\n변경할 경로를 입력하세요: ", SystemInfo.PATH);
					
					String path = in.readLine().trim();
					File newPath = new File(path);
				
					/* 입력한 경로가 만든 적이 없는 경로 & 그런데 새로 생성 실패 */
					if(newPath.exists()==false && newPath.mkdirs()==false) {
						ErrorHandling.printError("저장경로 변경 실패", false);
						break;
					}
					
					/* 생성 가능한 정상적인 경로라면 */
					Configuration.setProperty("PATH", path);
					Configuration.refresh(); //store -> load - > apply
					System.out.println("저장경로 변경 완료!");
					break;
			
				case 3: //다운받은 만화 하나로 합치기
					boolean merge = Configuration.getBoolean("MERGE", false);
					System.out.printf("true면 다운받은 만화를 하나의 긴 파일로 합친 파일을 추가로 생성합니다(현재: %s)\n", merge);
					System.out.print("값 입력(true or false): ");
					
					input = in.readLine().toLowerCase();
					if(!input.equals("true") && !input.equals("false")) {
						ErrorHandling.printError("잘못된 값입니다.", false);
						break;
					}
					
					Configuration.setProperty("MERGE", input);
					Configuration.refresh();
					System.out.println("변경 완료");
					break;
					
				case 4:
					boolean debug = Configuration.getBoolean("DEBUG", false);
					System.out.printf("true면 다운로드 과정에 파일의 용량과 메모리 사용량이 같이 출력됩니다(현재: %s)\n", debug);
					System.out.print("값 입력(true or false): ");
					
					input = in.readLine().toLowerCase();
					if(!input.equals("true") && !input.equals("false")) {
						ErrorHandling.printError("잘못된 값입니다.", false);
						break;
					}
					
					Configuration.setProperty("DEBUG", input);
					Configuration.refresh();
					System.out.println("변경 완료");
					break;
				}
				
				menuNum = 8; //이걸 달아줘야지 종료되는거 막을 수 있음
				break;
			
			case 9: //도움말
				SystemInfo.help();
				break;
			
			case 0: //종료
				System.out.println("프로그램을 종료합니다");
				break;
			}
		}
		in.close(); //BufferedReader close
	}
	
	/**
	 * <p>UI에 보여줄 메뉴 출력 메서드
	 */
	private void printMenu(){
		String menu = 
				"메뉴를 선택하세요\n"+
				"  1. 만화 다운로드\n"+
				"  2. 선택적 다운로드\n"+
				"  3. 다운로드 폴더 열기\n"+
				"  4. 마루마루 접속\n"+
				"  8. 환경설정\n"+
				"  9. 도움말\n"+
				"  0. 종료";
		System.out.println(menu);
	}
	
	private void printSettingMenu() {
		String settingMenu = 
				"설정할 메뉴를 선택하세요\n"+
				"  1. 업데이트 확인\n"+
				"  2. 저장경로 변경\n"+
				"  3. 이미지 병합 설정\n"+
				"  4. 디버깅 모드 설정\n"+
				"  9. 뒤로";
		System.out.println(settingMenu);
	}
	
	public void close(){
		instance = null;
	}
}
