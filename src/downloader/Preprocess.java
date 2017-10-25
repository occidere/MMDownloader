package downloader;

import java.io.BufferedReader;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import downloader.Downloader;
import util.UserAgent;
import common.DownloadMod;
import common.ErrorHandling;

public class Preprocess implements DownloadMod {
	
	/* 싱글톤 패턴 */
	private Preprocess(){
		downloader = Downloader.getInstance();
	}

	private static Preprocess instance;
	public static Preprocess getInstance(){
		if(instance==null) instance = new Preprocess();
		return instance;
	}
	
	private Downloader downloader; //다운로더 객체

	/**
	 * UI에서 주소를 스트링으로 입력받아 아카이브 리스트를 만들어주는 메서드
	 * @param rawAddress 만화 주소
	 * @param downloadMode 다운로드 모드
	 * @param in BufferedReader 객체
	 */
	public void connector(String rawAddress, int downloadMode, BufferedReader in) {
		//아카이브 주소가 담긴 리스트.
		List<Comic> archiveAddrList = getArchiveAddrList(rawAddress);
		
		switch(downloadMode){
		case ALL_DOWNLOAD: //전체 다운로드 기능
			downloader.download(archiveAddrList);
			break;
		
		case SELETIVE_DOWNLOAD: //선택적 다운로드 기능
			showList(archiveAddrList); //다운 가능한 페이지 출력
			System.out.print("다운받을 번호를 입력하세요: ");
			
			try{
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
		List<Comic> archiveAddrList = new ArrayList<>();
		
		//각 아카이브주소와 매핑되는 제목, 아카이브주소
		String archiveTitle, archiveAddr;
		
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
			//System.out.println(divContent);

			for(Element e : divContent){
				archiveTitle = e.text().trim();
				/* 닥터스톤 1화, 아발트 1화의 경우 텍스트 없이 주소값만 중복으로 입력되있음.
				 * 어차피 바로 아래에 정상적으로 있으므로 그냥 걸러줌
				 * <a target="_self" href="http://wasabisyrup.com/archives/U2j2ASrArLA"></a>
				 * <a target="_self" href="http://wasabisyrup.com/archives/U2j2ASrArLA">닥터 스톤(Dr. Stone) 1화</a> */
				if(archiveTitle == null || archiveTitle.equals("")) continue;
				
				//archiveAddr = toNewArchivesName(e.attr("href").trim());
				archiveAddr = e.attr("href").trim();
				archiveAddrList.add( new Comic(archiveTitle, null, archiveAddr, null) );
			}
		}
		catch(Exception e){
			ErrorHandling.saveErrLog("Jsoup 파싱 실패", "", e);
		}
		return archiveAddrList;
	}
	
	/**
	 * 선택적 다운로드 하기 전 (1번. 원피스 1화) 이런식으로 만화 리스트 보여주기
	 * 인덱스는 1부터 시작으로 변경
	 * @param archiveAddrList 아카이브 주소가 담긴 리스트
	 */
	private void showList(List<Comic> archiveAddrList){
		int idx = 1;
		for(Comic comic : archiveAddrList) {
			System.out.println(String.format("%3d. %s", idx++, comic.getTitle()));
		}
	}
	
	/**
	 * 여러편 다운로드를 위한 정규식 파싱해서 페이지 번호를 담은 int[] 로 반환
	 * @param command 여러편 다운로드 정규식
	 * @return 페이지 번호를 담은 리스트
	 */
	private List<Integer> parse(String command, int maxPage){
		String spliter = ","; //최우선 구분자
		String delim[] = { "~", "-" }; //페이지 간 구분자
		
		//구분자를 기준으로 하여"1,2,3-4,5~6 을 { "1", "2", "3-4", "5~6" }으로 split
		String splitted[] = command.split(spliter);
		String strPages[]; //임시로 사용될 스트링 배열 타입의 페이지 번호 집합

		Set<Integer> tmpPages = new HashSet<>();
		List<Integer> pages = null;
		
		int i, j, start, end, splittedLen = splitted.length, tmp, tmpPagesLen, pageNum;

		try{
			for(i = 0; i < splittedLen; i++){
				//delmin(~, -)을 기준으로 쪼갠다.
				strPages = splitted[i].split(String.format("%s|%s", delim[0], delim[1]));
				
				//스트링 페이지 배열 길이가 1이면, 숫자 1개라는 의미
				if(1 == strPages.length){
					pageNum = Integer.parseInt(strPages[0]);
					if(isCorrectPage(1, maxPage, pageNum)) tmpPages.add(pageNum);
				}
				//아니라면 s = 시작페이지, e = 끝페이지 까지 순차 저장.
				else{ 
					start = Integer.parseInt(strPages[0]);
					end = Integer.parseInt(strPages[1]);
					
					//페이지 순서 오름차순 유지
					if(end < start){
						tmp = start;
						start = end;
						end = tmp;
					}

					for(j = start; j <= end; j++){
						if(isCorrectPage(1, maxPage, j))
							tmpPages.add(j);
					}
				}
			}
			
			//임시페이지 저장소의 크기
			tmpPagesLen = tmpPages.size();
			
			pages = new ArrayList<>(tmpPagesLen);
			
			//Set에 담긴 중복이 제거된 페이지 번호를 iterator와 auto unboxing 기능을 활용하여 배열에 저장
			Iterator<Integer> it = tmpPages.iterator();
			while(it.hasNext()){
				tmp = it.next();
				// 인덱스 번호를 1번부터 입력받도록 했으므로, -1 필수
				if(tmp<=maxPage)  pages.add(tmp - 1);
			}
			
			//오름차순 페이지 정렬
			java.util.Collections.sort(pages);
			
			//페이지 번호가 담긴 배열의 크기가 0이면 에러
			if(pages.size()==0) throw new IndexOutOfBoundsException("페이지 선택 형식이 잘못되었습니다.");
		}
		catch(IndexOutOfBoundsException iobe){
			//어지간한 페이지 에러는 다 여기서 잡힌다.
			ErrorHandling.printError("유효한 페이지를 입력해주세요.", false);
		}
		catch(Exception e){
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
