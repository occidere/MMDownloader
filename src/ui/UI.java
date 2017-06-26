package ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import downloader.Preprocess;
import sys.SystemInfo;
import sys.Configuration;

public class UI {
	private final int EXIT = 0;
	
	private UI(){
		Configuration.applyConf(); //시작과 동시에 설정파일(MMDownloader.conf) 읽어들여 적용
		SystemInfo.makeDir(); //시작과 동시에 디폴트 폴더 생성.
		SystemInfo.makeDir(SystemInfo.PATH); //시작과 동시에 사용자 지정 다운로드 폴더 생성
		SystemInfo.printProgramInfo();//버전 출력
	}
	
	private static UI instance;
	public static UI getInstance(){
		if(instance==null) instance = new UI();
		return instance;
	}

	public void showMenu() throws Exception {

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		Preprocess preprocess = Preprocess.getInstance();

		int menuNum = Integer.MAX_VALUE;
		String comicAddress;

		while (menuNum != EXIT) {
			
			printMenu(); //메뉴 출력
			menuNum = Integer.parseInt(in.readLine());

			switch (menuNum) {
			case 1: //일반 다운로드
				System.out.print("주소를 입력하세요: ");
				comicAddress = in.readLine().trim();
				preprocess.connector(comicAddress, 0, in);
				preprocess.close();
				break;

			case 2: //선택적 다운로드
				System.out.print("전체보기 주소를 입력하세요: ");
				comicAddress = in.readLine().trim();
				preprocess.connector(comicAddress, 1, in);
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
				
				//환경설정 메뉴
				switch(menuNum){
				case 1: //업데이트 확인
					SystemInfo.printLatestVersionInfo(in);
					break;
					
				case 2: //저장경로 변경
					System.out.println("현재 저장경로: "+SystemInfo.PATH);
					System.out.print("변경할 경로를 입력하세요: ");
					String path = in.readLine().trim();
					if(Configuration.changePath(path)) System.out.println("저장경로 변경 완료!");
					else SystemInfo.printError("저장경로 변경 실패", false);
					break;
					
				}
				menuNum = 8; //이걸 달아줘야지 종료되는거 막을 수 있음
				Configuration.applyConf(); //설정 후 설정파일(MMDownloader.conf) 읽어들여 적용
				
				break;
			
			case 9: //도움말
				SystemInfo.help();
				break;
			
			case 0: //종료
				System.out.println("프로그램을 종료합니다");
				break;
			}
		}
		
		in.close();
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
				"  9. 뒤로";
		System.out.println(settingMenu);
	}
	
	public void close(){
		instance = null;
	}
}