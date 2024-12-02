package org.limdongha.util;


import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 테스트를 위해 사용할 정적 유틸 클래스
 * @author limdongha
 */
public class TestUtils {

    /**
     * 테스트를 위한 데이터 사이즈 들의 리스트를 반환한다.
     * 500에서 65535 사이의 값들이 담긴다.
     * @return - 테스트할 데이터 단위 크기 리스트
     */
    public static List<Integer> getDataSizes() {

        List<Integer> list = new ArrayList<>();
        //최대 크기 지정
        final int maxDataSize = 65535;

        int dataSize = 500;
        list.add(dataSize);

        //dataSize 최대 크기와 같아지기 까지 반복
        while(dataSize < maxDataSize) {
            //dataSize 두배 씩 늘려가며 리스트에 삽입
            dataSize = Math.min(dataSize * 2, maxDataSize);
            list.add(dataSize);
        }

        return list;
    }

    /**
     * 표준 입출력에서 서버의 IP 주소와 포트 번호를 받아서 InetSocketAddress를 반환한다.
     * 소켓 생성 시 사용한다.
     * @return - 사용자로 부터 입력받은 IP 주소와 포트 번호 (InetSocketAddress)
     */
    public static InetSocketAddress getSocketAddressFromStdIn() {
        String serverIp;
        int port;
        while(true) {
            try(Scanner sc = new Scanner(System.in)) {

                System.out.print("서버 IP 주소와 포트 번호를 입력해주세요. ex) 127.0.0.1 5000 \n: ");
                String[] input = sc.nextLine().split(" ");

                //flush '\n'
                sc.nextLine();

                if (input.length != 2) throw new IllegalArgumentException("잘못된 입력입니다.");

                //파싱
                serverIp = input[0];
                port = Integer.parseInt(input[1]);

                System.out.printf("IP : %s, Port : %d\n", serverIp, port);

                return new InetSocketAddress(serverIp, port);

            } catch (NumberFormatException e) {
                System.out.println(e.getMessage() + "\n포트 번호 형식이 잘못되었습니다.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 생성할 파일 이름(경로)와 파일 길이를 받아서, 랜덤한 값을 가진 테스트용 파일을 생성한다.
     * @param fileName - 생성할 파일 명(경로)
     * @param byteLength - 생성할 파일의 길이(바이트 단위)
     * @return - 성공 여부
     */
    public static boolean generateFile(String fileName, long byteLength) {

        boolean isSuccess = false;

        try(FileOutputStream fos = new FileOutputStream(fileName)) {
            //버퍼 사이즈 (100MB)
            int bufferSize = 1024 * 1024 * 100;
            byte[] buffer = new byte[bufferSize];

            //랜덤값 생성
            for(int i = 0; i < buffer.length; i++) {
                buffer[i] = (byte)(Math.random() * 255);
            }

            //쓴 바이트 수
            long totalSavedBytes = 0;

            //쓴 바이트 수와 총 바이트 수보다 작을 동안 반복
            while(totalSavedBytes < byteLength) {
                //버퍼 사이즈와 남은 용량 중 작은 쪽을 쓸 바이트 수로 지정한다.
                int bytesToSave = (int)Math.min(bufferSize, byteLength - totalSavedBytes);

                //지정한 바이트 수 만큼 쓴다.
                fos.write(buffer, 0, bytesToSave);

                //쓴 바이트 수 갱신
                totalSavedBytes += bytesToSave;
            }

            fos.flush();

            //성공
            isSuccess = true;

        } catch(FileNotFoundException e) {
            System.err.println(e.getMessage() +
                    "\n파일 이름이 잘못되었거나, 파일을 열 수 없습니다.");
        } catch(IOException e) {
            System.err.println(e.getMessage());
        }

        return isSuccess;
    }


    /**
     * IP 주소를 받아서 해당 IP에 대해 tracert를 실행한다.
     * 종료될 때까지 블로킹한 후, 파일로 출력한다.<br/>
     * 실험 환경을 출력하기 위함이다.
     * @param ipAddress - tracert를 실행할 IP 주소
     */
    public static void execTraceRouteAndWait(String ipAddress) {
        try {
            Process process = Runtime.getRuntime().exec("tracert " + ipAddress);

            //명령어를 실행한 프로세스의 출력을 입력 스트림으로 가져온 후, 문자 보조스트림과 버퍼 스트림으로 감싼다.
            //파일 출력 스트림을 받아온다.
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedWriter writer = Files.newBufferedWriter(Path.of("tracertResult.txt"), StandardCharsets.UTF_8)) {

                String line;
                while((line = reader.readLine()) != null) {
                    writer.write(line + "\n");
                }

            }

            int exitCode = process.waitFor();
            System.out.println("Exit code : " + exitCode);

        } catch (Exception e) {
            System.err.println("오류 발생... : " + e.getMessage());
        }
    }

}
