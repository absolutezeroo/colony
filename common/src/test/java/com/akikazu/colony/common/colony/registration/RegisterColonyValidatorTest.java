package com.akikazu.colony.common.colony.registration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterColonyValidatorTest
{
    @Test
    void acceptsBoundaryLengthLowerEdge()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("abc");

        assertTrue(result.isEmpty());
    }

    @Test
    void acceptsBoundaryLengthUpperEdge()
    {
        String name = "a".repeat(RegisterColonyValidator.MAX_NAME_LENGTH);

        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName(name);

        assertTrue(result.isEmpty());
    }

    @Test
    void acceptsAllAllowedCharacterClasses()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("Town-A_1 alpha");

        assertTrue(result.isEmpty());
    }

    @Test
    void rejectsTooShortName()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("ab");

        assertEquals(Optional.of(RegistrationRejection.NAME_LENGTH), result);
    }

    @Test
    void rejectsEmptyName()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("");

        assertEquals(Optional.of(RegistrationRejection.NAME_LENGTH), result);
    }

    @Test
    void rejectsTooLongName()
    {
        String name = "a".repeat(RegisterColonyValidator.MAX_NAME_LENGTH + 1);

        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName(name);

        assertEquals(Optional.of(RegistrationRejection.NAME_LENGTH), result);
    }

    @Test
    void rejectsNameWithInvalidPunctuation()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("Bad!Name");

        assertEquals(Optional.of(RegistrationRejection.NAME_CHARS), result);
    }

    @Test
    void rejectsNameWithUnicode()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("Tówñ");

        assertEquals(Optional.of(RegistrationRejection.NAME_CHARS), result);
    }

    @Test
    void rejectsNameWithSlash()
    {
        Optional<RegistrationRejection> result = RegisterColonyValidator.validateName("path/with/slash");

        assertEquals(Optional.of(RegistrationRejection.NAME_CHARS), result);
    }

    @Test
    void rangeAcceptsSamePoint()
    {
        boolean inRange = RegisterColonyValidator.isWithinRange(0, 0, 0, 0, 0, 0, 64);

        assertTrue(inRange);
    }

    @Test
    void rangeAcceptsAtExactLimit()
    {
        boolean inRange = RegisterColonyValidator.isWithinRange(0, 0, 0, 64, 0, 0, 64);

        assertTrue(inRange);
    }

    @Test
    void rangeRejectsBeyondLimit()
    {
        boolean inRange = RegisterColonyValidator.isWithinRange(0, 0, 0, 64.5, 0, 0, 64);

        assertFalse(inRange);
    }
}
