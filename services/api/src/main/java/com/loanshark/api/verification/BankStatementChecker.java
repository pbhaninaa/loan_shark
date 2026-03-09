package com.loanshark.api.verification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Checks bank statement data for consistency and for possible misrepresentation
 * (e.g. stated income vs parsed income, balance math, date order).
 * <p>
 * Not wired into any controller or service. Use when you are ready to integrate
 * bank statement verification (e.g. after parsing PDF/CSV into {@link BankStatementChecker.VerificationInput}).
 */
public final class BankStatementChecker {

    private static final BigDecimal INCOME_TOLERANCE_PERCENT = new BigDecimal("10");
    private static final int RECURRING_MONTHS_MIN = 2;
    private static final Pattern SALARY_LIKE = Pattern.compile(
        "salary|sal|wage|payroll|employer|deposit|credit", Pattern.CASE_INSENSITIVE);

    private BankStatementChecker() {
    }

    /**
     * Input for verification. Populate from your statement parser (PDF/CSV).
     */
    public record VerificationInput(
        /** Monthly income the borrower stated on the application. */
        BigDecimal statedMonthlyIncome,
        /** Account holder name as on the statement (optional). */
        String accountHolderName,
        /** Statement period start. */
        LocalDate periodStart,
        /** Statement period end. */
        LocalDate periodEnd,
        /** Opening balance at period start. */
        BigDecimal openingBalance,
        /** Closing balance at period end. */
        BigDecimal closingBalance,
        /** All transactions in the period, in date order. */
        List<TransactionLine> transactions
    ) {
        public VerificationInput {
            transactions = transactions != null ? List.copyOf(transactions) : List.of();
        }
    }

