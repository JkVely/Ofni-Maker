package com.ofni.controller;

import com.ofni.dto.OutfitGenerateRequest;
import com.ofni.dto.OutfitRequest;
import com.ofni.dto.OutfitResponse;
import com.ofni.security.UserPrincipal;
import com.ofni.service.OutfitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
        return ResponseEntity.ok(service.create(request, currentUser().userId()));
    }

    @PostMapping("/generate")
    public ResponseEntity<OutfitResponse> generate(
        @Valid @RequestBody OutfitGenerateRequest request
    ) {
        return ResponseEntity.ok(service.generate(request, currentUser().userId()));
    }

    @GetMapping
    public List<OutfitResponse> list() {
        return service.findAllByUser(currentUser().userId());
    }

    @GetMapping("/{id}")
    public OutfitResponse get(@PathVariable Long id) {
        return service.findById(id, currentUser().userId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, currentUser().userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/temperature")
    public List<OutfitResponse> byTemperature(@RequestParam Double temp) {
        return service.findByTemperature(temp, currentUser().userId());
    }

    private UserPrincipal currentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
