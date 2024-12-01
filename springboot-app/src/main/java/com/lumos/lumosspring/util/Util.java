package com.lumos.lumosspring.util;

import com.lumos.lumosspring.authentication.entities.User;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

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

    // Método auxiliar para verificar se uma string está vazia ou é nula
    public boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String formatPrice(BigDecimal price) {
        if (price != null) {
            // Formatação de número para garantir que a vírgula seja usada como separador decimal
            DecimalFormat df = new DecimalFormat("#,##0.00");
            return df.format(price).replace('.', ','); // Troca o ponto por vírgula
        }
        return "0,00";
    }
}
