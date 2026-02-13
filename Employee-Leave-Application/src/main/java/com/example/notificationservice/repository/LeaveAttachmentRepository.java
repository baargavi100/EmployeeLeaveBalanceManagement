// ═══════════════════════════════════════════════════════════════════
// FILE: LeaveAttachmentRepository.java (NEW)
// Location: src/main/java/com/example/notificationservice/repository/
// ═══════════════════════════════════════════════════════════════════

package com.example.notificationservice.repository;

import com.example.notificationservice.entity.LeaveAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, Long> {

    /**
     * Find all attachments for a leave application
     */
    List<LeaveAttachment> findByLeaveApplicationId(Long leaveApplicationId);

    /**
     * Delete all attachments for a leave application
     */
    void deleteByLeaveApplicationId(Long leaveApplicationId);

    /**
     * Count attachments for a leave application
     */
    int countByLeaveApplicationId(Long leaveApplicationId);
}