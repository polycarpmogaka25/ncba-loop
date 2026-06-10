package com.integration.ncba.test.service;


import com.integration.ncba.test.exception.UpstreamServiceException;
import com.integration.ncba.test.models.CountryInfo;
import com.integration.ncba.test.models.Language;
import com.integration.ncba.test.repo.CountryInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CountryIntegrationService {

    private final CountryInfoRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${soap.api.url}")
    private String soapUrl;

    public CountryInfo processCountryInbound(String rawName) {
        var formattedName = toSentenceCase(rawName);
        log.info("Processing inbound integration request for country: {}", formattedName);

        // Step 4: Fetch ISO Code from SOAP
        String isoCode = fetchIsoCodeFromSoap(formattedName);
        if (isoCode == null || isoCode.trim().isEmpty() || isoCode.contains("No country found")) {
            throw new RuntimeException("ISO Code not found for country: " + formattedName);
        }

        // Check cache/DB to protect downstream processing
        return repository.findByIsoCode(isoCode).orElseGet(() -> {
            // Step 5: Fetch Full Info if not cached
            CountryInfo freshInfo = fetchFullCountryInfoFromSoap(isoCode);
            freshInfo.setName(formattedName);
            return repository.save(freshInfo);
        });
    }

    private String toSentenceCase(String input) {
        if (input == null || input.isBlank()) return "";
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
            ResponseEntity<String> response = callSoapEndpoint(soapEnvelope);
            Document doc = parseXml(response.getBody());
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
            ResponseEntity<String> response = callSoapEndpoint(soapEnvelope);
            Document doc = parseXml(response.getBody());

            CountryInfo info = new CountryInfo();
            info.setIsoCode(doc.getElementsByTagName("m:sISOCode").item(0).getTextContent());
            info.setCapitalCity(doc.getElementsByTagName("m:sCapitalCity").item(0).getTextContent());
            info.setPhoneCode(doc.getElementsByTagName("m:sPhoneCode").item(0).getTextContent());
            info.setContinentCode(doc.getElementsByTagName("m:sContinentCode").item(0).getTextContent());

            NodeList langNodes = doc.getElementsByTagName("m:tLanguage");
            List<Language> languages = new ArrayList<>();
            for (int i = 0; i < langNodes.getLength(); i++) {
                Element el = (Element) langNodes.item(i);
                String lCode = el.getElementsByTagName("m:sISOCode").item(0).getTextContent();
                String lName = el.getElementsByTagName("m:sName").item(0).getTextContent();
                languages.add(new Language(null, lCode, lName));
            }
            info.setLanguages(languages);
            return info;

        } catch (Exception e) {
            log.error("Failed to fetch full country data for ISO code {}", isoCode, e);
            throw new UpstreamServiceException("SOAP API unavailable during details retrieval.");
        }
    }

    private ResponseEntity<String> callSoapEndpoint(String payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(soapUrl, entity, String.class);
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}