package com.lumos.lumosspring.premeasurement.repository.installation;


import com.lumos.lumosspring.premeasurement.model.PreMeasurement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreMeasurementInstallationRepository extends CrudRepository<PreMeasurement, Long> {

}
