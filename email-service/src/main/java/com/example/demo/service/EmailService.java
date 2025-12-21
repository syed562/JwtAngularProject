package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingEmail(String toEmail, String content) {
        System.out.println("Inside EmailService.sendBookingEmail()");
        System.out.println("Sending email to: " + toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@flightapp.com");
        message.setTo(toEmail);
        message.setSubject("Ticket Booking Confirmation");
        message.setText(content);

        mailSender.send(message);
        System.out.println("mailSender.send() finished");
    }
}
