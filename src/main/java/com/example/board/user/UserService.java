package com.example.board.user;

import com.example.board.user.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DataIntegrityViolationException(
                    "username duplicated: " + request.username());
        }
        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.nickname());
        return userRepository.save(user);
    }
}
