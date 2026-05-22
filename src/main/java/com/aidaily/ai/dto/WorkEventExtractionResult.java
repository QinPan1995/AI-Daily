package com.aidaily.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkEventExtractionResult {

    private List<ExtractedWorkEventDto> events = new ArrayList<ExtractedWorkEventDto>();

    public List<ExtractedWorkEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<ExtractedWorkEventDto> events) {
        this.events = events;
    }
}
