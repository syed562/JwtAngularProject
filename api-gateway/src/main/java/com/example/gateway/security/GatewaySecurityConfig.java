package com.example.gateway.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GatewaySecurityConfig {

	private final JwtAuthFilter jwtFilter;

	public GatewaySecurityConfig(JwtAuthFilter jwtFilter) {
		this.jwtFilter = jwtFilter;
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

		return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(ex -> ex.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						.pathMatchers("/auth-service/api/auth/signup", "/auth-service/api/auth/signin",
								"/auth-service/api/auth/me")
						.permitAll()

						.pathMatchers("/flight-service/flight/register").hasRole("ADMIN")
						.pathMatchers("/flight-service/flight/delete/**").hasRole("ADMIN")

						.pathMatchers("/flight-service/flight/getFlightById/**").hasAnyRole("ADMIN", "USER")

						.pathMatchers("/flight-service/flight/**").permitAll()

						.pathMatchers("/passenger-service/passenger/register").hasAnyRole("ADMIN", "USER")
						.pathMatchers("/passenger-service/passenger/getByPassengerId/**").hasAnyRole("ADMIN", "USER")
						.pathMatchers("/passenger-service/passenger/getPassengerIdByEmail/**")
						.hasAnyRole("ADMIN", "USER").pathMatchers("/passenger-service/passenger/delete/**")
						.hasAnyRole("ADMIN", "USER")

						.pathMatchers("/ticket-service/ticket/book").hasAnyRole("ADMIN", "USER")
						.pathMatchers("/ticket-service/ticket/getByPnr/**").hasAnyRole("ADMIN", "USER")
						.pathMatchers("/ticket-service/ticket/getTicketsByEmail/**").hasAnyRole("ADMIN", "USER")

						.anyExchange().authenticated())
				.addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable).build();
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(List.of("http://localhost:4200"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return new CorsWebFilter(source);
	}
}
