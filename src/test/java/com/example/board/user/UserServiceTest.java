package com.example.board.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.board.user.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void signup_persistsBcryptHashedPassword() {
        SignupRequest request = new SignupRequest("alice", "pw1234!", "앨리스");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getNickname()).isEqualTo("앨리스");
        assertThat(saved.getPassword()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("pw1234!", saved.getPassword())).isTrue();
    }

    @Test
    void signup_throwsOnDuplicateUsername_preCheck() {
        SignupRequest request = new SignupRequest("alice", "pw1234!", "앨리스");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_propagatesDataIntegrityFromSave_raceCondition() {
        SignupRequest request = new SignupRequest("alice", "pw1234!", "앨리스");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
