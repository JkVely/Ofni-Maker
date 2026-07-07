package com.ofni.controller;

import com.ofni.dto.ClothRequest;
import com.ofni.dto.ClothResponse;
import com.ofni.security.UserPrincipal;
import com.ofni.service.ClothService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/cloths")
public class ClothController {

    private final ClothService service;

    public ClothController(ClothService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClothResponse> upload(
        @RequestParam("image") MultipartFile image
    ) throws java.io.IOException {
        var user = currentUser();
        var response = service.save(
            image.getOriginalFilename(),
            image.getBytes(),
            user.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<ClothResponse> list() {
        return service.findAllByUser(currentUser().userId());
    }

    @GetMapping("/{id}")
    public ClothResponse get(@PathVariable Long id) {
        return service.findById(id, currentUser().userId());
    }

    @PutMapping("/{id}")
    public ClothResponse update(
        @PathVariable Long id,
        @Valid @RequestBody ClothRequest request
    ) {
        return service.update(id, request, currentUser().userId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id, currentUser().userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slot/{slot}")
    public List<ClothResponse> bySlot(@PathVariable String slot) {
        return service.findBySlot(slot, currentUser().userId());
    }

    private UserPrincipal currentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext()
            .getAuthentication().getPrincipal();
    }
}
