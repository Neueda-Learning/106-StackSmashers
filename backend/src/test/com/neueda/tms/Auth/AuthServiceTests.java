package com.neueda.tms.Auth;

import com.neueda.tms.repository.auth.User;
import com.neueda.tms.repository.auth.UserRepository;
import com.neueda.tms.service.auth.JwtTokenProvider;
import com.neueda.tms.service.auth.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl userDetailsService;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userRepository);

        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 3600000L);
    }

    @Test
    void activeAnalystCanLogInAndGetsTheRightRoleAuthority() {
        User analyst = aUser("sarah.analyst", User.UserRole.ANALYST, true);
        when(userRepository.findByUsername("sarah.analyst")).thenReturn(Optional.of(analyst));

        UserDetails loaded = userDetailsService.loadUserByUsername("sarah.analyst");

        assertThat(loaded.getUsername()).isEqualTo("sarah.analyst");
        assertThat(loaded.getAuthorities()).extracting("authority").containsExactly("ROLE_ANALYST");
    }

    @Test
    void unknownUsernameIsRejectedWithAClearMessage() {
        when(userRepository.findByUsername("missing.user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing.user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: missing.user");
    }

    @Test
    void disabledUserCannotAuthenticate() {
        User disabled = aUser("blocked.user", User.UserRole.ADMIN, false);
        when(userRepository.findByUsername("blocked.user")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("blocked.user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void tokenGeneratedFromUsernameCanBeReadBackAndValidated() {
        String token = tokenProvider.generateTokenFromUsername("sam.operator");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("sam.operator");
    }

    @Test
    void tokenGeneratedFromAuthenticationUsesPrincipalUsername() {
        Authentication authentication = mock(Authentication.class);
        UserDetails principal = org.springframework.security.core.userdetails.User.builder()
                .username("jane.supervisor")
                .password("ignored")
                .authorities("ROLE_ADMIN")
                .build();
        when(authentication.getPrincipal()).thenReturn(principal);

        String token = tokenProvider.generateToken(authentication);

        assertThat(tokenProvider.getUsernameFromToken(token)).isEqualTo("jane.supervisor");
    }

    @Test
    void malformedTokenFailsValidationInsteadOfCrashing() {
        assertThat(tokenProvider.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void expiredTokenFailsValidation() throws InterruptedException {
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 1L);
        String token = tokenProvider.generateTokenFromUsername("short.lived");
        Thread.sleep(5);

        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    private User aUser(String username, User.UserRole role, boolean active) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hashed-secret");
        user.setRole(role);
        user.setIsActive(active);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
