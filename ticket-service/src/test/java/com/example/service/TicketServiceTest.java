package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.example.event.TicketBookedEvent;
import com.example.feign.FlightInterface;
import com.example.feign.PassengerInterface;
import com.example.model.Ticket;
import com.example.repository.TicketRepository;
import com.example.request.BookTicketRequest;
import com.example.response.FlightResponse;
import com.example.response.PassengerDetailsResponse;
import com.example.response.TicketResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PassengerInterface passengerInterface;

    @Mock
    private FlightInterface flightInterface;

    @Mock
    private KafkaTemplate<String, TicketBookedEvent> kafkaTemplate;

    @InjectMocks
    private TicketService ticketService;

    @Captor
    private ArgumentCaptor<Ticket> ticketCaptor;

    private PassengerDetailsResponse passenger;
    private FlightResponse flight;

    @BeforeEach
    public void setUp() {
        passenger = PassengerDetailsResponse.builder().name("John Doe").email("john@example.com").phoneNum("12345").build();
        flight = FlightResponse.builder()
                .origin("A")
                .destination("B")
                .arrivalTime(LocalDateTime.now().plusDays(2))
                .departureTime(LocalDateTime.now().plusDays(2).minusHours(2))
                .build();
    }

    @Test
    public void testBookTicketService_success() {
        BookTicketRequest req = new BookTicketRequest();
        req.setFlightId(1);
        req.setPassengerId(2);
        req.setNumberOfSeats(2);

        // flightInterface.reserveSeats is void
        doNothing().when(flightInterface).reserveSeats(1, 2);

        // repository save returns the ticket (we'll capture it)
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        when(passengerInterface.getPassengerDetails(2)).thenReturn(ResponseEntity.ok(passenger));

        // kafka send - do nothing (we don't assert it here)
        when(kafkaTemplate.send(any(), any())).thenReturn(null);

        ResponseEntity<String> resp = ticketService.bookTicketService(req);
        assertEquals(200, resp.getStatusCodeValue());
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().length()).isEqualTo(8);

        verify(flightInterface, times(1)).reserveSeats(1, 2);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(passengerInterface, times(1)).getPassengerDetails(2);
    }

    @Test
    public void testDeleteTicketById_success() {
        Ticket t = Ticket.builder().ticketId(10).pnr("PNR12345").flightId(100).passengerId(5).numberOfSeats(1).booked(true).build();
        when(ticketRepository.findById(10)).thenReturn(Optional.of(t));

        FlightResponse flightResp = FlightResponse.builder()
                .departureTime(LocalDateTime.now().plusDays(2))
                .build();
        when(flightInterface.getByID(100)).thenReturn(ResponseEntity.ok(flightResp));

        ResponseEntity<String> resp = ticketService.deleteTicketById(10);
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Ticket cancelled successfully", resp.getBody());

        verify(flightInterface, times(1)).releaseSeats(100, 1);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    public void testDeleteTicketById_within24Hours_shouldReturnBadRequest() {
        Ticket t = Ticket.builder().ticketId(11).pnr("PNR67890").flightId(200).passengerId(6).numberOfSeats(1).booked(true).build();
        when(ticketRepository.findById(11)).thenReturn(Optional.of(t));

        FlightResponse flightResp = FlightResponse.builder()
                .departureTime(LocalDateTime.now().plusHours(12))
                .build();
        when(flightInterface.getByID(200)).thenReturn(ResponseEntity.ok(flightResp));

        ResponseEntity<String> resp = ticketService.deleteTicketById(11);
        assertEquals(400, resp.getStatusCodeValue());
        assertThat(resp.getBody()).contains("cannot be cancelled");

        // no release/seve save
    }

    @Test
    public void testGetByPnrService_success() {
        Ticket t = Ticket.builder().ticketId(20).pnr("PNR00001").flightId(300).passengerId(7).numberOfSeats(1).booked(true).build();
        when(ticketRepository.findByPnr("PNR00001")).thenReturn(Optional.of(t));

        when(passengerInterface.getPassengerDetails(7)).thenReturn(ResponseEntity.ok(passenger));
        when(flightInterface.getByID(300)).thenReturn(ResponseEntity.ok(flight));

        ResponseEntity<TicketResponse> resp = ticketService.getByPnrService("PNR00001");
        assertEquals(200, resp.getStatusCodeValue());
        assertThat(resp.getBody()).isNotNull();
        assertEquals("PNR00001", resp.getBody().getPnr());
        assertEquals("John Doe", resp.getBody().getName());
    }

    @Test
    public void testGetTicketsByEmailService_success() {
        String email = "john@example.com";
        when(passengerInterface.getIdByEmail(email)).thenReturn(ResponseEntity.ok(7));
        when(passengerInterface.getPassengerDetails(7)).thenReturn(ResponseEntity.ok(passenger));

        Ticket t1 = Ticket.builder().ticketId(31).pnr("P1").flightId(400).passengerId(7).numberOfSeats(1).booked(true).build();
        Ticket t2 = Ticket.builder().ticketId(32).pnr("P2").flightId(400).passengerId(7).numberOfSeats(2).booked(true).build();

        when(ticketRepository.findAllByPassengerId(7)).thenReturn(List.of(t1, t2));
        when(flightInterface.getByID(400)).thenReturn(ResponseEntity.ok(flight));

        ResponseEntity<List<TicketResponse>> resp = ticketService.getTicketsByEmailService(email);
        assertEquals(200, resp.getStatusCodeValue());
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody().get(0).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    public void testBookTicketService_invalidSeats_throws() {
        BookTicketRequest req = new BookTicketRequest();
        req.setFlightId(1);
        req.setPassengerId(2);
        req.setNumberOfSeats(0);

        assertThrows(IllegalArgumentException.class, () -> ticketService.bookTicketService(req));
    }
}

