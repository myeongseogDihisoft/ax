package com.example.board.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.board.config.SecurityConfig;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    UserService userService;

    static Stream<Arguments> invalidPayloads() {
        return Stream.of(
                Arguments.of("username null",
                        "{\"password\":\"pw1234!\",\"nickname\":\"앨리스\"}"),
                Arguments.of("username blank",
                        "{\"username\":\"\",\"password\":\"pw1234!\",\"nickname\":\"앨리스\"}"),
                Arguments.of("username too short",
                        "{\"username\":\"ab\",\"password\":\"pw1234!\",\"nickname\":\"앨리스\"}"),
                Arguments.of("username too long",
                        "{\"username\":\"" + "a".repeat(51) + "\",\"password\":\"pw1234!\",\"nickname\":\"앨리스\"}"),
                Arguments.of("password null",
                        "{\"username\":\"alice\",\"nickname\":\"앨리스\"}"),
                Arguments.of("password blank",
                        "{\"username\":\"alice\",\"password\":\"\",\"nickname\":\"앨리스\"}"),
                Arguments.of("password too short",
                        "{\"username\":\"alice\",\"password\":\"pw12\",\"nickname\":\"앨리스\"}"),
                Arguments.of("nickname null",
                        "{\"username\":\"alice\",\"password\":\"pw1234!\"}"),
                Arguments.of("nickname blank",
                        "{\"username\":\"alice\",\"password\":\"pw1234!\",\"nickname\":\"\"}")
        );
    }

    @ParameterizedTest(name = "{0} → 400")
    @MethodSource("invalidPayloads")
    void signup_invalidPayload_returns400(String caseName, String body) throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_validPayload_returns201() throws Exception {
        String body = "{\"username\":\"alice\",\"password\":\"pw1234!\",\"nickname\":\"앨리스\"}";

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(userService).signup(any());
    }

    @Test
    void signup_duplicateUsername_returns409() throws Exception {
        doThrow(new DataIntegrityViolationException("username duplicated"))
                .when(userService).signup(any());
        String body = "{\"username\":\"alice\",\"password\":\"pw1234!\",\"nickname\":\"앨리스\"}";

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }
}
