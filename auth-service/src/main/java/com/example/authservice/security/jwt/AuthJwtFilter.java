package com.example.authservice.security.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthJwtFilter extends OncePerRequestFilter {

	@Autowired
	private JwtUtils jwtUtils;

	@Autowired
	private UserDetailsService userDetailsService;

//	Does doFilterInternal() auto-run?
//			YES — for EVERY HTTP request
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException, java.io.IOException {
        System.out.println("AuthJwtFilter HIT for path: " + request.getRequestURI());
		String path = request.getRequestURI();

		// Allow auth endpoints without JWT
        if (path.equals("/api/auth/signin")
                || path.equals("/api/auth/signup")
                || path.equals("/api/auth/signout")) {
            filterChain.doFilter(request, response);
            return;
        }

		String jwt = jwtUtils.getJwtFromCookies(request);

		if (jwt != null && jwtUtils.validateJwtToken(jwt)) {

			String username = jwtUtils.getUserNameFromJwtToken(jwt);
            System.out.println("Auth Header: " + request.getHeader("Authorization"));
            System.out.println("Cookie token: " + request.getCookies());
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			// create auth token which is authenticated by passing userDetails
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
					null, userDetails.getAuthorities());
			// set details from request
			// the details are additional information about the authentication request, such
			// as the remote address and session ID.
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			// set authentication in security context
			SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("Authentication set: " + authentication.getName());
		}

		filterChain.doFilter(request, response);
	}
}
