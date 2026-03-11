package com.chhavi.busbuddy_backend.persistence.model;

import java.util.List;

public class ForwardBackStops {
    private List<Stop> forwardStops;
    private List<Stop> backStops;

    // Constructor for deserialization
    public ForwardBackStops() {
    }

    public List<Stop> getForwardStops() {
        return forwardStops;
    }

    public void setForwardStops(List<Stop> forwardStops) {
        this.forwardStops = forwardStops;
    }

    public List<Stop> getBackStops() {
        return backStops;
    }

    public void setBackStops(List<Stop> backStops) {
        this.backStops = backStops;
    }

    @Override
    public String toString() {
        return "ForwardBackStops{" +
                "forwardStop=" + forwardStops +
                ", backStops=" + backStops +
                '}';
    }

}
