package theater;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * This class generates a statement for a given invoice of performances.
 */
public class StatementPrinter {
    private Invoice invoice;
    private Map<String, Play> plays;

    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    /**
     * Returns a formatted statement of the invoice associated with this printer.
     * @return the formatted statement
     */
    public String statement() {
        int totalAmount = 0;
        int volumeCredits = 0;

        final StringBuilder result =
                new StringBuilder("Statement for " + invoice.getCustomer() + System.lineSeparator());

        final NumberFormat frmt = NumberFormat.getCurrencyInstance(Locale.US);

        // minimal magic-number fixes
        final int centsToDollars = 100;
        final int tragedyBase = 40000;
        final int tragedyExtraPerSeat = 1000;
        final int tragedyThreshold = 30;

        for (Performance performance : invoice.getPerformances()) {

            final int thisAmount =
                    getAmount(performance, tragedyBase, tragedyThreshold, tragedyExtraPerSeat);

            // volume credits now extracted:
            volumeCredits += getVolumeCredits(performance);

            // print line for this order
            result.append(String.format(
                    "  %s: %s (%s seats)%n",
                    getPlay(performance).getName(),
                    frmt.format(thisAmount / centsToDollars),
                    performance.getAudience()));

            totalAmount += thisAmount;
        }

        result.append(String.format(
                "Amount owed is %s%n",
                frmt.format(totalAmount / centsToDollars)));

        result.append(String.format(
                "You earned %s credits%n",
                volumeCredits));

        return result.toString();
    }

    private Play getPlay(Performance performance) {
        return plays.get(performance.getPlayID());
    }

    /**
     * Calculates the amount owed for a given performance.
     *
     * @param performance the performance being evaluated
     * @param tragedyBase the base amount for tragedy plays
     * @param tragedyThreshold the audience threshold for extra tragedy charges
     * @param tragedyExtraPerSeat the amount added per additional tragedy audience member
     * @return the calculated amount
     * @throws RuntimeException if the play type is unknown
     */
    private int getAmount(
            Performance performance,
            int tragedyBase,
            int tragedyThreshold,
            int tragedyExtraPerSeat) {

        int result = 0;

        switch (getPlay(performance).getType()) {
            case "tragedy":
                result = tragedyBase;
                if (performance.getAudience() > tragedyThreshold) {
                    result += tragedyExtraPerSeat * (performance.getAudience() - tragedyThreshold);
                }
                break;

            case "comedy":
                result = Constants.COMEDY_BASE_AMOUNT;
                if (performance.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                    result += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                            + (Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                            * (performance.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD));
                }
                result += Constants.COMEDY_AMOUNT_PER_AUDIENCE * performance.getAudience();
                break;

            default:
                throw new RuntimeException(
                        String.format("unknown type: %s", getPlay(performance).getType()));
        }

        return result;
    }

    /**
     * Calculates the volume credits earned for a given performance.
     *
     * @param performance the performance being evaluated
     * @return the number of volume credits earned
     */
    private int getVolumeCredits(Performance performance) {

        int result = 0;

        // base credit
        result += Math.max(
                performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD,
                0);

        // bonus for comedy
        if ("comedy".equals(getPlay(performance).getType())) {
            result += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }

        return result;
    }
}
