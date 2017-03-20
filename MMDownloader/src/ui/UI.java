package ui;

import java.util.Scanner;

import downloader.Downloader;
import sys.SystemInfo;

public class UI {
	private final int EXIT = 0;
	
	//제작자 출력. 수정 금지
	private UI(){
		System.out.println(SystemInfo.VERSION);
	}
	
	private static UI instance;
	public static UI getInstance(){
		if(instance==null) instance = new UI();
		return instance;
	}

	public void showMenu() {

		Scanner sc = new Scanner(System.in);
		Downloader downloader = Downloader.getInstance();

		int menuNum = Integer.MAX_VALUE;
		String comicAddress;

		while (menuNum != EXIT) {
			System.out.println("메뉴를 선택하세요\n  1. 만화 다운로드\n  2. 다운로드 폴더 열기\n  3. 마루마루 접속\n  0. 종료");
			menuNum = sc.nextInt();

			switch (menuNum) {
			case 1:
				System.out.print("주소를 입력하세요: ");
				comicAddress = sc.next().trim();
				downloader.download(comicAddress);
				downloader.close();
				break;

			case 2:
				SystemInfo.makeDir();
				SystemInfo.openDir();
				break;
			case 3:
				SystemInfo.openBrowser();
				break;
			case 0:
				System.out.println("프로그램을 종료합니다");
				break;
			}
		}
		sc.close();
	}
	
	public void close(){
		instance = null;
	}
}
