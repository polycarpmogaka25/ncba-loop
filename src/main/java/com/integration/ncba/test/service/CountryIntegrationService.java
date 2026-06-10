package com.integration.ncba.test.service;


import com.integration.ncba.test.exception.UpstreamServiceException;
import com.integration.ncba.test.models.CountryInfo;
import com.integration.ncba.test.models.Language;
import com.integration.ncba.test.models.dto.Country;
import com.integration.ncba.test.repo.CountryInfoRepository;
import com.integration.ncba.test.utils.Utils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("all")
public class CountryIntegrationService {

    private final CountryInfoRepository repository;

    private final Utils utils;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${soap.api.url}")
    private String soapUrl;

    public CountryInfo processCountryInbound(String rawName) {
        var formattedName = toSentenceCase(rawName);
        log.info("Processing inbound integration request for country: {}", formattedName);

        var isoCode = fetchIsoCodeFromSoap(formattedName);
        if (isoCode == null || isoCode.trim().isEmpty() || isoCode.contains("No country found")) {
            throw new RuntimeException("ISO Code not found for country: " + formattedName);
        }

        return repository.findByIsoCode(isoCode).orElseGet(() -> {
            var freshInfo = fetchFullCountryInfoFromSoap(isoCode);
            freshInfo.setName(formattedName);
            return repository.save(freshInfo);
        });
    }

    private String toSentenceCase(String input) {
        if (input == null || input.isBlank())
            return "";
        input = input.trim().toLowerCase();
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }

    private String fetchIsoCodeFromSoap(String countryName) {
        String soapEnvelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <CountryISOCode xmlns="http://www.oorsprong.org/websamples.countryinfo">
                      <sCountryName>%s</sCountryName>
                    </CountryISOCode>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(countryName);

        try {
            var response = callSoapEndpoint(soapEnvelope);
            assert response.getBody() != null;
            var doc = this.utils.parseXml(response.getBody());
            return doc.getElementsByTagName("m:CountryISOCodeResult").item(0).getTextContent();
        } catch (Exception e) {
            log.error("Failed to fetch ISO Code for country {}", countryName, e);
            throw new UpstreamServiceException("SOAP API unavailable during ISO code retrieval.");
        }
    }

    private CountryInfo fetchFullCountryInfoFromSoap(String isoCode) {
        String soapEnvelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <FullCountryInfo xmlns="http://www.oorsprong.org/websamples.countryinfo">
                      <sCountryISOCode>%s</sCountryISOCode>
                    </FullCountryInfo>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(isoCode);

        try {
            var response = callSoapEndpoint(soapEnvelope);
            var doc = this.utils.parseXml(response.getBody());

            var info = new CountryInfo();
            info.setIsoCode(doc.getElementsByTagName("m:sISOCode").item(0).getTextContent());
            info.setCapitalCity(doc.getElementsByTagName("m:sCapitalCity").item(0).getTextContent());
            info.setPhoneCode(doc.getElementsByTagName("m:sPhoneCode").item(0).getTextContent());
            info.setContinentCode(doc.getElementsByTagName("m:sContinentCode").item(0).getTextContent());

            var langNodes = doc.getElementsByTagName("m:tLanguage");
            List<Language> languages = new ArrayList<>();
            for (int i = 0; i < langNodes.getLength(); i++) {
                var el = (Element) langNodes.item(i);
                var lCode = el.getElementsByTagName("m:sISOCode").item(0).getTextContent();
                var lName = el.getElementsByTagName("m:sName").item(0).getTextContent();
                languages.add(new Language(null, lCode, lName));
            }
            info.setLanguages(languages);
            return info;

        } catch (Exception e) {
            log.error("Failed to fetch full country data for ISO code {}", isoCode, e);
            throw new UpstreamServiceException("SOAP API unavailable during details retrieval.");
        }
    }

    @Retry(name = "countrySoap")
    @CircuitBreaker(name = "countrySoap")
    private ResponseEntity<String> callSoapEndpoint(String payload) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        var entity = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(soapUrl, entity, String.class);
    }


    public List<CountryInfo> getAllCountries() {
        return repository.findAll();
    }

    public ResponseEntity<CountryInfo> getCountryById(Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    public ResponseEntity<CountryInfo> updateCountry(Long id, @Valid Country request) {
        return repository.findById(id).map(existing -> {
            existing.setCapitalCity(request.getCapitalCity());
            existing.setPhoneCode(request.getPhoneCode());
            existing.setContinentCode(request.getContinentCode());
            if (request.getLanguages() != null) {
                existing.getLanguages().clear();
                existing.getLanguages().addAll(request.getLanguages());
            }
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

}