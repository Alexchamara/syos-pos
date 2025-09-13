package main.java.cli.cashier.checkout;

import main.java.application.usecase.CheckoutCashUseCase;
import main.java.application.usecase.CheckoutCashUseCase.Item;
import main.java.application.usecase.QuoteUseCase;
import main.java.cli.bill.BillPrinter;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.pricing.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class CliCheckout {
    private final CheckoutCashUseCase checkout;
    private final BatchSelectionStrategy strategyDefault;
    private final QuoteUseCase quote;

    public CliCheckout(CheckoutCashUseCase checkout,
                       BatchSelectionStrategy strategyDefault,
                       QuoteUseCase quote) {
        this.checkout = checkout;
        this.strategyDefault = strategyDefault;
        this.quote = quote;
    }

    public void run() {
        var sc = new Scanner(System.in);
        System.out.println("=== SYOS Checkout (CLI) ===");
        System.out.println("Type product code and qty. Type 'done' to finish.");

        List<Item> cart = new ArrayList<>();
        while (true) {
            System.out.print("Item code (or 'done'): ");
            String code = sc.next().trim();
            if (code.equalsIgnoreCase("done")) break;
            System.out.print("Qty: ");
            int qty = sc.nextInt();
            cart.add(new Item(code, qty));
        }
        if (cart.isEmpty()) {
            System.out.println("Cart empty, aborting.");
            return;
        }

        // location
        StockLocation loc = StockLocation.SHELF;
        System.out.print("Location [SHELF/WEB] (default SHELF): ");
        String locIn = sc.next();
        if (locIn != null && !locIn.isBlank()) {
            try { loc = StockLocation.valueOf(locIn.toUpperCase()); } catch (Exception ignored) {}
        }

        // discount selection (policy)
        DiscountPolicy policy = new NoDiscount();
        System.out.print("Apply discount? [y/N]: ");
        String d = sc.next();
        if ("y".equalsIgnoreCase(d)) {
            System.out.print("Percent (0..100): ");
            int pct = Math.max(0, Math.min(100, sc.nextInt()));
            policy = new PercentDiscount(pct);
        }

        // PREVIEW (pre-bill)
        var q = quote.preview(cart, policy);
        BillPrinter.printPreview(q);

        // CASH LOOP
        long cash = 0;
        while (true) {
            System.out.print("Cash (cents, e.g., 10000 = Rs.100.00): ");
            cash = sc.nextLong();
            if (cash < q.total().amount().movePointRight(2).longValueExact()) {
                System.out.println("Insufficient cash. Need at least " + q.total().amount().toPlainString());
                continue;
            }
            break;
        }

        // Confirm
        System.out.print("Confirm checkout? [y/N]: ");
        if (!"y".equalsIgnoreCase(sc.next())) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            var bill = checkout.handle(cart, cash, loc, policy, "COUNTER"); // scope fixed to COUNTER here
            BillPrinter.print(bill);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
    }
}