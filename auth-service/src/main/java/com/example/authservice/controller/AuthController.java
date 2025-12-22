package com.example.authservice.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.authservice.models.ChangePasswordRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.authservice.models.ERole;
import com.example.authservice.models.Role;
import com.example.authservice.models.User;
import com.example.authservice.payload.request.LoginRequest;
import com.example.authservice.payload.request.SignupRequest;
import com.example.authservice.payload.response.MessageResponse;
import com.example.authservice.payload.response.UserInfoResponse;
import com.example.authservice.repository.RoleRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.jwt.JwtUtils;
import com.example.authservice.security.services.UserDetailsImpl;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired 
	PasswordEncoder encoder;
	

	@Autowired
	JwtUtils jwtUtils;

   

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {

		if (userDetails == null) {
			return ResponseEntity.status(401).body(new MessageResponse("Unauthorized"));
		}

		List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(
				new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
	}

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isPasswordExpired()) {
                user.setForcePasswordChange(true);
                userRepository.save(user);

                ResponseCookie jwtCookie =
                        jwtUtils.generateJwtCookie(userDetails, true);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(Map.of(
                                "status", "PASSWORD_EXPIRED",
                                "message", "Please change your password",
                                "forcePasswordChange", true
                        ));
            }

			ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails,user.isForcePasswordChange());

			List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority())
					.collect(Collectors.toList());

			return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString()).body(new UserInfoResponse(
					userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));

		} catch (BadCredentialsException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
		}
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Username already taken"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Email already in use"));
		}
		System.out.println("Incoming roles: " + signUpRequest.getRoles());

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword())
        );

        user.setPasswordLastChangedAt(LocalDateTime.now());
        user.setForcePasswordChange(false);

		Set<String> strRoles = signUpRequest.getRoles();
		Set<Role> roles = new HashSet<>();

		if (strRoles == null || strRoles.isEmpty()) {
			
			Role userRole = roleRepository.findByName(ERole.ROLE_USER)
					.orElseThrow(() -> new RuntimeException("Error: Role USER not found"));
			roles.add(userRole);
		} else {
			strRoles.forEach(role -> {
				switch (role.toLowerCase()) {
				case "admin":
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role ADMIN not found"));
					roles.add(adminRole);
					break;

				default:
					Role userRole = roleRepository.findByName(ERole.ROLE_USER)
							.orElseThrow(() -> new RuntimeException("Error: Role USER not found"));
					roles.add(userRole);
				}
			});
		}

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	@PostMapping("/signout")
	public ResponseEntity<?> logoutUser() {
		ResponseCookie cleanCookie = jwtUtils.getCleanJwtCookie();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
				.body(new MessageResponse("You've been signed out!"));
	}
	 @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        System.out.println("CONTROLLER HIT: change-password");
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Error: Old password is incorrect");
        }

        if (encoder.matches(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Error: New password must be not equal to previous one");
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setPasswordLastChangedAt(LocalDateTime.now());
        user.setForcePasswordChange(false);

        userRepository.save(user);

        return ResponseEntity.ok("Password is changed successfully");
    }

}
