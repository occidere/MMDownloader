package downloader;

import java.util.LinkedList;

public class KMP {
	private KMP(){}
	
	private static KMP instance = null;
	public static KMP getInstance(){
		if(instance==null) instance = new KMP();
		return instance;
	}
	public int getCount(String str, String pattern){
		int count = 0;
		int pi[] = getPi(pattern);
		int n = str.length(), m = pattern.length(), i, j=0;
		char []s = str.toCharArray(), p = pattern.toCharArray();
		
		for(i=0;i<n;i++){
			while(j>0 && s[i]!=p[j]) j = pi[j-1];
			
			if(s[i] == p[j]){
				if(j==m-1){
					count++;
					j = pi[j];
				}
				else j++;
			}
		}
		return count;
	}
	
	public LinkedList<Integer> getList(String str, String pattern){
		LinkedList<Integer> list = new LinkedList<>();
		int pi[] = getPi(pattern);
		int n = str.length(), m = pattern.length(), i, j=0;
		char[]s = str.toCharArray(), p = pattern.toCharArray();
		for(i=0;i<n;i++){
			while(j>0 && s[i]!=p[j]) j = pi[j-1];
			if(s[i]==p[j]){
				if(j==m-1){
					list.add(i-m+1);
					j = pi[j];
				}
				else j++;
			}
		}
		return list;
	}
	
	private int[] getPi(String pattern){
		int i, j=0, m = pattern.length();
		char[] p = pattern.toCharArray();
		int []pi = new int[m];
		
		for(i=1;i<m;i++){
			while(j>0 && p[i] != p[j]) j = pi[j-1];
			if(p[i] == p[j]) pi[i] = ++j;
		}
		return pi;
	}
	
	public void close(){
		instance = null;
	}
}