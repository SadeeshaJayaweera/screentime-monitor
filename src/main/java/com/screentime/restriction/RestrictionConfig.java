package com.screentime.restriction;
import java.util.ArrayList;
import java.util.List;

public class RestrictionConfig {
    private int dailyLimitMinutes = 480;
    private List<Integer> warningThresholds = new ArrayList<>(List.of(50, 75, 90, 100));
    public int getDailyLimitMinutes() { return dailyLimitMinutes; }
    public void setDailyLimitMinutes(int m) { this.dailyLimitMinutes = m; }
    public List<Integer> getWarningThresholds() { return warningThresholds; }
    public void setWarningThresholds(List<Integer> t) { this.warningThresholds = t; }
}
