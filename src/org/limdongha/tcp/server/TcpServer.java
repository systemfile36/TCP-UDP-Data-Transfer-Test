package org.limdongha.tcp.server;

import org.limdongha.util.CsvConvertible;
import org.limdongha.util.CsvWriter;
import org.limdongha.util.Timer;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 테스트를 위한 TCP 서버
 * @author limdongha
 */
public class TcpServer {
    //종료 제어를 위한 스레드-세이프한 원자값 객체
    private static final AtomicBoolean stop = new AtomicBoolean(false);

    public static void main(String[] args) {
        final int port = 5000;

        System.out.println("If you want close server, press 'q' and enter");


        List<TestRecord> testRecordList = new ArrayList<>();

        System.out.println("서버 시작 중...");

        //서버 시작
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on " + port);

            //키보드 입력을 감지해서, 소켓을 강제로 종료시키는 스레드
            Thread thread = getCloseControlThread(serverSocket);
            thread.start();

            //서버가 종료될 때까지 무한 반복
            while(!stop.get()) {
                //연결 대기...
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결 : " + clientSocket.getInetAddress());

                InputStream input = clientSocket.getInputStream();

                long totalBytes = 0;
                byte[] buffer = new byte[65536]; //65535 + 1

                //매 반복 읽은 단위
                int bytesRead;

                Timer timer = new Timer();
                timer.start();

                //더 이상 수신할 데이터가 없을 때 까지
                while((bytesRead = input.read(buffer)) != -1) {
                    //읽은 바이트 누적
                    totalBytes += bytesRead;
                }

                long endTime = timer.stop();

                //레코드 생성
                TestRecord record = new TestRecord(LocalDateTime.now(), endTime, totalBytes);
                testRecordList.add(record);

                System.out.println(record.toCsvString());

                //전송 완료 시 종료
                clientSocket.close();
            }

        } catch (IOException e) {
            System.err.println("서버 비정상 종료...");
            e.printStackTrace();
        } finally {
            System.out.println("실험 종료. 결과 파일 작성 중...");

            try(CsvWriter<TestRecord> csvWriter = new CsvWriter<>("testResult_server.csv")) {
                csvWriter.writeAll(testRecordList);
                csvWriter.flush();
            } catch(IOException e) {
                System.err.println("결과 작성 중 오류 발생...");
                return;
            }

            System.out.println("결과 작성 완료!");
        }



    }

    /**
     * 키보드에서 특정 입력이 감지되면 서버를 종료하는 스레드를 반환한다.
     * @return 스레드 객체.
     */
    private static Thread getCloseControlThread(ServerSocket serverSocket) {
        Thread thread = new Thread(() -> {
            try(Scanner scanner = new Scanner(System.in)) {
                while(!stop.get()) {
                    if(scanner.hasNextLine()) {
                        String input = scanner.nextLine();
                        if("q".equalsIgnoreCase(input)) {
                            stop.compareAndSet(false, true);
                            //서버소켓 강제로 닫기
                            serverSocket.close();
                            System.out.println("서버가 종료됩니다...");
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        //데몬 스레드로 설정하고 시작
        thread.setDaemon(true);
        return thread;
    }

    public record TestRecord(LocalDateTime receivedAt, long elapsedTime, long totalBytes) implements CsvConvertible {

        @Override
        public String toCsvString() {
            return receivedAt.toString() + "," + elapsedTime + "," + totalBytes + "\n";
        }

        @Override
        public String[] getColumnNames() {
            String[] temp = new String[3];
            temp[0] = "Received At";
            temp[1] = "Elapsed Time";
            temp[2] = "Total Bytes";
            return temp;
        }
    }

}
