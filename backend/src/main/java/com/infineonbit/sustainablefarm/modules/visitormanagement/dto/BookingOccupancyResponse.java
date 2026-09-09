package com.infineonbit.sustainablefarm.modules.visitormanagement.dto;

/**
 * Occupancy summary per activity (mockup: "Occupancy per activity").
 */
public class BookingOccupancyResponse {

    private Long activityId;
    private String activityName;
    private int capacity;
    private int booked;
    private int occupancyPercent;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getBooked() {
        return booked;
    }

    public void setBooked(int booked) {
        this.booked = booked;
    }

    public int getOccupancyPercent() {
        return occupancyPercent;
    }

    public void setOccupancyPercent(int occupancyPercent) {
        this.occupancyPercent = occupancyPercent;
    }
}