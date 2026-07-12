package com.weib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class MapService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MapService.class);

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amap.key}")
    private String amapKey;

    public double[] geocode(String address, String city) {
        try {
            URI uri = buildGeocodeUri(address, city, amapKey);
            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (root.get("status").asInt() == 1 && root.get("count").asInt() > 0) {
                String location = root.get("geocodes").get(0).get("location").asText();
                String[] parts = location.split(",");
                if (parts.length == 2) {
                    return new double[]{
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1])
                    };
                }
            }
        } catch (Exception e) {
            log.warn("地理编码失败, address={}", address, e);
        }
        return null;
    }

    static URI buildGeocodeUri(String address, String city, String key) {
        String fullAddress = (city != null && !city.isBlank()) ? city + address : address;
        return UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                .queryParam("address", fullAddress)
                .queryParam("key", key)
                .build()
                .encode()
                .toUri();
    }
}
