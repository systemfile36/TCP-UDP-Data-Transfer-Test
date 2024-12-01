package org.limdongha.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * CSV 파일을 편리하게 작성하기 위한 클래스 <br/>
 * write 할 모든 클래스는 CsvConvertible 을 구현하고,
 * 열의 갯수가 일치해야 하며, 열 이름을 반환할 수 있어야 한다. (열 이름 일치는 검사하지 않음)
 * @author limdongha
 */
public class CsvWriter<T extends CsvConvertible> implements Closeable, AutoCloseable, Flushable {

    private final BufferedWriter writer;

    public CsvWriter(Path path) throws IOException {
        //UTF-8 인코딩으로 파일을 생성한다.
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
    }

    public CsvWriter(String path) throws IOException {
        this(Path.of(path));
    }

    /**
     * 파일에 List의 모든 내용을 CSV 형식으로 write 한다.
     *
     * @param col 파일로 출력할 CsvConvertible한 객체들의 List
     * @throws IOException - CSV 형식에 맞지 않을 경우, 또는 입출력 중 오류 발생
     */
    public void writeAll(List<T> col) throws IOException {

        if(!validate(col)) {
            throw new IOException("invalidate csv");
        }

        writer.write(String.join(",", col.get(0).getColumnNames()) + "\n");

        for(CsvConvertible value : col) {
            writer.write(value.toCsvString());
        }
    }


    private boolean validate(List<T> col) {
        //비어있는지 체크
        if(col.isEmpty()) return false;

        //열 갯수
        long columnCount = col.get(0).getColumnCount();

        //열 갯수가 모두 일치하면 true, 아니면 false
        boolean columnCheck = col.stream()
                .noneMatch((value)->value.getColumnCount() != columnCount);

        //열 이름의 개수가 열 갯수와 일치하면 true, 아니면 false
        boolean headerCheck = col.get(0).getColumnNames().length == columnCount;

        return columnCheck && headerCheck;
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }
}
