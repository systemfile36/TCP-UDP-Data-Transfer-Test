package org.limdongha.tcp.server;

import org.limdongha.util.CsvConvertible;
import org.limdongha.util.CsvWriter;
import org.limdongha.util.TestUtils;
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

    public static void main(String[] args) {
        final int port = 5000;

        System.out.println("If you want close server, press 'q' and enter");


        List<TestRecord> testRecordList = new ArrayList<>();

        System.out.println("서버 시작 중...");

        //서버 시작
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server running on " + port);

            //키보드 입력을 감지해서, 소켓을 강제로 종료시키는 스레드
            Thread thread = TestUtils.getCloseControlThread(serverSocket);
            thread.start();

            //서버가 종료될 때까지 무한 반복
            while(true) {
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

            try(CsvWriter<TestRecord> csvWriter = new CsvWriter<>(TestUtils.getFormattedDateTime() + "testResult_server.csv")) {
                csvWriter.writeAll(testRecordList);
                csvWriter.flush();
            } catch(IOException e) {
                System.err.println("결과 작성 중 오류 발생...");
                return;
            }

            System.out.println("결과 작성 완료!");
        }



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
