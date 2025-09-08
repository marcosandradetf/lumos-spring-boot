package com.lumos.lumosspring.team.repository;

import com.lumos.lumosspring.team.entities.Region;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface RegionRepository extends CrudRepository<Region, Long> {
    List<Region> findRegionByRegionName(String regionName);
}
