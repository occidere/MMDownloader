package sys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MMDownloader의 다운로드 기록 저장용 Database
 * Static Way 로 사용할 것
 *
 * @author occidere
 * @since 2018-05-06
 */
public class Database {
    /* Database로 사용할 Map 객체 */
    private static Map<String, String> database;
    /* DB 주기별 자동저장에 사용할 스레드 풀 */
    private static final ScheduledExecutorService DB_EXECUTOR = Executors.newScheduledThreadPool(1);
    /* 자동저장 Executor가 오직 1개만 생성될 수 있도록 검증하는 AtomicBoolean 객체 */
    private static AtomicBoolean executorRunning = new AtomicBoolean(false);

    /* DB 경로 및 DB 파일 이름, DB 파일의 절대경로 */
    private static final String DB_PATH = SystemInfo.DEFAULT_PATH + "/db";
    private static final String DB_NAME = "MMDownloader.db";
    private static final String DB_FULL_NAME = DB_PATH + "/" + DB_NAME;

    /* Static Way 방식 */
    private Database() {}

    /**
     * Database 객체 초기화.
     * 반드시 맨 처음에 호출할 것
     */
    public static void initDatabase() {
        if(database == null && executorRunning.get() == false) {
            new File(DB_PATH).mkdirs(); // 폴더가 없을 시 새로 생성

            database = getDatabase();
            autosave(3); // 3초마다 자동저장
        }
    }

    /**
     * DB에 새로운 만화 다운로드 정보를 추가.
     * {@code comicId}가 이미 존재하는 경우 그냥 종료
     * @param comicId 추가할 만화의 id
     * @param comicTitle 추가할 만화의 article-title (원피스 21화 등)
     */
    public static void insert(String comicId, String comicTitle) {
        comicId = comicId.trim();
        comicTitle = comicTitle.trim();
        if(database.containsKey(comicId) == false) {
            database.put(comicId, comicTitle);
        }
    }

    /**
     * {@code comicIde}를 기준으로 기존 정보를 업데이트 하거나, 새로 추가. (UPdate + inSERT = UPSERT)
     * 이미 {@code comicId}가 존재하면 새로운 값으로 덮어씌우고, 없으면 새로 추가한다.
     * @param comicId upsert 할 만화의 id
     * @param comicTitle upsert 할 만화의 article-title (원피스 21화 등)
     */
    public static void upsert(String comicId, String comicTitle) {
        comicId = comicId.trim();
        comicTitle = comicTitle.trim();
        database.put(comicId, comicTitle);
    }

    /**
     * {@comicId}값으로 저장된 만화의 제목을 가져온다.
     * 저장 내역이 없을 시 공백("")을 반환한다.
     * @param comicId 만화 제목을 추출할 만화의 id
     * @return 추출된 만화의 article-title(원피스 25화 등)
     */
    public static String getComicTitle(String comicId) {
        return database.getOrDefault(comicId, "");
    }

    /**
     * 만회의 id를 기준으로 다운로드 기록을 삭제한다.
     * id가 있어서 삭제에 성공하면 true, 없거나 실패 시 false
     * @param comicId 기록을 삭제할 만회의 id
     * @return 삭제 성공 시 true, id가 없거나 실패 시 false
     */
    public static boolean deleteById(String comicId) {
        comicId = comicId.trim();
        return database.remove(comicId) == null ? false : true;
    }

    /**
     * 만화 id를 기준으로 다운로드 받은 적이 있는지를 확인한다.
     * @param comicId 다운기록을 조회할 만화의 아카이브 id
     * @return DB에 기록이 되어 있으면 true, 없으면 false
     */
    public static boolean contains(String comicId) {
        return database.containsKey(comicId);
    }

    /**
     * 현재 db의 다운로드 기록 데이터의 개수를 반환
     * @return 기록된 다운로드 데이터의 개수
     */
    public static int size() {
        return database.size();
    }

    /**
     * DB에 저장된 내용을 보여줌
     * @return DB의 저장 현황 문자열 목록
     */
    public static String getDatabaseToString() {
        int num = 1;
        StringBuilder dbStr = new StringBuilder();
        for(Map.Entry<String, String> entry : database.entrySet()) {
            dbStr.append(String.format("%,03d. %s(%s)\n", num, entry.getValue(), entry.getKey()));
            num++;
        }
        return dbStr.toString();
    }

    /**
     * Database로 사용할 HashMap 객체를 deserialize 하여 불러온다.
     * @return db용 HashMap 객체
     */
    private static Map<String, String> getDatabase() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DB_FULL_NAME))){
            return (HashMap) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            return new HashMap<>(); // DB 파일이 없는 경우 새로 만듦
        }
    }

    /**
     * DB write 기능.
     */
    private static synchronized void writeDatabase() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DB_FULL_NAME))) {
            oos.writeObject(database);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 비동기 스레드 db 자동저장 기능
     * 한번만 호출되야 한다.
     */
    private static void autosave(int periodSecond) {
        if(executorRunning.compareAndSet(false, true)) {
            DB_EXECUTOR.scheduleAtFixedRate(()-> {
                writeDatabase();
            }, 0, periodSecond, TimeUnit.SECONDS);
        }
    }

    /**
     * 모든 자원을 반환
     */
    public static void close() {
        DB_EXECUTOR.shutdown();
        executorRunning.set(false);
        writeDatabase();
        database = null;
    }
}