package com.springboot.training.service;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PingService {

    public Map<String, String> status() {
        return Map.of("status", "ok");
    }
}