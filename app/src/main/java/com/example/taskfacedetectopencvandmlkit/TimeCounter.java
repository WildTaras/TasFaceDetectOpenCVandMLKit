package com.example.taskfacedetectopencvandmlkit;

import java.util.Calendar;

public class TimeCounter {

    private long startTime;
    private long endTime;
    private long duration;

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public void endTime() {
        endTime = System.currentTimeMillis();
    }

    public String getMillisecondDuration() {
        duration = endTime - startTime;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(duration);
        return "Millisecond: " + cal.get(Calendar.MILLISECOND);
    }

    public long getMillisecondCounter() {
        duration = endTime - startTime;
        startTime = 0;
        endTime = 0;
        return duration;
    }

}
