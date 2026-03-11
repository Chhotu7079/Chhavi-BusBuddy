package com.chhavi.busbuddy_backend.persistence.model;

import com.chhavi.busbuddy_backend.util.FirestoreDocumentMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Route {
    @DocumentId
    private String id;
    private String company;
    private String code;
    private Schedule timetable;
    private Map<String, Schedule> history;
    private Schedule delays;
    private ForwardBackStops stops;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Schedule getTimetable() {
        return timetable;
    }

    public void setTimetable(Schedule timetable) {
        this.timetable = timetable;
    }

    public Map<String, Schedule> getHistory() {
        return history;
    }

    public void setHistory(Map<String, Schedule> history) {
        this.history = history;
    }

    public ForwardBackStops getStops() {
        return stops;
    }

    public void setStops(ForwardBackStops stops) {
        this.stops = stops;
    }

    public Schedule getDelays() {
        return delays;
    }

    public void setDelays(Schedule delays) {
        this.delays = delays;
    }

    public Route() {
    }

    public ForwardBackStops buildStopOutboundReturn(DocumentSnapshot document) throws InterruptedException, ExecutionException {
        List<Stop> outboundStops = buildStopList(document, "stops.forward");
        List<Stop> returnStops = buildStopList(document, "stops.back");

        ForwardBackStops stops = new ForwardBackStops();
        stops.setForwardStops(outboundStops);
        stops.setBackStops(returnStops);
        return stops;
    }

    private List<Stop> buildStopList(DocumentSnapshot document, String fieldPath) throws InterruptedException, ExecutionException {
        List<DocumentReference> stopRefs = FirestoreDocumentMapper.getReferenceList(document, fieldPath);
        List<Stop> stops = new ArrayList<>();
        if (stopRefs != null) {
            for (DocumentReference stopRef : stopRefs) {
                Stop stop = stopRef.get().get().toObject(Stop.class);
                stop.setId(stopRef.getId());
                stops.add(stop);
            }
        }
        return stops;
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", company='" + company + '\'' +
                ", code='" + code + '\'' +
                ", stops=" + stops +
                ", timetable=" + timetable +
                ", history=" + history +
                '}';
    }
}

