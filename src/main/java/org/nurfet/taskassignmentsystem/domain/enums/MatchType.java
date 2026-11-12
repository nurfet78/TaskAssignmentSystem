package org.nurfet.taskassignmentsystem.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

// enumAsRef = true выносит enum
// в отдельную схему (components/schemas), и все поля этого типа ссылаются на неё
// через $ref, а не повторяют список значений и описание в каждом DTO.

@Schema(enumAsRef = true, description = """
        Тип совпадения навыков при автоназначении:
        FULL: разработчик владеет всеми требуемыми навыками;
        PARTIAL: разработчик владеет частью требуемых навыков""")
public enum MatchType {
    FULL,
    PARTIAL
}