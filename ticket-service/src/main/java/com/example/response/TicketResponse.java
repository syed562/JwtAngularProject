package com.example.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
	private int id;
	private String name;
	private String email;
	private String origin;
	private String destination;
	private String pnr;

	private int numberOfSeats;

	private LocalDateTime departureTime;
	private LocalDateTime arrivalTime;
	private boolean booked;
}
