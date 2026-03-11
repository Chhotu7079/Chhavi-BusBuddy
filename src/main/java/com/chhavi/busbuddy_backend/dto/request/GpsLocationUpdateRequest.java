package com.chhavi.busbuddy_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Generic GPS location update payload.
 *
 * This is designed to be usable both for direct device -> backend and vendor webhook -> backend.
 */
public class GpsLocationUpdateRequest {

    @NotBlank(message = "must not be blank")
    private String imei;

    private Double latitude;
    private Double longitude;

    private Double speed;

    private String direction;

    private Integer lastStop;

    /**
     * Optional timestamp in epoch millis as supplied by device/vendor.
     */
    private Long deviceTimestamp;

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Integer getLastStop() {
        return lastStop;
    }

    public void setLastStop(Integer lastStop) {
        this.lastStop = lastStop;
    }

    public Long getDeviceTimestamp() {
        return deviceTimestamp;
    }

    public void setDeviceTimestamp(Long deviceTimestamp) {
        this.deviceTimestamp = deviceTimestamp;
    }
}
