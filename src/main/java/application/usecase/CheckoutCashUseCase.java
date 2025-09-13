package main.java.application.usecase;

import main.java.domain.billing.Bill;
import main.java.domain.billing.BillLine;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.repository.BillRepository;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Money;
import main.java.domain.shared.Quantity;
import main.java.infrastructure.concurrency.Tx;

import java.util.List;

public final class CheckoutCashUseCase {
    private final Tx tx;
    private final ProductRepository products;
    private final BillRepository bills;
    private final BatchSelectionStrategy strategy;

    public CheckoutCashUseCase(Tx tx, ProductRepository products, BillRepository bills, BatchSelectionStrategy strategy) {
        this.tx = tx; this.products = products; this.bills = bills; this.strategy = strategy;
    }

    /** cart: list of (productCode, qty). Cash in cents. */
    public Bill handle(List<Item> cart, long cashCents, StockLocation location) {
        return tx.inTx(con -> {
            // 1) Build bill lines from product master
            var builder = new Bill.Builder().serial("BILL-" + System.currentTimeMillis());
            for (Item it : cart) {
                var prod = products.findByCode(new Code(it.code())).orElseThrow(() -> new IllegalArgumentException("Unknown product: " + it.code()));
                var line = new BillLine(prod.code(), prod.name(), new Quantity(it.qty()), prod.price());
                builder.addLine(line);
            }
            builder.discount(Money.of(0)); // placeholder for future discount rules
            builder.cash(Money.of(cashCents));
            Bill bill = builder.build();

            // 2) Persist bill
            long id = bills.save(con, bill);

            // 3) Deduct inventory per line using strategy (FIFO/FEFO)
            for (var l : bill.lines()) {
                strategy.deduct(con, l.productCode(), l.qty().value(), location);
            }

            return bill; // Return the complete bill with all line items
        });
    }

    public record Item(String code, int qty) {}
}