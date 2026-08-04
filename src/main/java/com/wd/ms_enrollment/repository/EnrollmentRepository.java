package com.wd.ms_enrollment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.world_dance.wd_lib_common.entity.Enrollment;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserIdAndEventIdAndModalityId(Long userId, Long eventId, Long modalityId);
}
