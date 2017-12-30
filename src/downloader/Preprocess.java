package downloader;

import java.io.BufferedReader;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.LinkedList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import downloader.Downloader;
import util.UserAgent;
import common.DownloadMod;
import common.ErrorHandling;

public class Preprocess implements DownloadMod {
	
	private final Downloader downloader; //다운로더 객체
	
	/* 싱글톤 패턴 */
	private Preprocess(){
		downloader = Downloader.getInstance();
	}

	private static volatile Preprocess instance = null;
	
	/* DCL Singleton */
	public static Preprocess getInstance(){
		if(instance == null) {
			synchronized(Preprocess.class) {
				if(instance == null)
					instance = new Preprocess();
			}
		}
		return instance;
	}

	/**
	 * UI에서 주소를 스트링으로 입력받아 아카이브 리스트를 만들어주는 메서드
	 * @param rawAddress 만화 주소
	 * @param downloadMode 다운로드 모드
	 * @param in BufferedReader 객체
	 */
	public void connector(String rawAddress, int downloadMode, final BufferedReader in) {
		//아카이브 주소가 담긴 리스트.
		List<Comic> archiveAddrList = getArchiveAddrList(rawAddress);
		
		switch(downloadMode){
		case ALL_DOWNLOAD: //전체 다운로드 기능
			downloader.download(archiveAddrList);
			break;
		
		case SELECTIVE_DOWNLOAD: //선택적 다운로드 기능
			try{
				if(archiveAddrList.size() == 0) throw new Exception("잘못된 페이지입니다: "+rawAddress);
				
				showList(archiveAddrList); //다운 가능한 페이지 출력
				System.out.print("다운받을 번호를 입력하세요: ");
				//입력받은 페이지 정규식에서 공백 모두 제거
				String command = in.readLine().replaceAll(" ", "");

				//잘못된 정규식 삽입시 NullPointerException
				List<Integer> pages = parse(command, archiveAddrList.size());
				downloader.selectiveDownload(archiveAddrList, pages);
			}
			catch(Exception e){
				ErrorHandling.saveErrLog("다운로드 시도 실패", "", e);
			}
			break;
		}
	}
	
	/**
	 * <p>wasabisyrup, yuncomics, shencomics와 같은 아카이브주소가 들어오는 경우 -> 단편 다운로드에 해당 -> 바로 담아서 return
	 * <p>mangaup/과 같은 업데이트 주소, manga/와 같은 신 전체보기 주소,
	 * uid= 와 같이 구 전체보기 주소가 들어오는 경우 -> 아카이브 주소 파싱
	 * @param rawAddress 위에 언급된 요소들이 포함된 처리 전 주소
	 * @return wasabisyrup과 같은 아카이브 주소가 담긴 리스트
	 */
	private List<Comic> getArchiveAddrList(String rawAddress){
		List<Comic> archiveAddrList = new LinkedList<>();
		
		//wasabisyrup, yuncomics, shencomics와 같은 아카이브 주소가 들어오는 경우
		if(rawAddress.contains("http") && rawAddress.contains("archives")){
			archiveAddrList.add( new Comic("단편 아카이브 다운로드", null, rawAddress, null) );
			return archiveAddrList;
		}
		
		try{
			//Jsoup을 이용하여 파싱.
			Document doc = Jsoup.connect(rawAddress)
					.userAgent(UserAgent.getUserAgent())
					.header("charset", "utf-8")
					.timeout(downloader.MAX_WAIT_TIME)
					.get();
			
			/* 정규식 좀더 강력하게 수정-> <div class="Content">에서 href=".../archives/.."가 포함된 모든 주소 파싱 */
			Elements divContent = doc.select("div.content").select("[href*=/archives/]");

			/* e.text(): 만화 제목
			 * e.attr("href"): 만화 아카이브 주소 */
			archiveAddrList = divContent.stream()
					.filter(e-> e.text().trim() != null && e.text().trim().length() > 0) //정상적인 제목을 지닌 만화들만 추출
					.map(e-> new Comic(e.text(), null, e.attr("href"), null)) //정상 만화들의 제목, 아카이브 주소를 담은 Comic 객체로 변환
					.collect(Collectors.toList()); //리스트로 Collect
		}
		catch(Exception e){
			ErrorHandling.saveErrLog("Jsoup 파싱 실패", "ArchiveAddrList 파싱 실패", e);
		}
		
		return archiveAddrList;
	}
	
