package org.limdongha.util;

/**
 * 시간 측정을 위한 클래스
 * @author limdongha
 */
public class Timer {
    private long startTime = 0;

    public Timer() {}

    /**
     * 시간 측정을 시작한다.
     * @return - 이미 시작되었을 경우 false, otherwise true
     */
    public boolean start() {
        if(this.startTime != 0) {
            return false;
        } else {
            this.startTime = System.currentTimeMillis();
            return true;
        }
    }

    /**
     * 시간 측정을 멈추고 측정된 시간을 반환한다. <br/>
     * 반환 후 초기화한다.
     * @return - 측정된 시간
     */
    public long stop() {
        if(this.startTime == 0) {
            return 0;
        } else {
            long temp = System.currentTimeMillis() - this.startTime;
            this.startTime = 0;
            return temp;
        }
    }

    /**
     * 시간을 초기화 한다.
     */
    public void reset() {
        this.startTime = 0;
    }
}
