package com.example.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.exception.ResourceNotFoundException;
import com.example.model.Flight;
import com.example.repository.FlightRepository;
import com.example.request.FlightRequest;
import com.example.request.SearchRequest;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Transactional
    public void reserveSeats(int flightId, int seats) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        if (flight.getAvailableSeats() < seats) {
            throw new RuntimeException("Not enough seats available");
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - seats);
        flightRepository.save(flight);
    }

    @Transactional
    public void releaseSeats(int flightId, int seats) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        flight.setAvailableSeats(flight.getAvailableSeats() + seats);
        flightRepository.save(flight);
    }

    public ResponseEntity<Integer> registerFlightByIDService(FlightRequest req) {

        Flight flight = Flight.builder()
                .airline(req.getAirline())
                .origin(req.getOrigin())
                .destination(req.getDestination())
                .price(req.getPrice())
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats()) 
                .build();

        Flight savedFlight = flightRepository.save(flight);
        return new ResponseEntity<>(savedFlight.getFlightId(), HttpStatus.CREATED);
    }

    public ResponseEntity<Flight> getByIDService(int id) throws ResourceNotFoundException {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("flight by this id not found"));
        return new ResponseEntity<>(flight, HttpStatus.OK);
    }

    public ResponseEntity<List<Flight>> getByOriginAndDestinationService(SearchRequest req) {
        List<Flight> listOfFlights =
                flightRepository.findByOriginAndDestination(req.getOrigin(), req.getDestination());
        return new ResponseEntity<>(listOfFlights, HttpStatus.OK);
    }

    public ResponseEntity<List<Flight>> getByOriginAndDestinationAndDepartureDateTimeService(SearchRequest req) {

        LocalDateTime start = req.getDepartureDateTime().toLocalDate().atStartOfDay();
        LocalDateTime end = req.getDepartureDateTime().toLocalDate().atTime(23, 59, 59);

        List<Flight> listOfFlights =
                flightRepository.findByOriginAndDestinationAndDepartureTimeBetween(
                        req.getOrigin(), req.getDestination(), start, end);

        return new ResponseEntity<>(listOfFlights, HttpStatus.OK);
    }

    public ResponseEntity<String> deleteByIDService(int id) throws ResourceNotFoundException {
        flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("flight by this id not found"));

        flightRepository.deleteById(id);
        return new ResponseEntity<>("deleted", HttpStatus.OK);
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }
}
