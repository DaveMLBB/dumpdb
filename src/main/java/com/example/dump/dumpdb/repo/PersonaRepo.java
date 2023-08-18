package com.example.dump.dumpdb.repo;

import com.example.dump.dumpdb.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepo extends JpaRepository<Persona, Long> {
}
