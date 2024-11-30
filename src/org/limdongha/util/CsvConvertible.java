package org.limdongha.util;

import java.util.Collection;
import java.util.Optional;

/**
 * CSV 타입으로 직렬화 가능한 클래스가 구현할 인터페이스
 */
public interface CsvConvertible {
    String toCsvString();
    String[] getColumnNames();

    /**
     * 열의 갯수를 반환한다.
     * ","의 개수 + 1로 계산한다.
     * @return 열의 갯수
     */
    default long getColumnCount() {
        return toCsvString().chars()
                .filter(ch -> ch == ',')
                .count() + 1;
    }
}
