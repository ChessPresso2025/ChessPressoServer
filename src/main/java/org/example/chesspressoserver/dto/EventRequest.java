package org.example.chesspressoserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class EventRequest {

    @NotBlank(message = "Event-Type ist erforderlich")
    private String type;

    @NotNull(message = "Payload ist erforderlich")
    private Map<String, Object> payload;

    // Getter und Setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
