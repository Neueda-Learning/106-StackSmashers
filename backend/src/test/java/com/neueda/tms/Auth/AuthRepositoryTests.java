package com.neueda.tms.Auth;

import com.neueda.tms.repository.auth.User;
import com.neueda.tms.repository.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRepositoryTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository(jdbcTemplate);
    }

    @Test
    void findingAnExistingUsernameReturnsThatUser() {
        User user = aUser(1L, "jane.doe", User.UserRole.ANALYST, true);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("jane.doe")))
                .thenReturn(user);

        Optional<User> result = userRepository.findByUsername("jane.doe");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("jane.doe");
        assertThat(result.get().getRole()).isEqualTo(User.UserRole.ANALYST);
    }

    @Test
    void findingAnUnknownUsernameReturnsEmpty() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("missing.user")))
                .thenThrow(new EmptyResultDataAccessException(1));

        Optional<User> result = userRepository.findByUsername("missing.user");

        assertThat(result).isEmpty();
    }

    @Test
    void checkingUsernameExistenceReturnsTrueWhenCountIsPositive() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("alice")))
                .thenReturn(2L);

        boolean exists = userRepository.existsByUsername("alice");

        assertThat(exists).isTrue();
    }

    @Test
    void checkingUsernameExistenceReturnsFalseWhenCountIsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("nobody")))
                .thenReturn(0L);

        boolean exists = userRepository.existsByUsername("nobody");

        assertThat(exists).isFalse();
    }

    @Test
    void checkingUsernameExistenceReturnsFalseWhenDatabaseReturnsNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("maybe")))
                .thenReturn(null);

        boolean exists = userRepository.existsByUsername("maybe");

        assertThat(exists).isFalse();
    }

    @Test
    void savingANewUserAssignsTheGeneratedIdAndReturnsTheUser() {
        User newUser = aUser(null, "new.analyst", null, true);

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("id", 77L));
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        User saved = userRepository.save(newUser);

        assertThat(saved.getId()).isEqualTo(77L);
        assertThat(saved.getUsername()).isEqualTo("new.analyst");
    }

    @Test
    void savingAnExistingUserKeepsItsIdAndReturnsUpdatedRecord() {
        User existingUser = aUser(15L, "ops.admin", User.UserRole.ADMIN, false);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        User saved = userRepository.save(existingUser);

        assertThat(saved.getId()).isEqualTo(15L);
        assertThat(saved.getUsername()).isEqualTo("ops.admin");
        assertThat(saved.getRole()).isEqualTo(User.UserRole.ADMIN);
    }

    private User aUser(Long id, String username, User.UserRole role, Boolean active) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hashed-password");
        user.setRole(role);
        user.setIsActive(active);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}

