package org.limdongha.tcp.client;

import org.limdongha.util.CsvConvertible;
import org.limdongha.util.CsvWriter;
import org.limdongha.util.TestUtils;
import org.limdongha.util.Timer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 실험을 위한 TCP 클라이언트
 * @author limdongha
 */
public class TcpClient {
    public static void main(String[] args) {
        //접속할 서버의 아이피와 포트 번호를 사용자에게서 입력 받음
        InetSocketAddress socketAddress = TestUtils.getSocketAddressFromStdIn();

        //접속할 서버에 대해 `tracert`를 실행
        TestUtils.execTraceRouteAndWait(socketAddress.getHostString());

        //테스트할 데이터 단위 모음
        List<Integer> dataSizes = TestUtils.getDataSizes();

        //전송할 데이터의 바이트 수 (1GB)
        long totalBytes = 1024L * 1024 * 1024;

        byte[] buffer = new byte[65535];

        //0 ~ 255 사이의 값을 buffer 길이까지 반복해서 넣는다.
        for(int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (i % 256);
        }

        //실험 결과 저장하는 리스트
        List<TestRecord> testRecordList = new ArrayList<>();

        System.out.println("실험 시작...");

        try {
            String format = "데이터  단위 크기 : %d 바이트\n";
            //각 데이터 사이즈마다 반복
            for(int dataSize : dataSizes) {

                System.out.println("----------------------------------------");
                System.out.printf(format, dataSize);

                //총 보낸 바이트 수
                long totalSentBytes = 0;

                Timer timer = new Timer();
                timer.start();

                //입력받은 정보로 소켓을 열고, 출력 스트림을 버퍼 보조 스트림으로 가져온다.
                try(Socket socket = new Socket(socketAddress.getHostString(), socketAddress.getPort());
                    BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {

                    //전송할 데이터 만큼 모두 보냈는지 확인할 목적
                    while(totalSentBytes < totalBytes) {
                        //보내는 단위와 남은 보내야할 바이트 중 최소를 선택
                        int bytesToSend = (int) Math.min(dataSize, totalBytes - totalSentBytes);

                        output.write(buffer, 0, bytesToSend);

                        totalSentBytes += bytesToSend;
                    }

                    //버퍼 비우기
                    output.flush();

                    //출력 스트림 닫기. (TCP의 경우 연결 종료 신호를 보냄)
                    socket.shutdownOutput();
                }

                long endTime = timer.stop();

                //레코드 생성
                TestRecord record = new TestRecord(dataSize, endTime, totalSentBytes);
                testRecordList.add(record);

                System.out.printf("전송한 데이터 : %d 바이트\n", record.totalBytes);
                System.out.printf("소요 시간 : %d ms\n", record.elapsedTime);
                System.out.println("----------------------------------------");
            }
        } catch(IOException e) {
            System.err.println("실험 비정상 종료...");
            e.printStackTrace();
            return;
        }

        System.out.println("실험 종료. 결과 파일 작성 중...");

        try(CsvWriter<TestRecord> csvWriter = new CsvWriter<>("testResult_client.csv")) {
            csvWriter.writeAll(testRecordList);
            csvWriter.flush();
        } catch(IOException e) {
            System.err.println("결과 작성 중 오류 발생...");
            return;
        }

        System.out.println("결과 작성 완료!");
    }

    /**
     * 실험 데이터를 저장할 목적의 레코드
     * @param dataSize 테스트한 단위
     * @param elapsedTime 소요 시간
     * @param totalBytes 총 바이트 수
     */
    public record TestRecord(int dataSize, long elapsedTime, long totalBytes)
            implements CsvConvertible {
        @Override
        public String toCsvString() {
            return dataSize + "," + elapsedTime + "," + totalBytes + "\n";
        }

        @Override
        public String[] getColumnNames() {
            String[] temp = new String[3];
            temp[0] = "Data size";
            temp[1] = "Elapsed Time";
            temp[2] = "Total Bytes";
            return temp;
        }
    }
}
