package org.nurfet.taskassignmentsystem.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.nurfet.taskassignmentsystem.util.Validate;

import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "skills")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Skill extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    public Skill(String name, String description) {
        this.name = Validate.notBlank(name, "name", 100);
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Skill skill)) return false;
        if (getName() == null || skill.getName() == null) return false;
        return Objects.equals(getName(), skill.getName());
    }

    public void update(String name, String description) {
        if (name != null) {
            this.name = Validate.notBlank(name, "name", 100);
        }

        if (description != null) {
            this.description = description;
        }
    }

    public void changeName(String name) {
        this.name = Validate.notBlank(name, "name", 100);
    }

    public static String normalizeKey(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                '}';
    }
}