package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Region;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;


public interface RegionRepository extends CrudRepository<Region, Long> {
    Optional<Region> findRegionByRegionName(String regionName);
}
