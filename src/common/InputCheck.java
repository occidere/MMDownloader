package common;

/**
 * 입력 값이 원하는 형식인지 검증하는 클래스. 
 * Static Way로 사용할 것
 * @author occidere
 */
public class InputCheck {
	/** static class이기 때문에 객체 생성 불가 */
	private InputCheck() {}
	
	/** 숫자가 포함되지 않은 경우 */
	public static final int NON_NUMERIC = 0x00;
	/** 숫자만 존재하는 경우 */
	public static final int ONLY_NUMBER = 0x01;
	/** 알파벳만 존재하는 경우 */
	public static final int ONLY_ALPHABET = 0x02;
	/** HTTP URL 주소 형식인지 */
	public static final int IS_HTTP_URL = 0x03;
	
	/** TYPE 별 패턴을 저장한 테이블 */
	private static String patternTable[] = {
		"^\\D+$",						// NON_NUMERIC
		"^\\d+$",						// ONLY_NUMBER
		"^[a-zA-Z]+$",					// ONLY_ALPHABET
		"^(http)s?://\\S+$"				// IS_HTTP_URL
	};
	
	/**
	 * 인풋값이 원하는 TYPE의 입력인지 검증하는 메서드
	 * @param input 입력값
	 * @param TYPE 검증할 타입
	 * @return 올바른 타입의 입력이면 true, 아니면 false
	 */
	public static boolean isValid(String input, final int TYPE) {
		boolean isMatched = false;
		try { 
			isMatched = input.matches(patternTable[TYPE]);
		}
		catch(Exception e) {
			ErrorHandling.printError("No Such Pattern Type Exception", false);
		}
		return isMatched;
	}
}