package org.nurfet.taskassignmentsystem.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.ColumnDefault;
import org.nurfet.taskassignmentsystem.util.Validate;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "developers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Developer extends AuditableEntity {

    @Version
    private Long version;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "active", nullable = false)
    @ColumnDefault("true")
    private boolean active;

    @ManyToMany
    @JoinTable(
            name = "developer_skills",
            joinColumns = @JoinColumn(name = "developer_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    public Developer(String firstName, String lastName, String email,
                     Boolean active) {

        this.firstName = Validate.notBlank(firstName, "firstName", 100);
        this.lastName = Validate.notBlank(lastName, "lastName", 100);
        this.email = Validate.notBlank(email, "email", 254);
        this.active = active == null || active;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void addSkill(Skill skill) {
        skills.add(skill);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void removeSkill(Skill skill) {
        skills.remove(skill);
    }

    public boolean hasSkill(Skill requiredSkill) {
        return skills.contains(requiredSkill);
    }

    public boolean hasAllSkills(Set<Skill> requiredSkills) {
        return skills.containsAll(requiredSkills);
    }

    public boolean hasAnySkill(Set<Skill> requiredSkills) {
        return requiredSkills.stream().anyMatch(skills::contains);
    }

    public long countMatchingSkills(Set<Skill> requiredSkills) {
        return requiredSkills.stream()
                .filter(skills::contains)
                .count();
    }

    public void update(String firstName, String lastName, Boolean active) {
        if (firstName != null) {
            this.firstName = Validate.notBlank(firstName, "firstName", 100);
        }

        if (lastName != null) {
            this.lastName = Validate.notBlank(lastName, "lastName", 100);
        }

        if (active != null) {
            this.active = active;
        }
    }

    public void replaceSkills(Set<Skill> skills) {
        this.skills.clear();
        skills.forEach(this::addSkill);
    }

    public void changeEmail(String email) {
        this.email = Validate.notBlank(email, "email", 254);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Developer developer)) return false;
        if (getId() == null || developer.getId() == null) return false;
        return Objects.equals(getId(), developer.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return "Developer{" +
                "id=" + getId() +
                ", fullName='" + getFullName() + '\'' +
                ", active=" + active +
                ", email='" + email + '\'' +
                '}';
    }
}