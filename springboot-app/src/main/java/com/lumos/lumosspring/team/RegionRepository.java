package com.lumos.lumosspring.team;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface RegionRepository extends CrudRepository<Region, Long> {
    Optional<Region> findRegionByRegionName(String regionName);
}
