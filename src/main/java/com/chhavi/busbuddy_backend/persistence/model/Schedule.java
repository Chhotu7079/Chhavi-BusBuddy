package com.chhavi.busbuddy_backend.persistence.model;
import java.util.List;
import java.util.Map;

public class Schedule {
    private Map<String, List<String>> forward;
    private Map<String, List<String>> back;

    public Schedule() {
    }

    public Map<String, List<String>> getForward() {
        return forward;
    }

    public void setForward(Map<String, List<String>> forward) {
        this.forward = forward;
    }

    public Map<String, List<String>> getBack() {
        return back;
    }

    public void setBack(Map<String, List<String>> back) {
        this.back = back;
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "forward=" + forward +
                ", back=" + back +
                '}';
    }
}
