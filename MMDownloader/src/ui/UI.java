package ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import downloader.Preprocess;
import sys.SystemInfo;

public class UI {
	private final int EXIT = 0;
	
	//제작자 출력. 수정 금지
	private UI(){
		SystemInfo.printVersionInfo();
		SystemInfo.makeDir(); //시작과 동시에 Marumaru폴더 생성
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
			case 1:
				System.out.print("주소를 입력하세요: ");
				comicAddress = in.readLine().trim();
				preprocess.connector(comicAddress, 0, in);
				preprocess.close();
				break;

			case 2:
				System.out.print("전체보기 주소를 입력하세요: ");
				comicAddress = in.readLine().trim();
				preprocess.connector(comicAddress, 1, in);
				preprocess.close();
				break;
				
			case 3:
				SystemInfo.makeDir();
				SystemInfo.openDir();
				break;
			
			case 4:
				SystemInfo.openBrowser();
				break;
				
			case 8:
				SystemInfo.printLatestVersionInfo(in);
				break;
			
			case 9:
				SystemInfo.help();
				break;
			
			case 0:
				System.out.println("프로그램을 종료합니다");
				break;
			}
		}
		
		in.close();
	}
	
	/**
	 * <p>UI에 보여줄 메뉴 출력 메서드
	 */
	public static void printMenu(){
		String menu = 
				"메뉴를 선택하세요\n"+
				"  1. 만화 다운로드\n"+
				"  2. 선택적 다운로드\n"+
				"  3. 다운로드 폴더 열기\n"+
				"  4. 마루마루 접속\n"+
				"  8. 업데이트 확인\n"+
				"  9. 도움말\n"+
				"  0. 종료";
		System.out.println(menu);
	}
	
	public void close(){
		instance = null;
	}
}
