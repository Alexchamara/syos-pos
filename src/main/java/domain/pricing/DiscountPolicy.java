package main.java.domain.pricing;

import main.java.domain.billing.BillLine;
import main.java.domain.shared.Money;

import java.util.List;

/**
 * Strategy interface for calculating discounts on bill lines.
 */
public interface DiscountPolicy {
    /**
     * Return how much to subtract from subtotal for these lines.
     * @param lines the bill lines to calculate discount for
     * @return the discount amount
     */
    Money discountFor(List<BillLine> lines);
}
