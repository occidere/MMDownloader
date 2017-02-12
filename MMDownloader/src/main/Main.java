package main;

import ui.UI;

public class Main {
	public static void main(String args[]){
		UI ui = UI.getInstance();
		ui.showMenu();
		ui.close();
	}
}