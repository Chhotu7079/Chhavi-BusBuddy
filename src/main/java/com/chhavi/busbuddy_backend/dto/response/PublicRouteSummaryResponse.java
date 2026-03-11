package com.chhavi.busbuddy_backend.dto.response;

public class PublicRouteSummaryResponse {
    private String id;
    private String code;

    public PublicRouteSummaryResponse() {
    }

    public PublicRouteSummaryResponse(String id, String code) {
        this.id = id;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
