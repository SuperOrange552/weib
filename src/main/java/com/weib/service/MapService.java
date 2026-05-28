package com.weib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class MapService {

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amap.key}")
    private String amapKey;

    public double[] geocode(String address, String city) {
        try {
            String fullAddress = (city != null && !city.isEmpty()) ? city + address : address;
            String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);
            String url = GEOCODE_URL + "?address=" + encodedAddress + "&key=" + amapKey;

            String response = restTemplate.getForObject(url, String.class);
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
            // 地理编码失败，返回null
        }
        return null;
    }
}
