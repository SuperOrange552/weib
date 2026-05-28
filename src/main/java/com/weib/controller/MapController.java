package com.weib.controller;

import com.weib.entity.User;
import com.weib.service.MapService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/api/geocode")
    public Map<String, Object> geocode(@RequestParam String address,
                                        @RequestParam(required = false) String city,
                                        HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Map.of("code", 401, "msg", "请先登录");
        }
        double[] coords = mapService.geocode(address, city);
        if (coords != null) {
            return Map.of("code", 200, "lng", coords[0], "lat", coords[1]);
        }
        return Map.of("code", 500, "msg", "地理编码失败，请手动在地图上选择位置");
    }
}
