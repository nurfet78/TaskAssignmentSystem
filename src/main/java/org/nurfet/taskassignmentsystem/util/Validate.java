package org.nurfet.taskassignmentsystem.util;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

public final class Validate {

    private Validate() {}

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Поле %s должно быть заполнено".formatted(field)
            );
        }
        return value;
    }

    public static String notBlank(String value, String field) {
        String checked = notNull(value, field).strip();

        if (checked.isBlank()) {
            throw new IllegalArgumentException(
                    "Поле %s не может быть пустым".formatted(field)
            );
        }

        return checked;
    }

    public static String notBlank(String value, String field, int maxLength) {
        String checked = notBlank(value, field);

        if (checked.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Поле %s не должно превышать %d символов"
                            .formatted(field, maxLength)
            );
        }

        return checked;
    }

    public static void validateSort(Pageable pageable, Set<String> allowedFields) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Недопустимое поле сортировки '%s'. Допустимые поля: %s"
                                .formatted(order.getProperty(),
                                        String.join(", ", new TreeSet<>(allowedFields))));
            }
        }
    }
}