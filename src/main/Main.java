package main;

import ui.UI;

public class Main {
	public static void main(String args[]){
		try{
			UI ui = UI.getInstance();
			ui.showMenu();
			ui.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}