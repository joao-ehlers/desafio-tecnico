package com.irrah.desafio_tecnico.client.validation;

import com.irrah.desafio_tecnico.client.DocumentType;
import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DocumentValidator {

    private static final Pattern CPF_INPUT = Pattern.compile(
            "(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})"
    );

    private static final Pattern CNPJ_INPUT = Pattern.compile(
            "(?:[A-Za-z0-9]{12}[0-9]{2}|"
                    + "[A-Za-z0-9]{2}\\.[A-Za-z0-9]{3}\\."
                    + "[A-Za-z0-9]{3}/[A-Za-z0-9]{4}-[0-9]{2})"
    );

    private static final int[] CPF_FIRST_WEIGHTS =
            {10, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int[] CPF_SECOND_WEIGHTS =
            {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int[] CNPJ_FIRST_WEIGHTS =
            {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private static final int[] CNPJ_SECOND_WEIGHTS =
            {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private DocumentValidator() {
    }

    public static String normalizeAndValidate(
            String documentId,
            DocumentType documentType
    ) {
        if (documentType == null) {
            throw new InvalidInputException(
                    "o tipo do documento é obrigatório"
            );
        }

        String normalized = normalize(documentId);

        boolean valid = switch (documentType) {
            case CPF -> isValidCpf(normalized);
            case CNPJ -> isValidCnpj(normalized);
        };

        if (!valid) {
            throw new InvalidInputException(
                    "documento inválido para o tipo " + documentType
            );
        }

        return normalized;
    }

    public static String normalizeAndValidate(String documentId) {
        String normalized = normalize(documentId);

        DocumentType type = normalized.length() == 11
                ? DocumentType.CPF
                : DocumentType.CNPJ;

        return normalizeAndValidate(normalized, type);
    }

    private static String normalize(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new InvalidInputException(
                    "o documento é obrigatório"
            );
        }

        String value = documentId.strip();


        if (!CPF_INPUT.matcher(value).matches()
                && !CNPJ_INPUT.matcher(value).matches()) {
            throw new InvalidInputException(
                    "formato de documento inválido"
            );
        }

        return value
                .replace(".", "")
                .replace("/", "")
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
    }

    private static boolean isValidCpf(String document) {
        if (!document.matches("[0-9]{11}")
                || hasAllCharactersEqual(document)) {
            return false;
        }

        String base = document.substring(0, 9);

        int firstDigit = calculateDigit(base, CPF_FIRST_WEIGHTS);
        int secondDigit = calculateDigit(
                base + firstDigit,
                CPF_SECOND_WEIGHTS
        );

        return document.equals(base + firstDigit + secondDigit);
    }

    private static boolean isValidCnpj(String document) {
        if (!document.matches("[A-Z0-9]{12}[0-9]{2}")
                || hasAllCharactersEqual(document)) {
            return false;
        }

        String base = document.substring(0, 12);

        int firstDigit = calculateDigit(base, CNPJ_FIRST_WEIGHTS);
        int secondDigit = calculateDigit(
                base + firstDigit,
                CNPJ_SECOND_WEIGHTS
        );

        return document.equals(base + firstDigit + secondDigit);
    }

    private static int calculateDigit(String base, int[] weights) {
        int sum = 0;

        for (int i = 0; i < weights.length; i++) {
            int value = base.charAt(i) - '0';
            sum += value * weights[i];
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean hasAllCharactersEqual(String value) {
        return value.chars()
                .allMatch(character -> character == value.charAt(0));
    }
}