package com.integration.ncba.test.controller;

import com.integration.ncba.test.models.CountryInfo;
import com.integration.ncba.test.repo.CountryInfoRepository;
import com.integration.ncba.test.service.CountryIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryIntegrationService integrationService;

    private final CountryInfoRepository repository;

    @PostMapping("/integrate")
    public ResponseEntity<CountryInfo> integrateCountry(@RequestBody Map<String, String> payload) {
        String countryName = payload.get("name");
        if (countryName == null || countryName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.processCountryInbound(countryName));
    }

    @GetMapping
    public List<CountryInfo> getAllCountries() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryInfo> getCountryById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryInfo> updateCountry(@PathVariable Long id, @RequestBody CountryInfo updatedData) {
        return repository.findById(id).map(existing -> {
            existing.setName(updatedData.getName());
            existing.setCapitalCity(updatedData.getCapitalCity());
            existing.setPhoneCode(updatedData.getPhoneCode());
            existing.setContinentCode(updatedData.getContinentCode());
            if(updatedData.getLanguages() != null) {
                existing.getLanguages().clear();
                existing.getLanguages().addAll(updatedData.getLanguages());
            }
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountry(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}