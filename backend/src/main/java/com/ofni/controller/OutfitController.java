package com.ofni.controller;

import com.ofni.dto.OutfitGenerateRequest;
import com.ofni.dto.OutfitRequest;
import com.ofni.dto.OutfitResponse;
import com.ofni.service.OutfitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/outfits")
public class OutfitController {

    private final OutfitService service;

    public OutfitController(OutfitService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OutfitResponse> create(
        @Valid @RequestBody OutfitRequest request
    ) {
        return ResponseEntity.ok(service.create(request));
    }

    @PostMapping("/generate")
    public ResponseEntity<OutfitResponse> generate(
        @Valid @RequestBody OutfitGenerateRequest request
    ) {
        return ResponseEntity.ok(service.generate(request));
    }

    @GetMapping
    public List<OutfitResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OutfitResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/temperature")
    public List<OutfitResponse> byTemperature(@RequestParam Double temp) {
        return service.findByTemperature(temp);
    }
}
