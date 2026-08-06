package com.neueda.tms.Auth;

import com.neueda.tms.controller.auth.AuthController;
import com.neueda.tms.controller.auth.LoginRequest;
import com.neueda.tms.controller.auth.LoginResponse;
import com.neueda.tms.service.auth.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private Authentication authentication;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authenticationManager, tokenProvider);
    }

    @Test
    void successfulLoginReturnsABearerTokenForTheAuthenticatedUser() {
        UserDetails principal = User.builder()
                .username("admin.user")
                .password("ignored")
                .authorities("ROLE_ADMIN")
                .build();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        ResponseEntity<LoginResponse> response = authController.login(new LoginRequest("admin.user", "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getExpiresIn()).isEqualTo(3600000L);
        assertThat(response.getBody().getUsername()).isEqualTo("admin.user");
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void returnedRoleDropsTheSpringSecurityPrefixBeforeSendingItToTheFrontend() {
        UserDetails principal = User.builder()
                .username("fraud.analyst")
                .password("ignored")
                .authorities("ROLE_ANALYST")
                .build();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateToken(authentication)).thenReturn("token-2");
        when(tokenProvider.getExpirationMs()).thenReturn(7200000L);

        ResponseEntity<LoginResponse> response = authController.login(new LoginRequest("fraud.analyst", "secret"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("ANALYST");
    }

    @Test
    void loginFallsBackToAnalystWhenTheAuthenticatedUserHasNoAuthorities() {
        UserDetails principal = new User("new.user", "ignored", List.of());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(tokenProvider.generateToken(authentication)).thenReturn("token-3");
        when(tokenProvider.getExpirationMs()).thenReturn(1800000L);

        ResponseEntity<LoginResponse> response = authController.login(new LoginRequest("new.user", "secret"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("ANALYST");
    }

    @Test
    void invalidCredentialsBubbleUpSoTheGlobalHandlerCanTurnThemIntoUnauthorized() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authController.login(new LoginRequest("wrong.user", "wrong-pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void disabledAccountsAreRejectedWithoutCreatingAToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("account disabled"));

        assertThatThrownBy(() -> authController.login(new LoginRequest("locked.user", "secret")))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("disabled");
    }
}
