package com.sqi.lostandfound.repository;

import com.sqi.lostandfound.model.LostItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostItemRepository extends MongoRepository<LostItem, String> {

    // Feed: all items sorted by time — handled via controller + Sort
    List<LostItem> findByBranchOrderByTimestampDesc(String branch);

    // Filter by status (e.g. show only SEARCHING items)
    List<LostItem> findByStatusOrderByTimestampDesc(LostItem.ItemStatus status);

    // Filter by campus location
    List<LostItem> findBySqiLocationOrderByTimestampDesc(LostItem.SqiLocation sqiLocation);

    // Anti-redundancy search: check if a similar item was already reported
    List<LostItem> findByItemNameContainingIgnoreCaseAndStatus(
            String itemName,
            LostItem.ItemStatus status
    );
}