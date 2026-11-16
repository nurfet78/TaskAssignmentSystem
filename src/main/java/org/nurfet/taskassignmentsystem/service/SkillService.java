package org.nurfet.taskassignmentsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nurfet.taskassignmentsystem.domain.entity.Skill;
import org.nurfet.taskassignmentsystem.dto.request.CreateSkillRequest;
import org.nurfet.taskassignmentsystem.dto.request.UpdateSkillRequest;
import org.nurfet.taskassignmentsystem.dto.response.PageResponse;
import org.nurfet.taskassignmentsystem.dto.response.SkillResponse;
import org.nurfet.taskassignmentsystem.dto.response.SkillUsageResponse;
import org.nurfet.taskassignmentsystem.exception.DuplicateResourceException;
import org.nurfet.taskassignmentsystem.exception.ResourceInUseException;
import org.nurfet.taskassignmentsystem.exception.ResourceNotFoundException;
import org.nurfet.taskassignmentsystem.mapper.SkillMapper;
import org.nurfet.taskassignmentsystem.repository.SkillRepository;
import org.nurfet.taskassignmentsystem.repository.projection.SkillUsageProjection;
import org.nurfet.taskassignmentsystem.util.Validate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name");

    Skill findById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.skill(id));
    }

    Set<Skill> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }

        Set<Long> requested = new HashSet<>(ids);
        Set<Skill> found = skillRepository.findByIdIn(requested);

        /*
         * Запрос "where id in (...)" возвращает только существующие строки и не сообщает,
         * что какие-то id не найдены. Без этой проверки неверный id молча отбрасывался бы:
         * задача создалась бы с неполным набором навыков (а значит, автоназначение выбрало бы
         * не того разработчика), а разработчик остался бы без части навыков, и клиент получил бы
         * успешный ответ. Поэтому сравниваем число запрошенных уникальных id с числом найденных
         * и при расхождении сообщаем, какого навыка нет.
         * Сравнение идёт с requested, а не с ids: дубликаты в списке не должны считаться потерями.
         */

        if (found.size() != requested.size()) {
            Set<Long> foundIds = found.stream().map(Skill::getId).collect(Collectors.toSet());
            Long missing = requested.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw ResourceNotFoundException.skill(missing);
        }

        return found;
    }

    private boolean isNameAvailable(Skill skill, String name) {
        return skill.getName().equalsIgnoreCase(name)
                || !skillRepository.existsByNameIgnoreCase(name);
    }

    /**
     * Сохраняет и сразу делает flush, чтобы нарушение unique по name
     * возникло здесь, а не при коммите, и превращает его в понятную клиенту ошибку.
     */
    private Skill saveAndFlushWithUniqueName(Skill skill) {
        try {
            return skillRepository.saveAndFlush(skill);
        } catch (DataIntegrityViolationException e) {
            throw DuplicateResourceException.skillName(skill.getName());
        }
    }

    private void deleteAndFlushIfNotInUse(Skill skill) {
        try {
            skillRepository.delete(skill);
            skillRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw ResourceInUseException.skill(skill.getId());
        }
    }

    @Transactional
    public SkillResponse create(CreateSkillRequest request) {
        String name = Validate.notBlank(request.name(), "name", 100);
        log.info("Создание навыка: {}", name);

        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw DuplicateResourceException.skillName(name);
        }

        Skill skill = skillMapper.toEntity(request);
        skill = saveAndFlushWithUniqueName(skill);

        log.info("Навык создан: id={}, name={}", skill.getId(), skill.getName());

        return skillMapper.toResponse(skill);
    }

    @Transactional
    public SkillResponse update(Long id, UpdateSkillRequest request) {
        log.info("Обновление навыка: {}", id);

        String name = request.name() == null
                ? null
                : Validate.notBlank(request.name(), "name", 100);

        Skill skill = findById(id);
        if (name != null && !isNameAvailable(skill, name)) {
            throw DuplicateResourceException.skillName(name);
        }

        skill.update(name, request.description());

        /*
         * Используем saveAndFlushWithUniqueName, а не простой flush(): метод переводит
         * нарушение уникальности имени в понятную ошибку.
         *
         * В update навыка может сработать уникальный индекс по lower(name): два параллельных
         * запроса оба проходят isNameAvailable, и второй упирается в индекс при flush.
         * С голым flush() клиент получил бы общий DataIntegrityViolationException
         * вместо DuplicateResourceException.
         *
         * Отличие от DeveloperService.update: там достаточно flush(), потому что email
         * в нём не меняется (для этого есть отдельный changeEmail), и перехватывать нечего.
         */

        saveAndFlushWithUniqueName(skill);

        log.info("Навык сохранен: id={}, name={}", skill.getId(), skill.getName());

        return skillMapper.toResponse(skill);
    }

    @Transactional(readOnly = true)
    public SkillResponse getById(Long id) {
        return skillMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public SkillResponse getByName(String name) {
        String normalized = Validate.notBlank(name, "name", 100);
        return skillRepository.findByNameIgnoreCase(normalized)
                .map(skillMapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.skillByName(normalized));
    }

    @Transactional(readOnly = true)
    public PageResponse<SkillResponse> getAll(String q, Pageable pageable) {
        Validate.validateSort(pageable, SORTABLE_FIELDS);
        Pageable stable = Validate.withIdTiebreaker(pageable);

        String query = q == null ? "" : q.strip();
        Page<Skill> page = query.isEmpty()
                ? skillRepository.findAll(stable)
                : skillRepository.findByNameContainingIgnoreCase(query, stable);

        return PageResponse.of(skillMapper.toResponseList(page.getContent()), page);
    }

    @Transactional(readOnly = true)
    public SkillUsageResponse getUsage(Long id) {
        findById(id); // 404 для неизвестного навыка

        SkillUsageProjection usage = skillRepository.getUsage(id);
        long developers = usage.getDevelopersCount();
        long tasks = usage.getTasksCount();

        return new SkillUsageResponse(developers, tasks, developers + tasks > 0);
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = findById(id);

        if (skillRepository.isInUse(id)) {
            throw ResourceInUseException.skill(id);
        }

        deleteAndFlushIfNotInUse(skill);
        log.info("Навык {} удален", id);
    }
}