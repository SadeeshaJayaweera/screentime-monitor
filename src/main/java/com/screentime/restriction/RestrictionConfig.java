package com.screentime.restriction;
import java.util.ArrayList;
import java.util.List;

public class RestrictionConfig {
    private int dailyLimitMinutes = 480;
    public int getDailyLimitMinutes() { return dailyLimitMinutes; }
    public void setDailyLimitMinutes(int m) { this.dailyLimitMinutes = m; }
}
