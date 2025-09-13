package main.java.application.usecase;


import main.java.domain.billing.BillLine;
import main.java.domain.pricing.DiscountPolicy;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Money;
import main.java.domain.shared.Quantity;
import main.java.domain.product.Product;

import java.util.ArrayList;
import java.util.List;

public class QuoteUseCase {
    public record Quote(List<BillLine> lines, Money subtotal, Money discount, Money total) {}

    private final ProductRepository products;

    public QuoteUseCase(ProductRepository products) { this.products = products; }

    public Quote preview(List<CheckoutCashUseCase.Item> cart, DiscountPolicy discountPolicy) {
        List<BillLine> lines = new ArrayList<>();
        for (var it : cart) {
            Product p = products.findByCode(new Code(it.code()))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + it.code()));
            lines.add(new BillLine(p.code(), p.name(), new Quantity(it.qty()), p.price()));
        }
        var subtotal = lines.stream().map(BillLine::lineTotal).reduce(Money.of(0), Money::plus);
        var discount = discountPolicy.discountFor(lines);
        var total = subtotal.minus(discount);
        if (total.amount().signum() < 0) throw new IllegalArgumentException("Discount > subtotal");
        return new Quote(lines, subtotal, discount, total);
    }
}
