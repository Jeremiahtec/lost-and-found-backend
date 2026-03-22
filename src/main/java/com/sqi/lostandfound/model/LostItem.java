package com.sqi.lostandfound.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "lost_items")
public class LostItem {

    @Id
    private String id;

    @NotBlank(message = "Item name is required")
    private String itemName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Location is required")
    private SqiLocation sqiLocation;

    @Builder.Default
    @Indexed
    private ItemStatus status = ItemStatus.SEARCHING;

    // Optional: URL to uploaded image (Phase 2 — file upload)
    private String imageUrl;

    // Automatically set on insert via @EnableMongoAuditing
    @CreatedDate
    private Instant timestamp;

    // Reporter's contact or student ID (optional for Phase 1)
    private String reportedBy;
    // Who claimed this item
    private String claimedBy;
    private String branch;
    // Reporter's contact — phone or email
    private String reporterContact;

    // Contact info of the claimer
    private String claimContact;

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum ItemStatus {
        SEARCHING,             // Yellow/Amber — actively missing
        FOUND_PENDING,         // Blue — found, awaiting verification/pickup
        REUNITED               // Green — returned to owner
    }

    public enum SqiLocation {
        LAB_A("Lab A"),
        LAB_B("Lab B"),
        LAB_C("Lab C"),
        MAIN_HALL("Main Hall"),
        CAFETERIA("Cafeteria"),
        LIBRARY("Library"),
        ADMIN_BLOCK("Admin Block"),
        LECTURE_HALL_1("Lecture Hall 1"),
        LECTURE_HALL_2("Lecture Hall 2"),
        RECEPTION("Reception"),
        PARKING_LOT("Parking Lot"),
        RESTROOMS("Restrooms"),
        OTHER("Other");

        private final String displayName;

        SqiLocation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}