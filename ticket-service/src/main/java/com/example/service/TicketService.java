package com.example.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.event.TicketBookedEvent;
import com.example.exception.ResourceNotFoundException;
import com.example.feign.FlightInterface;
import com.example.feign.PassengerInterface;
import com.example.model.Ticket;
import com.example.repository.TicketRepository;
import com.example.request.BookTicketRequest;
import com.example.response.FlightResponse;
import com.example.response.PassengerDetailsResponse;
import com.example.response.TicketResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class TicketService {

	private TicketRepository ticketRepository;

	private PassengerInterface passengerInterface;
	private FlightInterface flightInterface;
	private KafkaTemplate<String, TicketBookedEvent> kafkaTemplate;
	public TicketService(@Autowired TicketRepository ticketRepository,
            @Autowired PassengerInterface passengerInterface,
            @Autowired FlightInterface flightInterface,
            @Autowired KafkaTemplate<String, TicketBookedEvent> kafkaTemplate) {
this.ticketRepository = ticketRepository;
this.passengerInterface = passengerInterface;
this.flightInterface = flightInterface;
this.kafkaTemplate = kafkaTemplate;
}

	private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

	@Transactional
	public ResponseEntity<String> deleteTicketById(int ticketId) {

	    Ticket ticket = ticketRepository.findById(ticketId)
	            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

	    if (!ticket.isBooked()) {
	        return ResponseEntity.ok("Ticket already cancelled");
	    }

	    ResponseEntity<FlightResponse> response =
	            flightInterface.getByID(ticket.getFlightId());

	    FlightResponse flight = response.getBody();

	    if (flight == null || flight.getDepartureTime() == null) {
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("Unable to fetch flight details");
	    }

	    LocalDateTime departureTime = flight.getDepartureTime();
	    LocalDateTime now = LocalDateTime.now();

	    if (now.plusHours(24).isAfter(departureTime)) {
	        return ResponseEntity
	                .status(HttpStatus.BAD_REQUEST)
	                .body("Ticket cannot be cancelled within 24 hours of departure");
	    }

	    flightInterface.releaseSeats(
	            ticket.getFlightId(),
	            ticket.getNumberOfSeats()
	    );

	    ticket.setBooked(false);
	    ticketRepository.save(ticket);

	    return ResponseEntity.ok("Ticket cancelled successfully");
	}



	@Transactional
	public ResponseEntity<String> bookTicketService(BookTicketRequest req) {

		
		if (req.getNumberOfSeats() <= 0) {
		    throw new IllegalArgumentException("Number of seats must be positive");
		}

		flightInterface.reserveSeats(req.getFlightId(), req.getNumberOfSeats());

		String pnr = UUID.randomUUID().toString().substring(0, 8);

		Ticket ticket = Ticket.builder().pnr(pnr).passengerId(req.getPassengerId()).flightId(req.getFlightId())
				.numberOfSeats(req.getNumberOfSeats()).booked(true).build();

		ticketRepository.save(ticket);

		PassengerDetailsResponse passenger =
		        passengerInterface.getPassengerDetails(req.getPassengerId()).getBody();

		TicketBookedEvent event = new TicketBookedEvent(
		        passenger.getEmail(),
		        pnr,
		        req.getFlightId(),
		        req.getNumberOfSeats()
		);

		try {
		    kafkaTemplate.send("ticket-confirmation", event);
		} catch (Exception ex) {
		    logger.error("Kafka failed for PNR {}: {}", pnr, ex.getMessage());
		}
		return ResponseEntity.ok(pnr);
	}

	@CircuitBreaker(name = "flightService", fallbackMethod = "getByPnrFallback")
	public ResponseEntity<TicketResponse> getByPnrService(String pnr) {

		Ticket ticket = ticketRepository.findByPnr(pnr)
				.orElseThrow(() -> new ResourceNotFoundException("No ticket with this PNR"));

	
		ResponseEntity<PassengerDetailsResponse> passengerResp = passengerInterface
				.getPassengerDetails(ticket.getPassengerId());
		if (passengerResp == null || passengerResp.getBody() == null) {
			throw new ResourceNotFoundException("Passenger details unavailable");
		}
		PassengerDetailsResponse passenger = passengerResp.getBody();

		ResponseEntity<FlightResponse> flightResp = flightInterface.getByID(ticket.getFlightId());
		if (flightResp == null || flightResp.getBody() == null) {
			throw new ResourceNotFoundException("Flight details unavailable");
		}
		FlightResponse flight = flightResp.getBody();

		TicketResponse res = TicketResponse.builder().name(passenger.getName()).email(passenger.getEmail())
				.origin(flight.getOrigin()).destination(flight.getDestination()).pnr(ticket.getPnr())
				.arrivalTime(flight.getArrivalTime()).departureTime(flight.getDepartureTime())
				.numberOfSeats(ticket.getNumberOfSeats()).id(ticket.getTicketId()).booked(ticket.isBooked()).build();

		return ResponseEntity.ok(res);
	}

	public ResponseEntity<TicketResponse> getByPnrFallback(String pnr, Throwable ex) {
		logger.warn("Fallback for getByPnrService for PNR {}: {}", pnr, ex.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
	}

	@CircuitBreaker(name = "passengerService", fallbackMethod = "getTicketsByEmailFallback")
	public ResponseEntity<List<TicketResponse>> getTicketsByEmailService(String email) {

		ResponseEntity<Integer> idResp = passengerInterface.getIdByEmail(email);
		Integer passengerId = (idResp != null) ? idResp.getBody() : null;

		if (passengerId == null)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(List.of());

	
		ResponseEntity<PassengerDetailsResponse> passengerResp = passengerInterface.getPassengerDetails(passengerId);
		if (passengerResp == null || passengerResp.getBody() == null) {
			throw new ResourceNotFoundException("Passenger details unavailable");
		}
		PassengerDetailsResponse passenger = passengerResp.getBody();

		List<Ticket> tickets = ticketRepository.findAllByPassengerId(passengerId);

		List<TicketResponse> responseList = tickets.stream().map(ticket -> {

			// safety check
			ResponseEntity<FlightResponse> fResp = flightInterface.getByID(ticket.getFlightId());
			if (fResp == null || fResp.getBody() == null) {
				throw new ResourceNotFoundException("Flight details unavailable");
			}
			FlightResponse flight = fResp.getBody();

			return TicketResponse.builder().name(passenger.getName()).email(passenger.getEmail())
					.origin(flight.getOrigin()).destination(flight.getDestination()).pnr(ticket.getPnr())
					.arrivalTime(flight.getArrivalTime()).departureTime(flight.getDepartureTime())
					.numberOfSeats(ticket.getNumberOfSeats()).id(ticket.getTicketId()).booked(ticket.isBooked()).build();

		}).toList();

		return ResponseEntity.ok(responseList);
	}

	public ResponseEntity<List<TicketResponse>> getTicketsByEmailFallback(String email, Throwable ex) {
		logger.warn("Fallback for getTicketsByEmailService for email {}: {}", email, ex.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(List.of());
	}
}
