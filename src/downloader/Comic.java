package downloader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 만화 아카이브 주소 파싱 시 "아카이브 주소"와, "주소" 등의 정보를 모아놓은 Bean.
 * 현재 최소 경량화 상태
 * @author occidere
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comic {
	private String title; //해당 주소와 매칭되는 제목. ex) 원피스
	private String titleNo; //제목과 회차까지 포함. ex) 원피스 3화
	private String address; //아카이브 주소. ex) http://wasabisyrup.com/archives/_PfyM_Hc3I8
	private List<String> imgURL; //아카이브 주소에 포함된 이미지 파일들의 URL이 담김.
	
	@Override
	public String toString() {
		return String.format("%s_%s\t%s\t%s", title, titleNo, address, imgURL);
	}
}