package com.example.dump.dumpdb.service;

import com.example.dump.dumpdb.entity.Persona;
import com.example.dump.dumpdb.repo.PersonaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonaService {

    @Autowired
    private PersonaRepo personaRepository;

    public List<Persona> findAll() {
        return personaRepository.findAll();
    }

    public Persona findById(Long id) {
        return personaRepository.findById(id).orElse(null);
    }

    public Persona save(Persona persona) {
        return personaRepository.save(persona);
    }

    public void delete(Long id) {
        personaRepository.deleteById(id);
    }
}
