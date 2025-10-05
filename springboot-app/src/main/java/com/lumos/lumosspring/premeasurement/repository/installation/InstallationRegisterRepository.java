package com.lumos.lumosspring.premeasurement.repository.installation;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstallationRegisterRepository {
    private final NamedParameterJdbcTemplate namedJDBC;

    public InstallationRegisterRepository(NamedParameterJdbcTemplate namedJDBC) {
        this.namedJDBC = namedJDBC;
    }

}
