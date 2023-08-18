package com.example.dump.dumpdb.controller;

import com.example.dump.dumpdb.entity.Persona;
import com.example.dump.dumpdb.service.PersonaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persone")
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @GetMapping
    public List<Persona> getAll() {
        return personaService.findAll();
    }

    @GetMapping("/{id}")
    public Persona getById(@PathVariable Long id) {
        return personaService.findById(id);
    }

    @PostMapping
    public Persona create(@RequestBody Persona persona) {
        return personaService.save(persona);
    }

    @PutMapping("/{id}")
    public Persona update(@PathVariable Long id, @RequestBody Persona persona) {
        // aggiorna l'entità persona qui
        return personaService.save(persona);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        personaService.delete(id);
    }

}
