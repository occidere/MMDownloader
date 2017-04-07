package downloader;

/**
 * 만화 아카이브 주소 파싱 시 "아카이브 주소"와, "주소" 등의 정보를 모아놓은 클래스.
 * 현재 최소 경량화 상태
 * @author occid
 *
 */
public class Comic {
	String title; //해당 주소와 매칭되는 제목
	String addr; //아카이브 주소
	
	//생성자 통해 정보 입력
	public Comic(String title, String addr){
		this.title = title;
		this.addr = addr;
	}
}