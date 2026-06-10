package com.integration.ncba.test.controller;

import com.integration.ncba.test.models.CountryInfo;
import com.integration.ncba.test.models.dto.CountryRequest;
import com.integration.ncba.test.service.CountryIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Slf4j
public class CountryController {

    private final CountryIntegrationService countryService;

    @PostMapping
    public ResponseEntity<CountryInfo> integrateCountry(
            @Valid @RequestBody CountryRequest request) {

        log.info("Received integration request for country={}", request.getName());

        CountryInfo countryInfo =
                countryService.processCountryInbound(request.getName());

        log.info("Successfully integrated country={}", countryInfo.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(countryInfo);
    }

    @GetMapping
    public ResponseEntity<Page<CountryInfo>> getAllCountries(
            Pageable pageable) {

        log.info("Fetching all countries");

        return ResponseEntity.ok(
                countryService.getAllCountries(pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryInfo> getCountryById(
            @PathVariable Long id) {

        log.info("Fetching country id={}", id);

        return ResponseEntity.ok(
                countryService.getCountryById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryInfo> updateCountry(
            @PathVariable Long id,
            @Valid @RequestBody CountryInfo request) {

        log.info("Updating country id={}", id);

        return ResponseEntity.ok(
                countryService.updateCountry(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountry(
            @PathVariable Long id) {

        log.info("Deleting country id={}", id);

        countryService.deleteCountry(id);

        return ResponseEntity.noContent().build();
    }
}