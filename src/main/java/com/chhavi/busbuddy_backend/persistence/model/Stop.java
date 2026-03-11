package com.chhavi.busbuddy_backend.persistence.model;

import com.google.cloud.firestore.GeoPoint;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.Objects;

public class Stop {
    @DocumentId
    private String id;
    private String name;
    private String address;
    private GeoPoint coords;

    // Constructor for deserialization
    public Stop() {
    }

    public Stop(String id, String name, String address, GeoPoint coords) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.coords = coords;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GeoPoint getCoords() {
        return coords;
    }

    public void setCoords(GeoPoint coords) {
        this.coords = coords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stop stop = (Stop) o;
        return Objects.equals(address, stop.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return "Stop{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", coords=" + coords +
                '}';
    }
}
