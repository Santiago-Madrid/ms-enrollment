package com.wd.ms_enrollment.service;

import com.wd.ms_enrollment.dto.RejectionRequest;
import com.wd.ms_enrollment.exception.ResourceNotFoundException;
import com.wd.ms_enrollment.repository.EnrollmentRepository;
import com.world_dance.wd_lib_common.dto.EnrollmentResponseDto;
import com.world_dance.wd_lib_common.entity.Enrollment;
import com.world_dance.wd_lib_common.enums.EnrollmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    // RF-25 y RF-26: Inscribirse en categoría
    @Transactional
    public EnrollmentResponseDto createEnrollment(Long userId, Long eventId, Long modalityId) {
        if (enrollmentRepository.existsByUserIdAndEventIdAndModalityId(userId, eventId, modalityId)) {
            throw new IllegalStateException("Ya te encuentras inscrito en esta categoría para este evento.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setEventId(eventId);
        enrollment.setModalityId(modalityId);
        enrollment.setStatus(EnrollmentStatus.PENDING);

        return convertToDto(enrollmentRepository.save(enrollment));
    }

    // RF-27: Aprobar inscripción manual
    @Transactional
    public EnrollmentResponseDto approveEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con id: " + enrollmentId));

        enrollment.setStatus(EnrollmentStatus.APPROVED);
        enrollment.setRejectionReason(null); // Limpiamos cualquier rechazo previo si existía
        return convertToDto(enrollmentRepository.save(enrollment));
    }

    // RF-28: Rechazar inscripción con justificación
    @Transactional
    public EnrollmentResponseDto rejectEnrollment(Long enrollmentId, RejectionRequest rejectionRequest) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con id: " + enrollmentId));

        enrollment.setStatus(EnrollmentStatus.REJECTED);
        enrollment.setRejectionReason(rejectionRequest.getReason());
        return convertToDto(enrollmentRepository.save(enrollment));
    }

    // RF-29: Consultar inscritos por categoría (Para Organizadores)
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> getEnrollmentsByCategory(Long modalityId) {
        return enrollmentRepository.findByModalityId(modalityId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // RF-30: Consultar inscripciones del participante autenticado
    @Transactional(readOnly = true)
    public List<EnrollmentResponseDto> getMyEnrollments(Long userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Método helper privado para transformar la entidad interna 
     * al DTO común que espera tu controlador.
     */
    private EnrollmentResponseDto convertToDto(Enrollment enrollment) {
        EnrollmentResponseDto dto = new EnrollmentResponseDto();
        dto.setId(enrollment.getId());
        dto.setUserId(enrollment.getUserId());
        dto.setEventId(enrollment.getEventId());
        
        // Mapeamos de forma explícita la variable modalityId al categoryId del DTO
        dto.setCategoryId(enrollment.getModalityId());        
        // Convertimos el Enum a String si el DTO lo maneja como String text
        dto.setStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null);
        dto.setRejectionReason(enrollment.getRejectionReason());
        
        return dto;
    }
}