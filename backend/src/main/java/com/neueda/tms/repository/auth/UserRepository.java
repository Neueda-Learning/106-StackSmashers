package com.neueda.tms.repository.auth;

import com.neueda.tms.repository.auth.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));

        String role = rs.getString("role");
        if (role != null) {
            user.setRole(User.UserRole.valueOf(role));
        }

        user.setIsActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        return user;
    };

    public User save(User user) {
        if (user.getId() == null) {
            return insert(user);
        }

        update(user);
        return user;
    }

    private User insert(User user) {

        String sql = """
                INSERT INTO users (username, password_hash, role, is_active, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());

            if (user.getRole() != null) {
                statement.setString(3, user.getRole().name());
            } else {
                statement.setString(3, User.UserRole.ANALYST.name());
            }

            statement.setBoolean(4,
                    user.getIsActive() != null && user.getIsActive());

            LocalDateTime createdTime = user.getCreatedAt();

            if (createdTime == null) {
                createdTime = LocalDateTime.now();
            }

            statement.setTimestamp(5, Timestamp.valueOf(createdTime));

            return statement;

        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id != null) {
            user.setId(id.longValue());
        }

        return user;
    }

    private void update(User user) {

        String sql = """
                UPDATE users
                SET username = ?,
                    password_hash = ?,
                    role = ?,
                    is_active = ?
                WHERE id = ?
                """;

        String role = user.getRole() != null
                ? user.getRole().name()
                : User.UserRole.ANALYST.name();

        boolean active = user.getIsActive() != null
                && user.getIsActive();

        jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getPasswordHash(),
                role,
                active,
                user.getId());
    }

    public Optional<User> findByUsername(String username) {

        try {

            User user = jdbcTemplate.queryForObject(
                    "SELECT * FROM users WHERE username = ?",
                    ROW_MAPPER,
                    username);

            return Optional.ofNullable(user);

        } catch (EmptyResultDataAccessException e) {

            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Long.class,
                username);

        return count != null && count > 0;
    }
}