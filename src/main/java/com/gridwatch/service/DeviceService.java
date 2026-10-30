package com.gridwatch.service;

import com.gridwatch.dto.DeviceRegistrationResponse;
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
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DeviceRegistrationResponse register(DeviceRequest request) {
        String apiKey = newKey();
        Device device = repository.save(new Device(
                request.name().trim(), request.latitude(), request.longitude(), hash(apiKey)));
        return new DeviceRegistrationResponse(DeviceResponse.from(device), apiKey);
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(DeviceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Device> authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();
        return repository.findByApiKeyHash(hash(apiKey));
    }

    static String newKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "gw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}