	/**
	 * 선택적 다운로드 하기 전 (1번. 원피스 1화) 이런식으로 만화 리스트 보여주기
	 * 인덱스는 1부터 시작으로 변경
	 * @param archiveAddrList 아카이브 주소가 담긴 리스트
	 */
	private void showList(final List<Comic> archiveAddrList){
		int idx = 1;
		for(Comic comic : archiveAddrList)
			System.out.printf("%3d. %s\n", idx++, comic.getTitle());
	}
	
	/**
	 * 여러편 다운로드를 위한 정규식을 파싱하여 페이지 번호를 담은 {@code List<Integer>}를 반환
	 * @param COMMAND 여러편 다운로드 정규식
	 * @param MAX_PAGE 최대 페이지 번호 = 만화 주소 리스트의 길이
	 * @return 페이지 번호를 담은 리스트
	 */
	private List<Integer> parse(final String COMMAND, final int MAX_PAGE){
		final String spliter = ",";				//최우선 구분자
		final String delim[] = { "~", "-" };	//페이지 간 구분자
		final String splitRegex = String.format("[%s|%s]", delim[0], delim[1]); //페이지 간 구분자 정규식
		
		List<Integer> pages = new LinkedList<>(); //최종 페이지 저장 리스트
		
		try {
			final List<Integer> tmpPages = new LinkedList<>(); //임시 페이지 저장 리스트
			
			//공백 제거 후, 구분자를 기준으로 하여"1,2,3-4,5~6 을 { "1", "2", "3-4", "5~6" }으로 split
			Arrays.stream(COMMAND.replaceAll(" ", "").split(spliter)) 
				.map(x-> x.split(splitRegex)) //범위식(~, -)이면 길이 2, 일반 페이지면 길이 1인 배열
				.forEach(x-> {
					try {
						int start = Integer.parseInt(x[0]), end;
								
						if(x.length == 1) { //스트링 페이지 배열 길이가 1이면, 숫자 1개라는 의미
							if(isCorrectPage(1, MAX_PAGE, start))
								tmpPages.add(start); //정상 페이지면 저장
						}
						else { //범위식인 경우. ex) "3 ~ 4" -> {"3", "4"}
							start = Math.min(Integer.parseInt(x[0]), Integer.parseInt(x[1]));
							end = Math.max(Integer.parseInt(x[0]), Integer.parseInt(x[1]));
							
							IntStream.rangeClosed(start, end) // [start, end] 생성
								.filter(y-> isCorrectPage(1, MAX_PAGE, y)) //각 숫자별로 정상페이지인지 검사
								.forEach(tmpPages::add); //정상 페이지는 추가
						}
					}
					catch(Exception e) {
						ErrorHandling.saveErrLog("잘못된 페이지", "정규식: "+COMMAND, e); //로그에 커맨드 출력
					}
				});
			pages = tmpPages.stream()
					.distinct()		//중복제거
					.map(x-> x-1)	//페이지번호 -> 인덱스
					.sorted()		//오름차순 정렬
					.collect(Collectors.toList());
			
			if(pages.size() == 0) throw new Exception("페이지 선택 형식이 잘못되었습니다.");
		}
		catch(Exception e) {
			ErrorHandling.saveErrLog("정규식 파싱 실패", "", e);
		}

		return pages;
	}
	
	/**
	 * <b>선택적 다운로드에서 페이지 분할할 때 각 페이지가 정상 범위에 있는 페이지인지 판별하는 메서드</b></br>
	 * min <= 페이지 <= max 범위에 있어야 정상 페이지.
	 * @param min 최소 페이지
	 * @param max 최대 페이지
	 * @param pageNum 검증할 현재 페이지 번호
	 * @return 정상 페이지이면 true, 아니면 false
	 */
	private boolean isCorrectPage(int min, int max, int pageNum){
		return min<=pageNum && pageNum<=max;
	}
	
	public void close(){
		instance = null;
		downloader.close();
	}
}
/* 
 변경사항
1. 오타 수정
*/