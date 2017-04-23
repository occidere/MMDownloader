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

public class Preprocess {
	
	/* 싱글톤 패턴 */
	private Preprocess(){
		downloader = Downloader.getInstance();
	}

	private static Preprocess instance;
	public static Preprocess getInstance(){
		if(instance==null){
			instance = new Preprocess();
		}
		return instance;
	}
	
	private Downloader downloader; //다운로더 객체

	/**
	 * UI에서 주소를 스트링으로 입력받아 아카이브 리스트를 만들어주는 메서드
	 * @param rawAddress 만화 주소
	 * @param downloadMode 다운로드 모드. (0: 전체 다운, 1: 선택적 다운)
	 */
	public void connector(String rawAddress, int downloadMode, BufferedReader in){
		//아카이브 주소가 담긴 리스트.
		List<Comic> archiveAddress = getArchiveAddress(rawAddress);
		
		switch(downloadMode){
		case 0: //전체 다운로드 기능
			downloader.download(archiveAddress);
			break;
		case 1: //선택적 다운로드 기능
			showList(archiveAddress); //다운 가능한 페이지 출력
			System.out.print("다운받을 번호를 입력하세요: ");
			
			try{
				//입력받은 페이지 정규식에서 공백 모두 제거
				String command = in.readLine().replaceAll(" ", "");

				//잘못된 정규식 삽입시 NullPointerException
				List<Integer> pages = parse(command, archiveAddress.size());
				
				downloader.selectiveDownload(archiveAddress, pages);
			}
			catch(NullPointerException npe){
				System.err.println("해당 페이지와 매칭되는 만화가 없습니다.");
			}
			catch(Exception e){
				e.printStackTrace();
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
	private List<Comic> getArchiveAddress(String rawAddress){
		List<Comic> archiveAddress = null;
		
		//rawAddress = toNewArchivesName(rawAddress);
		
		//각 아카이브주소와 매핑되는 제목, 아카이브주소
		String archiveTitle, archiveAddr;
		
		//wasabisyrup, yuncomics, shencomics와 같은 아카이브 주소가 들어오는 경우
		if(rawAddress.contains("http") && rawAddress.contains("archives")){
			archiveAddress = new ArrayList<>(1);
			archiveAddress.add(new Comic("단편 아카이브 다운로드", rawAddress)); //아카이브 주소가 들어왔을 때
			return archiveAddress;
		}
		
		try{
			//Jsoup을 이용하여 파싱. timeout은 5분
			Document doc = Jsoup.connect(rawAddress).userAgent(downloader.USER_AGENT).header("charset", "utf-8").timeout(downloader.MAX_WAIT_TIME).get();
			
			/* 정규식 좀더 강력하게 수정-> <div class="Content">에서 href=".../archives/.."가 포함된 모든 주소 파싱 */
			Elements divContent = doc.select("div.content").select("[href*=/archives/]");
			
			//리스트의 예상 크기 = divContent (이 사이즈만큼 주소 개수가 있기 때문)
			archiveAddress = new ArrayList<>(divContent.size());

			for(Element e : divContent){
				archiveTitle = e.text().trim();
				//닥터스톤 같은 경우는 1화가 중복 입력되있어서, 이름이 하나가 출력 안됨.
				if(archiveTitle.equals("") || archiveTitle == null) archiveTitle = "제목이 없습니다";
				
				//archiveAddr = toNewArchivesName(e.attr("href").trim());
				archiveAddr = e.attr("href").trim();
				archiveAddress.add(new Comic(archiveTitle, archiveAddr));
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return archiveAddress;
	}
	
	/**
	 * 선택적 다운로드 하기 전 (0번. 원피스 1화) 이런식으로 만화 리스트 보여주기
	 * @param archiveAddress 아카이브 주소가 담긴 리스트
	 */
	private void showList(List<Comic> archiveAddress){
		int idx = 0;
		for(Comic comic : archiveAddress)
			System.out.println(String.format("%3d. %s", idx++, comic.title));
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
		
		int i, j, start, end, splittedLen = splitted.length, tmp, tmpPagesLen;

		try{
			for(i = 0; i < splittedLen; i++){
				//delmin(~, -)을 기준으로 쪼갠다.
				strPages = splitted[i].split(String.format("%s|%s", delim[0], delim[1]));
				
				//스트링 페이지 배열 길이가 1이면, 숫자 1개라는 의미
				if(1 == strPages.length) 
					tmpPages.add(Integer.parseInt(strPages[0]));
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

					for(j = start; j <= end; j++) tmpPages.add(j);
				}
			}
			
			//임시페이지 저장소의 크기
			tmpPagesLen = tmpPages.size();
			
			pages = new ArrayList<>(tmpPagesLen);
			
			//Set에 담긴 중복이 제거된 페이지 번호를 iterator와 auto unboxing 기능을 활용하여 배열에 저장
			Iterator<Integer> it = tmpPages.iterator();
			while(it.hasNext()){
				tmp = it.next();
				if(tmp<maxPage) pages.add(tmp);
			}
			
			//오름차순 페이지 정렬
			java.util.Collections.sort(pages);
			
			//페이지 번호가 담긴 배열의 크기가 0이면 에러
			if(pages.size()==0) throw new IndexOutOfBoundsException("페이지 선택 형식이 잘못되었습니다.");
		}
		catch(IndexOutOfBoundsException iobe){
			//어지간한 페이지 에러는 다 여기서 잡힌다.
			System.err.println("유효한 페이지를 입력해주세요.");
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return pages;
	}
	
	public void close(){
		instance = null;
		downloader.close();
	}
}
