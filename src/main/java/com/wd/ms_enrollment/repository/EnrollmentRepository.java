package com.wd.ms_enrollment.repository;

import com.world_dance.wd_lib_common.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndEventIdAndModalityId(Long userId, Long eventId, Long modalityId);

    // RF-29: Permite al Organizador listar todos los inscritos de una categoría específica
    List<Enrollment> findByModalityId(Long modalityId);

    // RF-30: Permite al Participante ver todas sus inscripciones activas y sus estados
    List<Enrollment> findByUserId(Long userId);
}
