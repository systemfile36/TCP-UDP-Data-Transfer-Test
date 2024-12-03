package org.limdongha.udp.client;

import org.limdongha.tcp.client.TcpClient;
import org.limdongha.util.CsvConvertible;
import org.limdongha.util.CsvWriter;
import org.limdongha.util.TestUtils;
import org.limdongha.util.Timer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 실험을 위한 UDP 클라이언트
 */
public class UdpClient {
    public static void main(String[] args) {

        final int port = 5500;

        //접속할 서버의 아이피와 포트 번호를 사용자에게서 입력 받음
        InetSocketAddress socketAddress = TestUtils.getSocketAddressFromStdIn();

        //접속할 서버에 대해 `tracert`를 실행
        TestUtils.execTraceRouteAndWait(socketAddress.getHostString());

        //테스트할 데이터 단위 모음
        List<Integer> dataSizes = TestUtils.getDataSizes();

        //전송할 데이터의 바이트 수 (1GB)
        long totalBytes = 1024L * 1024 * 1024;

        final String testFileName = "testFile.bin";

        byte[] buffer = new byte[65536];

        //테스트용 파일 생성
        TestUtils.generateFile(testFileName, totalBytes);

        //실험 결과를 저장할 리스트
        List<TestRecord> testRecordList = new ArrayList<>();

        System.out.println("실험 시작...");

        //UDP 소켓을 열고, 읽어올 파일을 버퍼 스트림으로 불러온다.
        //서버로 부터의 응답을 받기 위해 클라이언트도 포트를 개방한다.
        try(DatagramSocket socket = new DatagramSocket(port);
            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(testFileName))) {

            //서버로 부터 응답을 받을 때 대기할 시간.
            socket.setSoTimeout(5000);

            String format = "데이터  단위 크기 : %d 바이트\n";

            for(int dataSize : dataSizes) {
                System.out.println("----------------------------------------");
                System.out.printf(format, dataSize);

                //총 보낸 바이트 수
                long totalSentBytes = 0;

                LocalDateTime sendAt = LocalDateTime.now();

                //보낼 용량만큼 대 보낼 때까지
                while(totalSentBytes < totalBytes) {
                    //보내는 단위와 남은 보내야할 바이트 중 최소를 선택
                    int bytesToSend = (int) Math.min(dataSize, totalBytes - totalSentBytes);

                    //보낼 만큼 파일에서 읽어서 버퍼에 넣는다.
                    reader.read(buffer, 0, bytesToSend);

                    //읽어온 만큼 버퍼에서 읽어서 DatagramPacket 을 서버에 보낸다.
                    DatagramPacket packet = new DatagramPacket(buffer, 0, bytesToSend, socketAddress.getAddress(), socketAddress.getPort());
                    socket.send(packet);

                    //읽어온 만큼 누적
                    totalSentBytes += bytesToSend;
                }

                //종료 신호 메시지를 패킷으로 구성해서 보낸다.
                byte[] endMessage = TestUtils.END_MESSAGE.getBytes();
                DatagramPacket endPacket = new DatagramPacket(endMessage, endMessage.length, socketAddress.getAddress(), socketAddress.getPort());

                try {
                    //20초 대기
                    Thread.sleep(20000);
                } catch(InterruptedException e) {

                }

                //확인 응답이 올 때까지 설정된 timeout 만큼 대기한다.
                //만약 응답이 오지않으면 다시 전송하고 대기한다.
                while(true) {
                    try {
                        socket.send(endPacket);

                        byte[] ackBuffer = new byte[255];
                        DatagramPacket response = new DatagramPacket(ackBuffer, ackBuffer.length);
                        System.out.println("확인 응답 대기 중...");
                        socket.receive(response);
                        String message = new String(response.getData());

                        if(message.contains(TestUtils.ACK_MESSAGE)) break;
                    } catch(SocketTimeoutException e) {
                        System.out.println("Timeout, 완료 메시지를 재전송합니다...");
                        continue;
                    }
                }

                TestRecord record = new TestRecord(sendAt, dataSize, totalSentBytes);
                testRecordList.add(record);

                System.out.printf("총 전송 데이터 : %d\n", totalSentBytes);
                System.out.println("----------------------------------------");
            }

        } catch(IOException e) {
            System.err.println("실험 비정상 종료...");
            e.printStackTrace();
        }

        System.out.println("실험 종료. 결과 파일 작성 중...");

        try(CsvWriter<TestRecord> csvWriter = new CsvWriter<>(TestUtils.getFormattedDateTime() + "testResult_UdpClient.csv")) {
            csvWriter.writeAll(testRecordList);
            csvWriter.flush();
        } catch(IOException e) {
            System.err.println("결과 작성 중 오류 발생...");
            return;
        }

        System.out.println("결과 작성 완료!");
    }

    public record TestRecord(LocalDateTime sendAt, int dataSize, long totalBytes) implements CsvConvertible {

        @Override
        public String toCsvString() {
            return sendAt.toString() + "," + dataSize + "," + totalBytes + "\n";
        }

        @Override
        public String[] getColumnNames() {
            String[] temp = new String[3];
            temp[0] = "Send at";
            temp[1] = "Data Size";
            temp[2] = "Total Bytes";
            return temp;
        }
    }
}
