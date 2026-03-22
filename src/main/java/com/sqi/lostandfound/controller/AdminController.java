package com.sqi.lostandfound.controller;

import com.sqi.lostandfound.model.Admin;
import com.sqi.lostandfound.model.LostItem;
import com.sqi.lostandfound.repository.AdminRepository;
import com.sqi.lostandfound.repository.LostItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminRepository adminRepository;
    private final LostItemRepository lostItemRepository;

    // ── POST /api/admin/login ──────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid credentials"
                ));

        if (!admin.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return ResponseEntity.ok(Map.of(
                "id",       admin.getId(),
                "username", admin.getUsername(),
                "role",     admin.getRole().name(),
                "branch",   admin.getBranch() != null ? admin.getBranch() : "ALL"
        ));
    }

    // ── GET /api/admin/items ───────────────────────────────────────────────
    // Super admin gets all, branch admin gets their branch only
    @GetMapping("/items")
    public ResponseEntity<List<LostItem>> getItems(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String adminUsername
    ) {
        Admin admin = adminRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Admin not found"
                ));

        List<LostItem> items;

        if (admin.getRole() == Admin.AdminRole.SUPER_ADMIN) {
            if (branch != null && !branch.equals("ALL")) {
                items = lostItemRepository.findByBranchOrderByTimestampDesc(branch);
            } else {
                items = lostItemRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
            }
        } else {
            items = lostItemRepository.findByBranchOrderByTimestampDesc(admin.getBranch());
        }

        return ResponseEntity.ok(items);
    }

    // ── DELETE /api/admin/items/{id} ───────────────────────────────────────
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable String id,
            @RequestParam String adminUsername
    ) {
        Admin admin = adminRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Admin not found"
                ));

        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"
                ));

        // Branch admin can only delete their own branch items
        if (admin.getRole() == Admin.AdminRole.BRANCH_ADMIN) {
            if (!item.getBranch().equals(admin.getBranch())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        lostItemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── PUT /api/admin/items/{id}/status ───────────────────────────────────
    @PutMapping("/items/{id}/status")
    public ResponseEntity<LostItem> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"
                ));

        try {
            item.setStatus(LostItem.ItemStatus.valueOf(body.get("status").toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }

        return ResponseEntity.ok(lostItemRepository.save(item));
    }

    // ── POST /api/admin/seed ───────────────────────────────────────────────
    // Run once to create your admin accounts
    @PostMapping("/seed")
    public ResponseEntity<String> seedAdmins() {
        if (adminRepository.count() > 0) {
            return ResponseEntity.ok("Admins already seeded");
        }

        List<Admin> admins = List.of(
                // Super admin — you
                Admin.builder()
                        .username("superadmin")
                        .password("SQI@Admin2024!")
                        .role(Admin.AdminRole.SUPER_ADMIN)
                        .branch(null)
                        .build(),
                // Branch admins
                Admin.builder().username("admin_lagos")
                        .password("Lagos@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("LAGOS").build(),
                Admin.builder().username("admin_abuja")
                        .password("Abuja@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("ABUJA").build(),
                Admin.builder().username("admin_ibadan_dugbe")
                        .password("Dugbe@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("IBADAN_DUGBE").build(),
                Admin.builder().username("admin_ibadan_challenge")
                        .password("Challenge@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("IBADAN_CHALLENGE").build(),
                Admin.builder().username("admin_ibadan_iwo")
                        .password("IwoRoad@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("IBADAN_IWO_ROAD").build(),
                Admin.builder().username("admin_ogbomoso")
                        .password("Ogbomoso@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("OGBOMOSO").build(),
                Admin.builder().username("admin_abeokuta")
                        .password("Abeokuta@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("ABEOKUTA").build(),
                Admin.builder().username("admin_osogbo")
                        .password("Osogbo@2024").role(Admin.AdminRole.BRANCH_ADMIN).branch("OSOGBO").build()
        );

        adminRepository.saveAll(admins);
        return ResponseEntity.ok("Admins seeded successfully");
    }
}