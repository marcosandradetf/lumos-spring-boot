package com.lumos.lumosspring.installation.repository.premeasurement;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegisterRepository {
    private final NamedParameterJdbcTemplate namedJDBC;

    public RegisterRepository(NamedParameterJdbcTemplate namedJDBC) {
        this.namedJDBC = namedJDBC;
    }

}
