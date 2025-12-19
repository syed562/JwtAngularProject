package com.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickets")
public class Ticket {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int ticketId;

	@Column(name = "pnr", unique = true, nullable = false)
	private String pnr;

	@Column(name = "flight_id", nullable = false)
	private int flightId;

	@Column(name = "passenger_id", nullable = false)
	private int passengerId;

	@Column(name = "number_of_seats", nullable = false)
	private int numberOfSeats;

	@Column(name = "booked")
	private boolean booked;
}
