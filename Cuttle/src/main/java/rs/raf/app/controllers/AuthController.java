package rs.raf.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import rs.raf.app.model.User;
import rs.raf.app.requests.AuthRequest;
import rs.raf.app.responses.LoginResponse;
import rs.raf.app.responses.ResponseDto;
import rs.raf.app.services.UserService;
import rs.raf.app.utils.JwtUtil;

import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest){
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
        } catch (Exception   e){
//            e.printStackTrace();
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new LoginResponse(jwtUtil.generateToken(authRequest.getUsername())));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest authRequest){
        if (userService.findByUsername(authRequest.getUsername()).isPresent()) {
            return ResponseEntity.status(400).body("User with that username already exists");
        }
        return ResponseEntity.ok(userService.create(new User(0L, authRequest.getUsername(), authRequest.getPassword())));
    }

}
