package de.saarland.events.dto;

public class CityEventCountDto {
    private String cityName;
    private long eventCount;

    public CityEventCountDto(String cityName, long eventCount) {
        this.cityName = cityName;
        this.eventCount = eventCount;
    }


    public String getCityName() { return cityName; }
    public long getEventCount() { return eventCount; }
}