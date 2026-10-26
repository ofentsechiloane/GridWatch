package com.gridwatch.service;

import com.gridwatch.dto.DeviceRequest;
import com.gridwatch.dto.DeviceResponse;
import com.gridwatch.model.Device;
import com.gridwatch.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
public class DeviceService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    /** Creates a device and returns it with its API key. The key is never retrievable again. */
    @Transactional
    public DeviceResponse register(DeviceRequest req) {
        String apiKey = generateKey();
        Device saved = repository.save(new Device(req.name(), req.latitude(), req.longitude(), hash(apiKey)));
        return DeviceResponse.from(saved, apiKey);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> list() {
        return repository.findAll().stream().map(d -> DeviceResponse.from(d, null)).toList();
    }

    public static String generateKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "gw_" + HexFormat.of().formatHex(bytes);
    }

    public static String hash(String apiKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}