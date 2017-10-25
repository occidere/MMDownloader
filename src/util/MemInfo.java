package util;

import java.text.DecimalFormat;
import java.lang.management.ManagementFactory;
//import java.lang.management.MemoryMXBean;
//import java.lang.management.MemoryUsage;

/**
 * 개발 참고용 메모리 관리 클래스
 * 2017.10.24.화
 * @author occidere
 */
public class MemInfo {
//	private static MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
//	private static MemoryUsage heap = memBean.getHeapMemoryUsage();
//	private static MemoryUsage nonheap = memBean.getNonHeapMemoryUsage();
	
	/**
	 * 현재 Heap Usage, NonHeap Usage를 출력
	 * (crlf 없음)
	 */
	public static void printMemInfo() {
		DecimalFormat format = new DecimalFormat("###,###,###.##");
		System.out.printf("(Heap Used: %s, NonHeap Used: %s)", 
				format.format(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()),
				format.format(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()));
	}
}