package org.nurfet.taskassignmentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nurfet.taskassignmentsystem.domain.entity.Developer;
import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.nurfet.taskassignmentsystem.dto.request.ChangeEmailRequest;
import org.nurfet.taskassignmentsystem.dto.request.CreateDeveloperRequest;
import org.nurfet.taskassignmentsystem.dto.request.UpdateDeveloperRequest;
import org.nurfet.taskassignmentsystem.dto.response.DeveloperResponse;
import org.nurfet.taskassignmentsystem.dto.response.PageResponse;
import org.nurfet.taskassignmentsystem.exception.DuplicateResourceException;
import org.nurfet.taskassignmentsystem.exception.EntityConcurrentModificationException;
import org.nurfet.taskassignmentsystem.exception.InvalidDeveloperStateException;
import org.nurfet.taskassignmentsystem.exception.ResourceNotFoundException;
import org.nurfet.taskassignmentsystem.mapper.DeveloperMapper;
import org.nurfet.taskassignmentsystem.repository.DeveloperRepository;
import org.nurfet.taskassignmentsystem.repository.TaskRepository;
import org.nurfet.taskassignmentsystem.repository.spec.DeveloperSpecifications;
import org.nurfet.taskassignmentsystem.util.Validate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeveloperService {

    private final DeveloperRepository developerRepository;
    private final TaskRepository taskRepository;
    private final SkillService skillService;
    private final DeveloperMapper developerMapper;

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "firstName", "lastName", "email", "active", "createdAt", "updatedAt");

    Developer findById(Long id) {
        return developerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.developer(id));
    }

    Developer findByIdWithLock(Long id) {
        return developerRepository.findByIdWithLock(id)
                .orElseThrow(() -> ResourceNotFoundException.developer(id));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private boolean isEmailAvailable(Developer developer, String email) {
        return developer.getEmail().equals(email)
                || !developerRepository.existsByEmail(email);
    }

    private void checkVersion(Developer developer, Long version) {
        if (!developer.getVersion().equals(version)) {
            throw EntityConcurrentModificationException.developer(developer.getId());
        }
    }

    private static final String EMAIL_UNIQUE_INDEX = "uk_developers_email";

    private static boolean isEmailConstraintViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
                && EMAIL_UNIQUE_INDEX.equalsIgnoreCase(cve.getConstraintName());
    }

    private Developer saveAndFlushWithUniqueEmail(Developer developer) {
        try {
            return developerRepository.saveAndFlush(developer);
        } catch (DataIntegrityViolationException e) {
            if (isEmailConstraintViolation(e)) {
                throw DuplicateResourceException.developerEmail(developer.getEmail());
            }

            throw e;
        }
    }

    private DeveloperResponse toResponseWithTaskCount(Developer developer) {
        long taskCount = taskRepository.countActiveTaskByAssigneeId(developer.getId());
        return developerMapper.toResponseWithTaskCount(developer, taskCount);
    }

    private List<DeveloperResponse> toResponses(List<Developer> developers) {
        if (developers.isEmpty()) {
            return List.of();
        }

        List<Long> ids = developers.stream().map(Developer::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        taskRepository.countActiveTasksByAssigneeIds(ids)
                .forEach(row -> counts.put(row.getAssigneeId(), row.getTaskCount()));

        return developers.stream()
                .map(d -> developerMapper
                        .toResponseWithTaskCount(d, counts.getOrDefault(d.getId(), 0L)))
                .toList();
    }

    @Transactional
    public DeveloperResponse create(CreateDeveloperRequest request) {
        log.info("Создание разработчика");

        String email = normalizeEmail(request.email());

        if (developerRepository.existsByEmail(email)) {
            throw DuplicateResourceException.developerEmail(email);
        }

        Developer developer = developerMapper.toEntity(request);
        developer.changeEmail(email);

        if (request.skillIds() != null && !request.skillIds().isEmpty()) {
            Set<Skill> skills = skillService.findByIds(request.skillIds());
            skills.forEach(developer::addSkill);
        }

        developer = saveAndFlushWithUniqueEmail(developer);
        log.info("Разработчик создан: {}", developer.getId());

        return developerMapper.toResponse(developer);
    }

    @Transactional
    public DeveloperResponse update(Long id, UpdateDeveloperRequest request) {
        log.info("Обновление разработчика: {}", id);

        boolean deactivating = Boolean.FALSE.equals(request.active());

        Developer developer = deactivating ? findByIdWithLock(id) : findById(id);
        checkVersion(developer, request.version());

        if (deactivating && developer.isActive()) {
            long activeTasks = taskRepository.countActiveTaskByAssigneeId(id);
            if (activeTasks > 0) {
                throw InvalidDeveloperStateException.cannotDeactivate(id, activeTasks);
            }
        }

        developer.update(request.firstName(), request.lastName(), request.active());

        if (request.skillIds() != null) {
            developer.replaceSkills(skillService.findByIds(request.skillIds()));
        }

        developerRepository.flush();
        log.info("Разработчик обновлен: {}", developer.getId());

        return toResponseWithTaskCount(developer);
    }

    @Transactional
    public DeveloperResponse changeEmail(Long id, ChangeEmailRequest request) {
        log.info("Смена адреса электронной почты разработчика: {}", id);

        Developer developer = findById(id);
        checkVersion(developer, request.version());

        String email = normalizeEmail(request.newEmail());
        if (!isEmailAvailable(developer, email)) {
            throw DuplicateResourceException.developerEmail(email);
        }

        developer.changeEmail(email);
        saveAndFlushWithUniqueEmail(developer);

        log.info("Email обновлен: {}", developer.getId());

        return toResponseWithTaskCount(developer);
    }

    @Transactional(readOnly = true)
    public DeveloperResponse getById(Long id) {
        return toResponseWithTaskCount(findById(id));
    }

    @Transactional(readOnly = true)
    public DeveloperResponse getByEmail(String email) {
        String normalized = normalizeEmail(email);
        return developerRepository.findByEmail(normalized)
                .map(this::toResponseWithTaskCount)
                .orElseThrow(() -> ResourceNotFoundException.developerEmail(normalized));
    }

    @Transactional(readOnly = true)
    public PageResponse<DeveloperResponse> getAll(boolean activeOnly, String q, Pageable pageable) {
        Validate.validateSort(pageable, SORTABLE_FIELDS);
        Pageable stable = Validate.withIdTiebreaker(pageable);

        Page<Developer> page = developerRepository.findAll(
                DeveloperSpecifications.byFilter(activeOnly, q), stable);

        return PageResponse.of(toResponses(page.getContent()), page);
    }

    @Transactional(readOnly = true)
    public long getActiveTaskCount(Long developerId) {
        if (!developerRepository.existsById(developerId)) {
            throw ResourceNotFoundException.developer(developerId);
        }

        return taskRepository.countActiveTaskByAssigneeId(developerId);
    }
}