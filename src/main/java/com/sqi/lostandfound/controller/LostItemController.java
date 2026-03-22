package com.sqi.lostandfound.controller;

import lombok.extern.slf4j.Slf4j;
import com.sqi.lostandfound.model.LostItem;
import com.sqi.lostandfound.repository.LostItemRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Angular dev server
public class LostItemController {
    private final NotificationService notificationService;
    @PostMapping("/{id}/claim")
    public ResponseEntity<LostItem> claimItem(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        LostItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found: " + id
                ));

        String claimedBy     = body.get("claimedBy");
        String claimContact  = body.get("claimContact");

        if (claimedBy == null || claimContact == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "claimedBy and claimContact are required"
            );
        }

        item.setClaimedBy(claimedBy);
        item.setClaimContact(claimContact);
        item.setStatus(LostItem.ItemStatus.FOUND_PENDING);
        LostItem saved = repository.save(item);

        // Fire notifications asynchronously
        try {
            notificationService.notifyOnClaim(saved, claimedBy, claimContact);
        } catch (Exception e) {
            log.warn("Notification failed but claim was saved: {}", e.getMessage());
        }

        return ResponseEntity.ok(saved);
    }


    private final LostItemRepository repository;

    // ── GET /api/items ─────────────────────────────────────────────────────
    // Main feed: all items, newest first
    @GetMapping
    public ResponseEntity<List<LostItem>> getAllItems(
            @RequestParam(required = false) LostItem.ItemStatus status,
            @RequestParam(required = false) LostItem.SqiLocation location
    ) {
        if (status != null) {
            return ResponseEntity.ok(repository.findByStatusOrderByTimestampDesc(status));
        }
        if (location != null) {
            return ResponseEntity.ok(repository.findBySqiLocationOrderByTimestampDesc(location));
        }
        // Default: full feed sorted newest first
        List<LostItem> items = repository.findAll(
                Sort.by(Sort.Direction.DESC, "timestamp")
        );
        return ResponseEntity.ok(items);
    }

    // ── GET /api/items/{id} ────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<LostItem> getItemById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found: " + id
                ));
    }

    // ── GET /api/items/search?name=wallet ──────────────────────────────────
    // Anti-redundancy endpoint: check before you post
    @GetMapping("/search")
    public ResponseEntity<List<LostItem>> searchByName(@RequestParam String name) {
        List<LostItem> matches = repository
                .findByItemNameContainingIgnoreCaseAndStatus(
                        name,
                        LostItem.ItemStatus.SEARCHING
                );
        return ResponseEntity.ok(matches);
    }

    // ── GET /api/items/locations ───────────────────────────────────────────
    // Provide enum values to Angular dropdown — avoids hardcoding on frontend
    @GetMapping("/locations")
    public ResponseEntity<LostItem.SqiLocation[]> getLocations() {
        return ResponseEntity.ok(LostItem.SqiLocation.values());
    }

    // ── POST /api/items ────────────────────────────────────────────────────
    // Report a new lost or found item
    @PostMapping
    public ResponseEntity<LostItem> createItem(@Valid @RequestBody LostItem item) {
        // Ensure client cannot force a pre-set ID
        item.setId(null);
        LostItem saved = repository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── PUT /api/items/{id}/status ─────────────────────────────────────────
    // Update item status only (the key action in the tracker)
    @PutMapping("/{id}/status")
    public ResponseEntity<LostItem> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        LostItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found: " + id
                ));

        String rawStatus = body.get("status");
        if (rawStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'status' field is required");
        }

        try {
            item.setStatus(LostItem.ItemStatus.valueOf(rawStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Valid values: SEARCHING, FOUND_PENDING, REUNITED"
            );
        }

        return ResponseEntity.ok(repository.save(item));
    }

    // ── PUT /api/items/{id} ────────────────────────────────────────────────
    // Full item update (admin use)
    @PutMapping("/{id}")
    public ResponseEntity<LostItem> updateItem(
            @PathVariable String id,
            @Valid @RequestBody LostItem updatedItem
    ) {
        LostItem existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found: " + id
                ));

        updatedItem.setId(existing.getId());
        updatedItem.setTimestamp(existing.getTimestamp()); // preserve original timestamp
        return ResponseEntity.ok(repository.save(updatedItem));
    }

    // ── POST /api/items/{id}/claim ─────────────────────────────────────────
    @PostMapping("/{id}/claim")
    public ResponseEntity<LostItem> claimItem(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        LostItem item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found: " + id
                ));

        String claimedBy = body.get("claimedBy");
        String claimContact = body.get("claimContact");

        if (claimedBy == null || claimContact == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "claimedBy and claimContact are required"
            );
        }

        item.setClaimedBy(claimedBy);
        item.setClaimContact(claimContact);
        item.setStatus(LostItem.ItemStatus.FOUND_PENDING);
        return ResponseEntity.ok(repository.save(item));
    }

    // ── DELETE /api/items/{id} ─────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}