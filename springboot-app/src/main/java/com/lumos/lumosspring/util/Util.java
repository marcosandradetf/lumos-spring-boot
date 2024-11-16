package com.lumos.lumosspring.util;

import com.lumos.lumosspring.authentication.entities.User;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Util {
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public Util(final JwtDecoder jwtDecoder, RefreshTokenRepository refreshTokenRepository) {
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public BigDecimal convertToBigDecimal(String value) {
        if (value == null) {
            return null;
        }
        try {
            var newValue = value.replace(",", ".");
            return new BigDecimal(newValue);
        } catch (Exception e) {
            return null;
        }
    }

    public User getUserFromRToken(String rToken) {
        var tokenFromDb = refreshTokenRepository.findByToken(rToken);
        if (tokenFromDb.isEmpty()) {
            return null;
        }
        Jwt jwt = jwtDecoder.decode(rToken);

        return tokenFromDb.get().getUser();
    }

}