    public record TransactionLine(
        LocalDate date,
        BigDecimal credit,
        BigDecimal debit,
        String description,
        BigDecimal runningBalance
    ) {
        public BigDecimal amount() {
            if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
                return credit;
            }
            if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
                return debit.negate();
            }
            return BigDecimal.ZERO;
        }

        public boolean isCredit() {
            return credit != null && credit.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * Result of running all checks. Use {@link #isPassed()} and {@link #getFindings()} to decide.
     */
    public record VerificationResult(
        boolean passed,
        List<String> findings
    ) {
        public static VerificationResult pass() {
            return new VerificationResult(true, List.of());
        }

        public static VerificationResult fail(List<String> findings) {
            return new VerificationResult(false, List.copyOf(findings));
        }
    }

    /**
     * Runs all verification checks. Does not throw; returns a result with findings.
     */
    public static VerificationResult verify(VerificationInput input) {
        if (input == null) {
            return VerificationResult.fail(List.of("No statement data provided."));
        }
        List<String> findings = new ArrayList<>();

        checkBalanceMath(input, findings);
        checkDateOrder(input, findings);
        checkTotalsConsistency(input, findings);
        checkStatedIncomeVsParsed(input, findings);
        checkRecurringIncome(input, findings);
        checkFutureDates(input, findings);

        return findings.isEmpty()
            ? VerificationResult.pass()
            : VerificationResult.fail(findings);
    }

    private static void checkBalanceMath(VerificationInput input, List<String> findings) {
        BigDecimal opening = nullToZero(input.openingBalance());
        BigDecimal totalCredits = input.transactions().stream()
            .filter(TransactionLine::isCredit)
            .map(t -> nullToZero(t.credit()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebits = input.transactions().stream()
            .filter(t -> t.debit() != null && t.debit().compareTo(BigDecimal.ZERO) > 0)
            .map(TransactionLine::debit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closing = nullToZero(input.closingBalance());

        BigDecimal expectedClosing = opening.add(totalCredits).subtract(totalDebits);
        if (closing.compareTo(expectedClosing) != 0) {
            findings.add(String.format(Locale.US,
                "Balance math mismatch: opening + credits - debits = %s but closing = %s. Possible tampering or parsing error.",
                expectedClosing.setScale(2, RoundingMode.HALF_UP), closing.setScale(2, RoundingMode.HALF_UP)));
        }
    }

    private static void checkDateOrder(VerificationInput input, List<String> findings) {
        LocalDate prev = null;
        for (TransactionLine t : input.transactions()) {
            if (t.date() == null) continue;
            if (prev != null && t.date().isBefore(prev)) {
                findings.add("Transactions are not in date order. Possible reordering or tampering.");
                break;
            }
            prev = t.date();
        }
    }

    private static void checkTotalsConsistency(VerificationInput input, List<String> findings) {
        BigDecimal sumCredits = input.transactions().stream()
            .filter(TransactionLine::isCredit)
            .map(t -> nullToZero(t.credit()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumDebits = input.transactions().stream()
            .filter(t -> t.debit() != null && t.debit().compareTo(BigDecimal.ZERO) > 0)
            .map(TransactionLine::debit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumCredits.compareTo(BigDecimal.ZERO) == 0 && sumDebits.compareTo(BigDecimal.ZERO) == 0 && !input.transactions().isEmpty()) {
            findings.add("Statement has transactions but total credits/debits are zero. Check parsing.");
        }
    }

    private static void checkStatedIncomeVsParsed(VerificationInput input, List<String> findings) {
        BigDecimal stated = input.statedMonthlyIncome();
        if (stated == null || stated.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal avgCredit = averageMonthlyCredit(input);
        if (avgCredit == null) return;
        BigDecimal diffPercent = stated.subtract(avgCredit).abs()
            .multiply(BigDecimal.valueOf(100))
            .divide(stated, 2, RoundingMode.HALF_UP);
        if (diffPercent.compareTo(INCOME_TOLERANCE_PERCENT) > 0) {
            findings.add(String.format(Locale.US,
                "Stated monthly income (R%s) differs from average monthly credits in statement (R%s) by more than %s%%. Verify income.",
                stated.setScale(2, RoundingMode.HALF_UP), avgCredit.setScale(2, RoundingMode.HALF_UP), INCOME_TOLERANCE_PERCENT));
        }
    }

    private static void checkRecurringIncome(VerificationInput input, List<String> findings) {
        BigDecimal stated = input.statedMonthlyIncome();
        if (stated == null || stated.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<BigDecimal> salaryLikeCredits = input.transactions().stream()
            .filter(TransactionLine::isCredit)
            .filter(t -> t.description() != null && SALARY_LIKE.matcher(t.description()).find())
            .map(t -> nullToZero(t.credit()))
            .toList();
        long closeToStated = salaryLikeCredits.stream()
            .filter(amount -> amount.compareTo(stated.multiply(new BigDecimal("0.9"))) >= 0
                && amount.compareTo(stated.multiply(new BigDecimal("1.1"))) <= 0)
            .count();
        if (salaryLikeCredits.size() >= RECURRING_MONTHS_MIN && closeToStated < RECURRING_MONTHS_MIN) {
            findings.add(String.format(Locale.US,
                "No recurring deposit close to stated income (R%s) found for at least %d months. Income may be misstated.",
                stated.setScale(2, RoundingMode.HALF_UP), RECURRING_MONTHS_MIN));
        }
    }

    private static void checkFutureDates(VerificationInput input, List<String> findings) {
        LocalDate end = input.periodEnd();
        if (end == null) return;
        LocalDate today = LocalDate.now();
        for (TransactionLine t : input.transactions()) {
            if (t.date() != null && t.date().isAfter(today)) {
                findings.add("Statement contains future dates. Possible tampering.");
                break;
            }
        }
    }

    private static BigDecimal averageMonthlyCredit(VerificationInput input) {
        BigDecimal total = input.transactions().stream()
            .filter(TransactionLine::isCredit)
            .map(t -> nullToZero(t.credit()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) return null;
        LocalDate start = input.periodStart();
        LocalDate end = input.periodEnd();
        if (start == null || end == null || !end.isAfter(start)) return total;
        long months = Math.max(1, java.time.temporal.ChronoUnit.MONTHS.between(start, end) + 1);
        return total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
