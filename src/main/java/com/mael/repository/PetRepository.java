package com.mael.repository;

import com.mael.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, String> {

    List<Pet> findAllByOrderBySortOrderAsc();
}
