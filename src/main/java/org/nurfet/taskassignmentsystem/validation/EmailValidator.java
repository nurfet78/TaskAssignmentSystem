package org.nurfet.taskassignmentsystem.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^(?=.{1,254}$)(?=.{1,64}@)"
                    + "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
                    + "(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return EMAIL_PATTERN.matcher(value).matches();
    }
}