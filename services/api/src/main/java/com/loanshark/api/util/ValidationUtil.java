package com.loanshark.api.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class ValidationUtil {

    // =========================================================
    // 1. PDF AMOUNT + DATE VALIDATION (EXISTING)
    // =========================================================
    public static BigDecimal extractAmountFromPdfAndValidateDate(String base64Pdf) {
        try {
            String base64Content = base64Pdf.contains(",") ? base64Pdf.split(",")[1] : base64Pdf;
            byte[] pdfBytes = Base64.getDecoder().decode(base64Content);

            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                String cleanedText = text.replaceAll("\\s+", "");

                Pattern amountPattern = Pattern.compile(
                        "R?\\s*(\\d{1,3}(?:[ ,]\\d{3})*(?:\\.\\d{2})|\\d+\\.\\d{2})"
                );

                Matcher amountMatcher = amountPattern.matcher(cleanedText);
                BigDecimal largestAmount = null;

                while (amountMatcher.find()) {
                    String value = amountMatcher.group(1).replaceAll("[ ,]", "");
                    try {
                        BigDecimal amount = new BigDecimal(value);
                        if (largestAmount == null || amount.compareTo(largestAmount) > 0) {
                            largestAmount = amount;
                        }
                    } catch (Exception ignored) {}
                }

                if (largestAmount == null) {
                    throw new ResponseStatusException(BAD_REQUEST, "No valid amount found");
                }

                validateTodayDate(text);

                return largestAmount;
            }

        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
        }
    }

    // =========================================================
    // 2. SA ID PDF VALIDATION
    // =========================================================
    public static boolean isValidSouthAfricanIdPdf(MultipartFile idFile) {
        if (idFile.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "ID document required");
        }

        try {
            // Convert PDF to Base64 (optional, but keeping your previous logic)
            String base64Pdf = Base64.getEncoder().encodeToString(idFile.getBytes());
            String base64Content = base64Pdf.contains(",") ? base64Pdf.split(",")[1] : base64Pdf;
            byte[] pdfBytes = Base64.getDecoder().decode(base64Content);

            // Load PDF and extract text
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                String text = new PDFTextStripper().getText(document);

                // Clean text: keep digits, spaces, and dashes
                String cleanedText = text.replaceAll("[^0-9\\- ]", " ");

                // Regex to match 13-digit IDs with optional spaces/dashes
                Pattern idPattern = Pattern.compile("\\b\\d{6}[- ]?\\d{4}[- ]?\\d{3}\\b");
                Matcher matcher = idPattern.matcher(cleanedText);

                while (matcher.find()) {
                    // Remove spaces/dashes before validation
                    String potentialId = matcher.group().replaceAll("[- ]", "");
                    if (isValidSAIdNumber(potentialId)) {
                        return true; // Found a valid SA ID
                    }
                }

                return false; // No valid ID found
            }

        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Error reading ID document: " + e.getMessage());
        }
    }
    public static boolean isValidSouthAfricanIdPdf(String base64Pdf) {
        try {
            // Extract the Base64 content if it contains a data URI prefix
            String base64Content = base64Pdf.contains(",") ? base64Pdf.split(",")[1] : base64Pdf;

            // Decode the Base64 PDF
            byte[] pdfBytes = Base64.getDecoder().decode(base64Content);

            // Load PDF and extract text
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
                String text = new PDFTextStripper().getText(document);

                // Clean text: remove anything except digits and spaces
                String cleanedText = text.replaceAll("[^0-9]", " ");

                // Match 13-digit numbers (possible SA ID numbers)
                Pattern idPattern = Pattern.compile("\\b\\d{13}\\b");
                Matcher matcher = idPattern.matcher(cleanedText);

                while (matcher.find()) {
                    String potentialId = matcher.group();
                    if (isValidSAIdNumber(potentialId)) {
                        return true; // Found a valid SA ID number
                    }
                }
                return false; // No valid ID found
            }
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid PDF or Base64 content: " + e.getMessage());
        }
    }

    // =========================================================
    // 3. SA ID NUMBER VALIDATION
    // =========================================================

    public static boolean isValidSAIdNumber(String idNumber) {
        if (idNumber == null || !idNumber.matches("\\d{13}")) return false;

        // Extract date of birth and check validity
        String yy = idNumber.substring(0, 2);
        String mm = idNumber.substring(2, 4);
        String dd = idNumber.substring(4, 6);

        try {
            int year = Integer.parseInt(yy);
            int month = Integer.parseInt(mm);
            int day = Integer.parseInt(dd);

            if (month < 1 || month > 12) return false;
            if (day < 1 || day > 31) return false; // Could be improved with real month/day validation

            // Optional: add Luhn check (SA ID numbers use it)
            return luhnCheck(idNumber);
        } catch (NumberFormatException e) {
            return false;
        }
    }



    // =========================================================
    // 4. AGE VALIDATION (VERY IMPORTANT FOR LOANS)
    // =========================================================
    public static boolean isAboveMinimumAge(String saId, int minAge) {
        try {
            String dob = saId.substring(0, 6);
            LocalDate birthDate = LocalDate.parse(dob, DateTimeFormatter.ofPattern("yyMMdd"));

            int age = Period.between(birthDate, LocalDate.now()).getYears();
            return age >= minAge;

        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    // 5. PHONE NUMBER VALIDATION (SA)
    // =========================================================
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^(\\+27|0)[6-8][0-9]{8}$");
    }

    // =========================================================
    // 6. EMAIL VALIDATION
    // =========================================================
    public static boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    // =========================================================
    // 7. BANK STATEMENT VALIDATION (BASIC)
    // =========================================================
    public static boolean isValidBankStatement(String pdfText) {
        String lower = pdfText.toLowerCase();

        return lower.contains("account")
                && lower.contains("balance")
                && lower.contains("transaction");
    }

    // =========================================================
    // 8. SALARY / AFFORDABILITY CHECK
    // =========================================================
    public static boolean isAffordable(BigDecimal income, BigDecimal loanInstallment) {

        if (income == null || loanInstallment == null) return false;

        // Rule: installment must not exceed 30% of income
        BigDecimal threshold = income.multiply(BigDecimal.valueOf(0.3));

        return loanInstallment.compareTo(threshold) <= 0;
    }

    // =========================================================
    // 9. LOAN AMOUNT LIMIT VALIDATION
    // =========================================================
    public static boolean isValidLoanAmount(BigDecimal amount) {
        if (amount == null) return false;

        BigDecimal min = BigDecimal.valueOf(500);
        BigDecimal max = BigDecimal.valueOf(50000);

        return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
    }

    // =========================================================
    // 10. DATE VALIDATION (USED INTERNALLY)
    // =========================================================
    private static void validateTodayDate(String text) {
        Pattern datePattern = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b");
        Matcher matcher = datePattern.matcher(text);

        LocalDate today = LocalDate.now();

        while (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(
                        matcher.group(1),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );

                if (date.equals(today)) return;

            } catch (Exception ignored) {}
        }

        throw new ResponseStatusException(BAD_REQUEST, "Date not valid (must be today)");
    }

    // =========================================================
    // 11. LUHN CHECK (REUSABLE)
    // =========================================================
    private static boolean luhnCheck(String number) {

        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }

            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }
    public static String extractPdfText(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException exception) {
            return "";
        }
    }
    public static String normalize(String value) {
        if (value == null) return "";

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }
    public static String matchField(String text, String value) {
        return normalize(text).contains(normalize(value)) ? value : null;
    }
    public static long averageHash(BufferedImage image) {
        long total = 0;
        int[] pixels = new int[32 * 32];
        image.getRaster().getPixels(0, 0, 32, 32, pixels);

        for (int pixel : pixels) total += pixel;

        long avg = total / pixels.length;
        long hash = 0L;

        for (int i = 0; i < 64; i++) {
            if (pixels[i] >= avg) hash |= (1L << i);
        }

        return hash;
    }
    public static BufferedImage cropCenter(BufferedImage source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        BufferedImage cropped = source.getSubimage(x, y, size, size);
        Image scaled = cropped.getScaledInstance(32, 32, Image.SCALE_SMOOTH);

        BufferedImage gray = new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();

        return new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(gray, null);
    }
    public static BigDecimal calculateImageSimilarity(BufferedImage img1, BufferedImage img2) {
        long hash1 = averageHash(cropCenter(img1));
        long hash2 = averageHash(cropCenter(img2));

        int distance = Long.bitCount(hash1 ^ hash2);
        double similarity = ((64.0 - distance) / 64.0) * 100.0;

        return BigDecimal.valueOf(Math.max(0.0, similarity))
                .setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal compareSelfieWithId(Path pdfPath, Path selfiePath) {
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage idImage = renderer.renderImageWithDPI(0, 140);
            BufferedImage selfieImage = javax.imageio.ImageIO.read(selfiePath.toFile());
            if (selfieImage == null) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            long firstHash = ValidationUtil.averageHash(ValidationUtil.cropCenter(idImage));
            long secondHash = ValidationUtil.averageHash(ValidationUtil.cropCenter(selfieImage));
            int distance = Long.bitCount(firstHash ^ secondHash);
            double similarity = ((64.0 - distance) / 64.0) * 100.0;
            return BigDecimal.valueOf(Math.max(0.0, similarity)).setScale(2, RoundingMode.HALF_UP);
        } catch (IOException exception) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }
}