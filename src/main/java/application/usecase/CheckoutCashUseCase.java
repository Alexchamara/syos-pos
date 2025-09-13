package main.java.application.usecase;

import main.java.domain.billing.Bill;
import main.java.domain.billing.BillLine;
import main.java.application.services.BillNumberService;
import main.java.domain.pricing.DiscountPolicy;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.repository.BillRepository;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Money;
import main.java.domain.shared.Quantity;
import main.java.infrastructure.concurrency.Tx;

import java.util.ArrayList;
import java.util.List;

public final class CheckoutCashUseCase {
    private final Tx tx;
    private final ProductRepository products;
    private final BillRepository bills;
    private final BatchSelectionStrategy strategy;
    private final BillNumberService billNumbers;

    public CheckoutCashUseCase(Tx tx, ProductRepository products, BillRepository bills, BatchSelectionStrategy strategy, BillNumberService billNumbers) {
        this.tx = tx; this.products = products; this.bills = bills; this.strategy = strategy;
        this.billNumbers = billNumbers;
    }

    /** cart: list of (productCode, qty). Cash in cents. */
    public Bill handle(List<Item> cart, long cashCents, StockLocation location,  DiscountPolicy discountPolicy, String scope) {
        return tx.inTx(con -> {
            // 1) Build bill lines from product master
            String serial = billNumbers.next("COUNTER");
            var builder = new Bill.Builder().serial(serial);
            List<BillLine> lines = new ArrayList<>();

            for (Item it : cart) {
                var prod = products.findByCode(new Code(it.code())).orElseThrow(() -> new IllegalArgumentException("Unknown product: " + it.code()));
                var line = new BillLine(prod.code(), prod.name(), new Quantity(it.qty()), prod.price());
                builder.addLine(line);
                lines.add(line);
            }

            // 2) Apply discount from the chosen policy
            var discount = discountPolicy.discountFor(lines);

            // 3) Set discount & cash; build validates Cash >= Total
            builder.discount(discount).cash(Money.of(cashCents));
            Bill bill = builder.build();

            // 4) Persist bill
            bills.save(con, bill);

            // 5) Deduct inventory per line using strategy (FIFO/FEFO)
            for (var l : bill.lines()) {
                strategy.deduct(con, l.productCode(), l.qty().value(), location);
            }

            return bill; // Return the complete bill with all line items
        });
    }

    public record Item(String code, int qty) {}
}