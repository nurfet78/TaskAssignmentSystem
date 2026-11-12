package org.nurfet.taskassignmentsystem.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailValidator.class)
@Target({ElementType.FIELD, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    String message() default "Некорректный адрес электронной почты";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}