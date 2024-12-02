package org.limdongha.udp.server;

import org.limdongha.tcp.server.TcpServer;
import org.limdongha.util.CsvConvertible;
import org.limdongha.util.CsvWriter;
import org.limdongha.util.TestUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UdpServer {
    public static void main(String[] args) {
        final int port = 5001;

        List<TestRecord> testRecordList = new ArrayList<>();

        //받아서 저장할 파일 이름
        final String receiveFileName = "testFile_received.bin";

        //클라이언트에서 보낸 원본 파일의 바이트 수
        final long origin_totalBytes = 1024L * 1024 * 1024;

        System.out.println("서버 시작 중...");

        try(DatagramSocket socket = new DatagramSocket(port)) {

            System.out.println("Server running on " + port);

            //키보드 입력을 감지해서, 소켓을 강제로 종료시키는 스레드
            Thread thread = TestUtils.getCloseControlThread(socket);
            thread.start();

            //서버가 종료될 때까지 계속 데이터를 받을 준비를 한다.
            while(true) {

                byte[] buffer = new byte[65536];

                //수신받은 데이터를 저장할 파일 출력 스트림
                //BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(receiveFileName));

                long totalBytes = 0;

                //종료 신호를 받을 때까지 데이터를 받는다.
                while(true) {

                    //패킷을 받는다. (블로킹)
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    //종료 메시지를 받으면 현재 반복을 종료하고, 다시 다음 데이터 전송 시작을 기다린다.
                    String message = new String(packet.getData());

                    if(message.contains("END")) {
                        System.out.println("데이터 수신 완료");
                        byte[] ack = TestUtils.ACK_MESSAGE.getBytes();
                        DatagramPacket ackResponse = new DatagramPacket(ack, 0, ack.length, packet.getAddress(), packet.getPort());
                        socket.send(ackResponse);
                        break;
                    }

                    //패킷으로 받은 만큼만 파일에 쓴다.
                    //writer.write(buffer, 0, packet.getLength());

                    //읽은 바이트 수를 누적한다.
                    totalBytes += packet.getLength();
                }

                LocalDateTime receivedAt = LocalDateTime.now();

                //파일 출력 스트림을 닫는다.
                //writer.close();

                //데이터를 모두 받았다면 손실된 바이트 수를 센다.
                //int errorBytes = 0;

                //수신받은 데이터를 다시 불러온다.
                /*
                try(BufferedInputStream reader = new BufferedInputStream(new FileInputStream(receiveFileName))) {

                }
                 */

                //실험 결과를 기록한다.
                long errorBytes = origin_totalBytes - totalBytes;
                TestRecord record = new TestRecord(receivedAt, errorBytes, ((double)errorBytes/origin_totalBytes) * 100.0, totalBytes);
                testRecordList.add(record);

                System.out.println(record.toCsvString());

            }

        } catch(IOException e) {
            System.err.println("서버 비정상 종료...");
            e.printStackTrace();
        } finally {
            System.out.println("실험 종료. 결과 파일 작성 중...");

            try(CsvWriter<TestRecord> csvWriter = new CsvWriter<>("testResult_UdpServer.csv")) {
                csvWriter.writeAll(testRecordList);
                csvWriter.flush();
            } catch(IOException e) {
                System.err.println("결과 작성 중 오류 발생...");
            }

            System.out.println("결과 작성 완료!");
        }
    }

    public record TestRecord(LocalDateTime receivedAt, long errorBytes, double errorRate, long totalBytes) implements CsvConvertible {

        @Override
        public String toCsvString() {
            return receivedAt.toString() + "," + errorBytes + "," + errorRate + "," + totalBytes + "\n";
        }

        @Override
        public String[] getColumnNames() {
            String[] temp = new String[4];
            temp[0] = "Received At";
            temp[1] = "Error Bytes";
            temp[2] = "Error Rate (%)";
            temp[3] = "Total Bytes";
            return temp;
        }
    }
}
