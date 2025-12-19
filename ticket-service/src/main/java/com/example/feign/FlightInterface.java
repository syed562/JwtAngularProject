package com.example.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.response.FlightResponse;

@FeignClient(name = "flight-service")
public interface FlightInterface {

    @PutMapping("/flight/flights/{id}/reserve")
    void reserveSeats(@PathVariable int id,
                      @RequestParam int seats);

    @PutMapping("/flight/flights/{id}/release")
    void releaseSeats(@PathVariable int id,
                      @RequestParam int seats);

    @GetMapping("/flight/getFlightById/{id}")
	public ResponseEntity<FlightResponse> getByID(@PathVariable int id);
}
