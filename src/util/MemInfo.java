package util;

import ch.qos.logback.classic.Logger;
import common.MaruLoggerFactory;

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

	private static final Logger print = MaruLoggerFactory.getPrintLogger();
	
	/**
	 * 현재 Heap Usage, NonHeap Usage를 출력
	 * (crlf 없음)
	 */
	public static void printMemInfo() {
		DecimalFormat format = new DecimalFormat("###,###,###.##");
		print.info("(Heap Used: {}, NonHeap Used: {})",
				format.format(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()),
				format.format(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()));
	}
}