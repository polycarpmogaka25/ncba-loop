package com.integration.ncba.test.controller;

import com.integration.ncba.test.models.CountryInfo;
import com.integration.ncba.test.models.dto.Country;
import com.integration.ncba.test.models.dto.CountryRequest;
import com.integration.ncba.test.repo.CountryInfoRepository;
import com.integration.ncba.test.service.CountryIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
@Slf4j
public class CountryController {

    private final CountryIntegrationService integrationService;

    private final CountryInfoRepository repository;

    @PostMapping("/integrate")
    public ResponseEntity<CountryInfo> integrateCountry(@Valid @RequestBody CountryRequest request) {
        log.info("Received integration request for country={}", request.getName());

        var countryName = request.getName();

        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.processCountryInbound(countryName));
    }

    @GetMapping
    public List<CountryInfo> getAllCountries() {
        return integrationService.getAllCountries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryInfo> getCountryById(@PathVariable Long id) {
        return integrationService.getCountryById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryInfo> updateCountry(@PathVariable Long id, @RequestBody Country updatedData) {
        return integrationService.updateCountry(id, updatedData);
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