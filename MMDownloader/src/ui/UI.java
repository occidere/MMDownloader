package ui;

import java.io.IOException;
import java.util.Scanner;

import downloader.Downloader;

public class UI {
	private final int EXIT = 0;
	private final String DEFAULT_PATH = "C:\\Marumaru\\";
	
	//제작자 출력. 수정 금지
	private UI(){
		System.out.println("제작자: occidere\t버전: 0.0.1 (2017.02.12)");
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
			System.out.println("메뉴를 선택하세요\n  1. 한 편씩 다운로드\n  2. 여러 편씩 다운로드(구현예정)\n  3. 다운로드 폴더 열기\n  0. 종료");
			menuNum = sc.nextInt();

			switch (menuNum) {
			case 1:
				System.out.print("주소를 입력하세요: ");
				comicAddress = sc.next().trim();
				downloader.download(comicAddress, DEFAULT_PATH);
				downloader.close();
				break;
			case 2:
				System.out.println("추후 구현 예정");
				break;

			case 3:
				downloader = Downloader.getInstance();
				downloader.makeDir(DEFAULT_PATH);
				try {
					Runtime.getRuntime().exec("explorer.exe " + DEFAULT_PATH);
				} catch (IOException e) {
					e.printStackTrace();
				}
				downloader.close();
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
