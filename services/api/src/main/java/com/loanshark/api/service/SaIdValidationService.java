package com.loanshark.api.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Service;

@Service
public class SaIdValidationService {

    public boolean isValid(String idNumber) {
        if (idNumber == null || !idNumber.matches("\\d{13}")) {
            return false;
        }

        if (extractDateOfBirth(idNumber) == null) {
            return false;
        }

        return checksumMatches(idNumber);
    }

    public LocalDate extractDateOfBirth(String idNumber) {
        if (idNumber == null || idNumber.length() < 6) {
            return null;
        }

        int yearPart = Integer.parseInt(idNumber.substring(0, 2));
        int month = Integer.parseInt(idNumber.substring(2, 4));
        int day = Integer.parseInt(idNumber.substring(4, 6));

        int currentYearPart = LocalDate.now().getYear() % 100;
        int year = yearPart <= currentYearPart ? 2000 + yearPart : 1900 + yearPart;

        try {
            YearMonth.of(year, month);
            return LocalDate.of(year, month, day);
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private boolean checksumMatches(String idNumber) {
        int sumOdd = 0;
        for (int i = 0; i < 12; i += 2) {
            sumOdd += Character.getNumericValue(idNumber.charAt(i));
        }

        StringBuilder evenDigits = new StringBuilder();
        for (int i = 1; i < 12; i += 2) {
            evenDigits.append(idNumber.charAt(i));
        }

        int doubledEven = Integer.parseInt(evenDigits.toString()) * 2;
        int sumEven = 0;
        for (char digit : String.valueOf(doubledEven).toCharArray()) {
            sumEven += Character.getNumericValue(digit);
        }

        int total = sumOdd + sumEven;
        int checkDigit = (10 - (total % 10)) % 10;
        return checkDigit == Character.getNumericValue(idNumber.charAt(12));
    }
}
