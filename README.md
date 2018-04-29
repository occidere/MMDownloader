# MMDownloader
[![Latest](https://img.shields.io/badge/Latest-v0.5.0.8-brightgreen.svg)](https://github.com/occidere/MMDownloader/releases/tag/v0.5.0.8)
[![Build Status](https://travis-ci.org/occidere/MMDownloader.svg?branch=master)](https://travis-ci.org/occidere/MMDownloader)
![Downloads](https://img.shields.io/github/downloads/occidere/MMDownloader/total.svg)
[![Java Version](https://img.shields.io/badge/Java-1.8-red.svg)](https://www.java.com/ko/)
[![GitHub license](https://img.shields.io/github/license/occidere/MMDownloader.svg)](https://github.com/occidere/MMDownloader/blob/master/LICENSE)

마루마루 다운로더 신규 프로젝트

### 사용법(Gitbook): https://occidere.gitbooks.io/mmdownloader/content/

### 다운로드: [Release](https://github.com/occidere/MMDownloader/releases/tag/v0.5.0.8)

### 주의사항
* 해당 프로그램은 HtmlUnit과 Jsoup 라이브러리를 사용하였으며, Apache License 2.0을 따르고 있습니다.
* HtmlUnit : http://htmlunit.sourceforge.net/
* Jsoup : https://jsoup.org/
* 해외에서 접속 시 403 에러가 발생할 수 있습니다. **해외에선 VPN 사용을 권장**합니다.
* 해당 프로그램 및 소스코드는 개인적인 자바 공부를 위하여 작성되었습니다.
* 저작권법을 준수하여야 합니다.
* 다운로드 받은 만화는 개인 소장용도로만 이용하여야 하고, 재배포, 판매 등의 행위를 하면 안됩니다.
* 해당 소스코드나 프로그램을 악용하여 발생하는 상황에 대해 어떠한 책임도 지지 않습니다.
* 지원되는 플랫폼: Windows, MAC OS X(beta), Linux 입니다.

### 사용 방법
* 다운로드 경로는 (Windows= C:\Users\사용자\Marumaru) (Linux, MAX OS X= /home/사용자/Marumaru/) 입니다.
* 윈도우의 경우 exe파일을 실행시키면 되고, MAC OS X, 리눅스의 경우 java -jar MMDownloader.jar 을 통해 실행시키면 됩니다.
* 리눅스의 경우 nautilus등의 GUI가 지원되어야 폴더열기, 마루마루 사이트 열기 등을 정상적으로 사용할 수 있습니다.
* MAC OS X의 경우 개발자가 맥북이 없어서 테스트가 원할하지 않아 베타 버전 정도로만 생각해주시면 됩니다.
* 자세한 내용은 [여기](https://occidere.gitbooks.io/mmdownloader/content/)를 참고해 주시길 바랍니다.

### 작동과정 중 유의사항
* Jsoup을 이용하여 고속 파싱 시도를 하여 다운로드를 진행합니다.
* 만일 고속 파싱에 실패하면, HtmlUnit을 이용하여 자바스크립트를 포함하여 페이지를 유닛테스트 개념으로 읽어들인 뒤, 이미지를 다운로드 합니다. 초기 자바스크립트 파싱 과정(=이미지 추출과정)에서 시간이 다소 소요될 수 있습니다.
* 다운로드 속도는 마루마루 서버 상태와 PC의 네트워크 속도에 제일 큰 영향을 받습니다.

### 업데이트 내역
--- ver 0.5.0.8 ---
 * 최신버전 업데이트 상황 표시기 수정 완료
 * 만화 URL에 ASCII 이외 값이 포함된 경우 다운로드가 제대로 되지 않던 문제 해결

--- ver 0.5.0.6 ---
 * timeout 설정하여 다운로드 시 hang 문제 처리
 * maven project로 변경
 * 0.5.0.3, 0.5.0.4 기능 모두 도입

--- ver 0.5.0.4 ---
 * [Beta] 정규식을 이용해 입력값을 검증하는 클래스 추가 (InputCheck.java)
 * [Beta] UI.java 및 선택적 다운로드 부분의 입력들 정규식 검증

--- ver 0.5.0.3 ---
 * [Beta] 최대 다운로드 스레드 개수가 총 페이지 수를 넘지 않게 조정
 * [Beta] 기타 오타 수정

--- ver 0.5.0.2 ---
* Beta에서 정식 Release로 변경
* MULTI 프로퍼티 값을 T/F에서 0, 1, 2, 3, 4로 변경
* 코드 가독성 및 성능 향상을 위한 전반적인 대규모 리팩토링 진행
* Stream & Lambda 도입으로 인한 Java7 이하 버전에 대한 지원 중단
* 멀티스레딩 다운로드 모드 추가 -> 다운로드 속도 향상
* 멀티스레딩 환경설정 프로퍼티 추가(MULTI, Default: 2)
* HtmlUnit v2.28로 업데이트
* 디버깅 정보에 스레드 & 날짜 정보 추가 출력
* 싱글톤을 DCL 방식으로 모두 변경
* v0.4.5.0 의 업데이트 내역 반영(gzip, BufferedInputStream, BufferedOutputStream)

--- ver 0.5.0.0 ---
* [Beta] 코드 가독성 및 성능 향상을 위한 전반적인 대규모 리팩토링 진행
* [Beta] Stream & Lambda 도입으로 인한 Java7 이하 버전에 대한 지원 중단
* [Beta] 멀티스레딩 다운로드 모드 추가 -> 다운로드 속도 향상
* [Beta] 멀티스레딩 환경설정 프로퍼티 추가(MULTI, Default: true)
* [Beta] HtmlUnit v2.28로 업데이트
* [Beta] 디버깅 정보에 스레드 & 날짜 정보 추가 출력
* [Beta] 싱글톤을 DCL 방식으로 모두 변경
* [Beta] v0.4.5.0 의 업데이트 내역 반영(gzip, BufferedInputStream, BufferedOutputStream)
* [Beta] 기존 Beta(0.4.5.0) 파일 삭제

--- ver 0.4.5.0 ---
* [Beta] byte buf[] 없애고 BufferedInputStream, BufferedOutputStream으로 대체
* [Beta] Request Header에 Accept-Encoding: gzip 추가

--- ver 0.4.4.3 ---
* 일부 단편만화 다운로드 시 지정된 경로를 찾을 수 없는 버그 수정

--- ver 0.4.4.2 ---
* jdk 1.8.0_152로 업데이트
* 다운로드 속도 출력 추가. ex) 3.37 MB/s 처럼 출력
* 디버깅 모드 추가(다운받은 파일 용량, 메모리 정보 출력)
* 메모리 담당 MemInfo 모듈 추가
* User-Agent Random Generator 모듈 추가
* Timeout 5분에서 1분으로 축소
* Properties 파일이 없는 상태에서 프로그램 실행 시 발생하던 오류 수정
* Depreciated 메서드들 제거
* 기타 오탈자 수정 및 버그 픽스

--- ver 0.4.2.8 ---
* 업데이트 파일 다운로드 시 디렉토리 구분자가 없어서 폴더 분리가 제대로 안 되던 문제 해결
    * .../MarumaruMMDownlaoder.exe -> .../Marumaru/MMDownloader.exe

--- ver 0.4.2.7 ---
* **이미지 병합 기능 추가!**
* 다운받은 만화들을 **웹툰처럼 세로로 길게 이어붙인 파일을 새로 생성**할 수 있음.
* 기존 설정파일 이름 변경 (MMDownloader.conf -> MMDownloader.properties)
    * 자바의 Property를 이용
    * **절대로 MMDownloader.properties의 내용을 직접 수정하지 말 것!**
* 클래스를 기능별로 분리 & common 패키지 생성하여 에러 핸들링, 다운로드 모드 클래스 신설
* 기타 잡다한 버그 수정 및 전반적인 클린코드화

--- ver 0.3.1.0 ---
* 저장경로 변경 기능 추가!
* 환경설정파일(MMDownloader.conf)내부에 저장경로 입력(PATH=변경할경로) 또는 프로그램 내부 8-2 번 메뉴를 통해 변경
* 단, 저장 경로만 변경되며, 다른 업데이트 파일 다운 경로 및 환경설정파일 경로는 항상 기본 경로(C:\Users\사용자\Marumaru) 유지
* 8번 환경설정 메뉴 추가 및 기존 메뉴들 부분 통합
* 선택적 다운로드의 인덱스의 시작을 1번부터로 변경
* 메서드 리팩토링
* 변수명 정리
* 확장자 추출 메서드 최적화
* 에러 출력 형식 변경

--- ver 0.3.0.4 ---
* 주소 UTF-8 변환 메서드 내부에서 띄어쓰기(" ")도 %20 으로 맞춰줌

--- ver 0.3.0.3 ---
* 맥, 리눅스용 업데이트 파일 다운로드시 윈도우용 파일 다운로드 되던 문제 수정
* 업데이트 파일 다운로드시 Marumaru폴더가 없어서 발생하던 에러 수정
* 시작과 동시에(UI 호출) Marumaru 폴더 생성하도록 변경

--- ver 0.3.0.2 ---
* Comic 클래스 내부에서 Image URL이 담길 List, title, titleNo를 모두 관리 -> 전역변수 title, titleNo 제거
* Comic 클래스에 getter/setter 적용
* Comic 객체 내에 List, title, titleNo를 저장하기 위한 parseImageURL() 메서드 새로 생성 / 기존에 사용되던 getImgList() 제거
* 항상 Domain을 최신 아카이브로 설정 / toNewArchivesName() 제거, 전역변수 OLD_ARCHIVES_NAME, NEW_ARCHIVES_NAME, BLOG 제거
* 아카이브 페이지 파싱 시 실패하면 로그파일로 저장
* Downloader.java 내부에 다운로드에 사용되는 FileOutputStream 등의 객체를 모두 전역변수화
* printError(String msg, boolean exitProgram) 메서드 새로 생성
* 기타 오타 수정 및 final 변수명 재설정

--- ver 0.3.0.0 ---
* 업데이트 확인 기능 추가!
* 서버에 현재 버전보다 최신 버전이 있는 경우 다운로드 여부를 물어봄.
* 다운로드 선택 시, 사용자의 OS를 바탕으로 알맞은 버전을 선택하여 다운로드 시도(저장 폴더는 Marumaru 폴더)
* 만화를 다운받다가 에러가 발생하면 로그파일로 저장함.
* Marumaru/log/ 폴더 내부에 2017-04-28_22-24-30_원피스_17화_003.txt 와 같은 텍스트 파일 생성.
* jdk 1.8.0_131로 업데이트!
* 메뉴 출력 부분 메서드로 변경
* 도움말 업데이트
* 버전 표기 4글자로 변경(기존 3.0.0 -> 현재 3.0.0.0)

--- ver 0.2.9 ---
* 아카이브가 최신이 아니여서 이미지가 제대로 저장이 안되는 문제 해결(기존 최신 아카이브 맞춰주는 메서드 살림)
* blog.yuncomics.com과 같은 경우 대처하기 위해 blog를 www로 바꿔줌
* 결합성을 낮추기 위해 최신 아카이브 맞추는 메서드(toNewArchievesName())를 Downloader.java로 옮김
* 기타 사소한 오타들 수정

--- ver 0.2.8 ---
* 선택적 여러편 다운로드 기능 추가! (프로그램 내 사용법 참고)
* 다운로더 클래스와 전처리 클래스 분리
* Scanner 를 BufferedReader로 변경
* List에 담기던 String형식의 주소를 Comic클래스로 재정의하여 담음.
* Comic 클래스 내부엔 title(만화 제목), addr(아카이브주소) 스트링 변수만 존재
* 도움말 항목 추가 (9번)
* 기존에 있던 아카이브 최신 유지 기능 잠정 폐지(없어도 알아서 리디렉션 됨) 

--- ver 0.2.6 ---
* 한글이 포함된 이미지 URL 다운로드 가능!
* 위 문제를 처리하기 위해 UTF-8 인코딩 변환 메서드 추가
* 다운로드 성공 메세지와 같이 다운로드 받은 파일의 크기(KB) 출력

--- ver 0.2.5 ---
* 다운로드시 과거 아카이브주소(shencomics, yuncomics)가 들어가서 제대로 다운되지 않던 버그 수정
* 아카이브 주소 태그 파싱 버그 수정
* 아카이브 주소 파싱 부분 별도 메서드로 분리
* 과거 아카이브 주소를 현재 새로운 아카이브 주소로 변환하는 메서드 추가

--- ver 0.2.4 ---
* 비밀번호 걸린 만화 다운로드 가능(베타; 고속 파싱에 한해서만 가능)
* Jsoup 버전 기존의 1.10.1에서 1.10.2 로 업그레이드
* 기타 오타 수정

--- ver 0.2.3 ---
* MAC OS X, 리눅스 플랫폼 지원
* 마루마루 사이트 오픈 기능 추가
* 기본 다운로드 경로 변경. (Windows= C:\Users\사용자\Marumaru\) (Linux= /home/사용자/Marumaru/)
* 시스템 기능용 패키지(sys) & 클래스(SystemInfo) 별도 생성
* 해당 클래스는 폴더생성&오픈, 마루마루사이트 오픈, 기본경로 등 시스템적 기능 담음
* 버전 출력부 UI 클래스에서 SystemInfo 클래스로 이동

--- ver 0.2.0 ---
* 고속 페이지 파싱 우선 방식 도입 (Jsoup을 이용한 고속 페이지파싱 우선 시도 -> 실패시 HtmlUnit을 이용한 일반 페이지파싱 시도)
* User-Agent를 크롬 모바일(Chrome/30.0.0.0 Mobile)로 변경
* 전역변수 최적화를 통한 재사용성 증가
* 불필요한 메서드 정리

--- ver 0.1.8 ---
* Jsoup을 이용하여 만화제목(title) & 만화 회차(title no)를 구함
* 위의 기능 덕분에 언제나 c:\Marumaru\원피스\원피스 337화\ 와 같이 폴더 생성 가능
* HtmlUnit과 KMP 알고리즘 대신 Jsoup을 이용하여 이미지 url 파싱
* 특수문자 제거 기능 메서드로 추출

--- ver 0.1.7 ---
* Jsoup 라이브러리 추가!
* Jsoup 라이브러리를 이용하여 전체체보기 주소 입력시 개별 만화주소 파싱 알고리즘 변경
* 기존 전권 다운로드 시도시 첫 화만 다운로드 되던 버그 수정
* 다운로드 시도 중 에러 발생시 작업을 전체취소 하는 것이 아닌, 가능한 다음 작업을 찾아서 계속 수행

--- ver 0.1.5 ---
* 만화 업데이트(mangaup) 주소를 넣을 경우 아카이브주소 + 전편 보러가기 주소(marumaru.in/manga/번호)가 같이 들어와서 발생하는 에러 처리

--- ver 0.1.3 ---
* 이미지 다운로드 시도 타임아웃 5분으로 증가(마루마루 사이트의 느린 InputStream을 고려)

--- ver 0.1.2 ---
* 아카이브 주소 파싱 인덱스 오류 수정

--- ver 0.1.1 ---
* 여러편 다운로드시 아카이브 주소가 제대로 파싱되지 않는 문제점 수정

--- ver 0.1.0 ---
* 한 편씩 다운로드 & 여러편 다운로드를 "만화 다운로드" 기능 하나로 합침 -> 프로그램이 알아서 판단하여, 한 편만 있으면 한편만 다운 or 여러편이 있으면 전체 다운로드 진행
* 여러편 다운로드에 한해서 부모폴더생성 지원 ex) C:\Marumaru\원피스\원피스 337화\
* 기존 wasabisyrup 등이 들어간 아카이브 주소만 다운로드 되던 것을 "아무 주소"나 넣어도 다운로드가 가능하게 개선

--- ver 0.0.1 ---
* 한편씩 다운로드 기능 지원
* 다운로드 폴더 열기 기능 지원
