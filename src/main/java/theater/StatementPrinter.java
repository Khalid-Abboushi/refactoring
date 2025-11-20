package theater;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Generates a statement for a given invoice consisting of multiple performances.
 */
public class StatementPrinter {

    private static final int CENTS_TO_DOLLARS = 100;
    private static final int TRAGEDY_BASE = 40000;
    private static final int TRAGEDY_EXTRA_PER_SEAT = 1000;
    private static final int TRAGEDY_THRESHOLD = 30;

    private final Invoice invoice;
    private final Map<String, Play> plays;

    /**
     * Constructs a StatementPrinter.
     *
     * @param invoice the invoice containing performances
     * @param plays   the map of playID to Play objects
     */
    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    /**
     * Builds the formatted statement string for the invoice.
     *
     * @return the formatted statement
     */
    public String statement() {

        final StringBuilder result = new StringBuilder(
                "Statement for " + invoice.getCustomer() + System.lineSeparator());

        final int volumeCredits = getTotalVolumeCredits();
        final int totalAmount = getTotalAmount();

        appendPerformanceLines(result);

        result.append(String.format("Amount owed is %s%n", usd(totalAmount)));
        result.append(String.format("You earned %s credits%n", volumeCredits));

        return result.toString();
    }

    /**
     * Appends each performance's formatted line to the result builder.
     *
     * @param result the StringBuilder to append to
     */
    private void appendPerformanceLines(StringBuilder result) {
        for (Performance performance : invoice.getPerformances()) {
            final int amount = getAmount(performance);

            result.append(
                    String.format(
                            "  %s: %s (%s seats)%n",
                            getPlay(performance).getName(),
                            usd(amount),
                            performance.getAudience()
                    )
            );
        }
    }

    /**
     * Retrieves the Play associated with the given performance.
     *
     * @param performance the performance
     * @return the matching Play
     */
    private Play getPlay(Performance performance) {
        return plays.get(performance.getPlayID());
    }

    /**
     * Computes the cost for a single performance.
     *
     * @param performance the performance being evaluated
     * @return the cost of the performance in cents
     * @throws RuntimeException if the play type is not recognized
     */
    private int getAmount(Performance performance) {

        int amount = 0;
        final String type = getPlay(performance).getType();
        final int audience = performance.getAudience();

        switch (type) {
            case "tragedy":
                amount = TRAGEDY_BASE;
                if (audience > TRAGEDY_THRESHOLD) {
                    amount += TRAGEDY_EXTRA_PER_SEAT * (audience - TRAGEDY_THRESHOLD);
                }
                break;

            case "comedy":
                amount = Constants.COMEDY_BASE_AMOUNT;

                if (audience > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                    amount += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                            + (Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (audience - Constants.COMEDY_AUDIENCE_THRESHOLD));
                }
                amount += Constants.COMEDY_AMOUNT_PER_AUDIENCE * audience;
                break;

            default:
                throw new RuntimeException(
                        String.format("unknown type: %s", type));
        }

        return amount;
    }

    /**
     * Computes the total volume credits across all performances.
     *
     * @return total volume credits
     */
    private int getTotalVolumeCredits() {
        int result = 0;

        for (Performance performance : invoice.getPerformances()) {
            result += getVolumeCredits(performance);
        }

        return result;
    }

    /**
     * Computes the volume credits for a single performance.
     *
     * @param performance the performance being evaluated
     * @return the number of volume credits earned
     */
    private int getVolumeCredits(Performance performance) {

        int credits = Math.max(
                performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD,
                0
        );

        if ("comedy".equals(getPlay(performance).getType())) {
            credits += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }

        return credits;
    }

    /**
     * Computes the total amount across all performances.
     *
     * @return the total amount in cents
     */
    private int getTotalAmount() {
        int result = 0;

        for (Performance performance : invoice.getPerformances()) {
            result += getAmount(performance);
        }

        return result;
    }

    /**
     * Formats an amount (in cents) into U.S. currency.
     *
     * @param amountInCents the amount in cents
     * @return USD formatted currency string
     */
    private String usd(int amountInCents) {
        final NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format((double) amountInCents / CENTS_TO_DOLLARS);
    }
}
