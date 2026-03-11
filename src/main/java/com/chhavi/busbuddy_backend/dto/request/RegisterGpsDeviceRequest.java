package com.chhavi.busbuddy_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RegisterGpsDeviceRequest {

    @NotBlank(message = "must not be blank")
    private String imei;

    @NotBlank(message = "must not be blank")
    private String busId;

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }
}
