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
import main.java.domain.events.EventPublisher;
import main.java.domain.events.LowStockEvent;
import main.java.domain.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.List;

public final class CheckoutCashUseCase {
    private final Tx tx;
    private final ProductRepository products;
    private final BillRepository bills;
    private final BatchSelectionStrategy strategy;
    private final BillNumberService billNumbers;
    private final InventoryRepository inventory;
    private final EventPublisher events;
    private final int lowStockThreshold = 50;

    public CheckoutCashUseCase(Tx tx, ProductRepository products, BillRepository bills, BatchSelectionStrategy strategy, BillNumberService billNumbers, InventoryRepository inventory, EventPublisher events) {
        this.tx = tx; this.products = products; this.bills = bills; this.strategy = strategy;
        this.billNumbers = billNumbers; this.inventory = inventory; this.events = events;
    }

    /** cart: list of (productCode, qty). Cash in cents. Hybrid fulfillment: when location==SHELF and insufficient stock, picks remainder from WEB. */
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

            // 5) Deduct inventory per line.
            for (var l : bill.lines()) {
                Code code = l.productCode();
                int qty = l.qty().value();
                if (location == StockLocation.SHELF) {
                    int remain = inventory.remainingQuantity(con, l.productCode().value(), location.name());
                    if (remain < lowStockThreshold) {
                        events.publish(new LowStockEvent(l.productCode(), remain));
                    }
                }
                if (location == StockLocation.SHELF) {
                    // Try to deduct as much as possible from SHELF
                    int takenShelf = strategy.deductUpTo(con, code, qty, StockLocation.SHELF);
                    int remaining = qty - takenShelf;
                    if (remaining > 0) {
                        // Deduct remainder from WEB (throws if not enough there)
                        strategy.deduct(con, code, remaining, StockLocation.WEB);
                    }
                } else {
                    // Pure single-location deduction (WEB or others in future)
                    strategy.deduct(con, code, qty, location);
                }
            }

            return bill; // Return the complete bill with all line items
        });
    }

    public record Item(String code, int qty) {}
}