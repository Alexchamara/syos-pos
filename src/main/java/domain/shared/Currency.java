package main.java.domain.shared;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Currency {

    public static final String SYMBOL = "Rs.";
    public static final String CODE = "LKR";
    public static final Locale LOCALE = new Locale("si", "LK");

    private static final NumberFormat FORMATTER = NumberFormat.getCurrencyInstance(LOCALE);

    static {
        FORMATTER.setCurrency(java.util.Currency.getInstance(CODE));
    }

    public static String format(BigDecimal amount) {
        return FORMATTER.format(amount);
    }

    public static String formatSimple(BigDecimal amount) {
        return SYMBOL + " " + amount.toPlainString();
    }

    public static String formatSimple(Money money) {
        return formatSimple(money.amount());
    }

    private Currency() {} // Utility class
}
