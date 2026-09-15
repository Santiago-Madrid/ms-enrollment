package com.wd.ms_enrollment.repository;

import java.util.Optional;

import com.world_dance.wd_lib_common.entity.UserEventRole;
import com.world_dance.wd_lib_common.enums.EventRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEventRoleRepository extends JpaRepository<UserEventRole, Long> {
    boolean existsByUserIdAndEventIdAndRoleInEvent(Long userId, Long eventId, EventRole eventRole);
    Optional<UserEventRole> findByUserIdAndEventId(Long userId, Long eventId);
    Optional<UserEventRole> findByUserIdAndEventIdAndRoleInEvent(Long userId, Long eventId, EventRole eventRole);
}