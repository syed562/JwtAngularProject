package com.example.event;

public class TicketBookedEvent {

    private String email;
    private String pnr;
    private int flightId;
    private int seats;

    public TicketBookedEvent() {}

    public TicketBookedEvent(String email, String pnr, int flightId, int seats) {
        this.email = email;
        this.pnr = pnr;
        this.flightId = flightId;
        this.seats = seats;
    }

    public String getEmail() {
        return email;
    }

    public String getPnr() {
        return pnr;
    }

    public int getFlightId() {
        return flightId;
    }

    public int getSeats() {
        return seats;
    }
}
