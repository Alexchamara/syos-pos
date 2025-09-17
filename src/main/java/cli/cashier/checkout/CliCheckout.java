package main.java.cli.cashier.checkout;

import main.java.application.services.AvailabilityService;
import main.java.application.services.ShortageNotifier;
import main.java.application.usecase.CheckoutCashUseCase;
import main.java.application.usecase.CheckoutCashUseCase.Item;
import main.java.application.usecase.QuoteUseCase;
import main.java.cli.bill.BillPrinter;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.pricing.DiscountPolicy;
import main.java.domain.pricing.NoDiscount;
import main.java.domain.pricing.PercentDiscount;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class CliCheckout {
    private final CheckoutCashUseCase checkout;
    private final BatchSelectionStrategy strategyDefault;
    private final QuoteUseCase quote;
    private final AvailabilityService availability;

    public CliCheckout(CheckoutCashUseCase checkout,
                       BatchSelectionStrategy strategyDefault,
                       QuoteUseCase quote,
                       AvailabilityService availability) {
        this.checkout = checkout;
        this.strategyDefault = strategyDefault;
        this.quote = quote;
        this.availability = availability;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SYOS Checkout (CLI) ===");
        System.out.println("Type product code then quantity. Type 'done' to finish.");

        List<Item> cart = new ArrayList<>();

        while (true) {
            System.out.print("Item code (or 'done'): ");
            String code = sc.next().trim();
            if (code.equalsIgnoreCase("done")) break;

            if (!quote.productExists(code)) {
                System.out.println("Invalid code: " + code);
                continue;
            }

            System.out.print("Qty: ");
            int qty = sc.nextInt();
            if (qty <= 0) {
                System.out.println("Qty must be > 0");
                continue;
            }

            // Check availability and handle restocking for this item
            int finalQuantity = handleItemRestocking(sc, code, qty);
            if (finalQuantity > 0) {
                cart.add(new Item(code, finalQuantity));
                System.out.println("✓ Added " + finalQuantity + " x " + code + " to cart");
            } else {
                System.out.println("✗ Item not added to cart");
            }
        }

        if (cart.isEmpty()) {
            System.out.println("Cart empty, aborting.");
            return;
        }

        // Continue with normal checkout process
        continueNormalCheckout(sc, cart);
    }

    /**
     * Handles the restocking logic for a single item as per requirements:
     * 1. Default deduct from SHELF
     * 2. If not enough on SHELF, check WEB availability
     * 3. If available on WEB, ask cashier to transfer
     * 4. If neither SHELF nor WEB has stock, notify manager
     *
     * @return The final quantity to add to cart (0 if item should not be added)
     */
    private int handleItemRestocking(Scanner sc, String code, int requestedQty) {
        int shelfStock = availability.available(code, StockLocation.SHELF);

        // DEBUG: Always show stock levels for debugging
        System.out.println("DEBUG: Checking stock for " + code);
        System.out.println("DEBUG: SHELF stock: " + shelfStock + ", Requested: " + requestedQty);

        // Check if SHELF has enough stock
        if (shelfStock >= requestedQty) {
            System.out.println("DEBUG: Sufficient stock on SHELF, proceeding normally");
            return requestedQty; // Sufficient stock on SHELF, proceed normally
        }

        System.out.println("⚠ Not enough stock on SHELF for " + code);
        System.out.println("  SHELF stock: " + shelfStock + ", Required: " + requestedQty);

        // Check WEB stock availability
        int webStock = availability.available(code, StockLocation.WEB);
        System.out.println("  WEB stock: " + webStock);
        System.out.println("DEBUG: Total available: " + (shelfStock + webStock));

        if (webStock == 0) {
            // No stock in WEB either
            System.out.println("No stock available in WEB either!");
            String msg = String.format("URGENT: Product %s out of stock (SHELF: %d, WEB: %d, Required: %d)",
                code, shelfStock, webStock, requestedQty);
            System.out.println(msg);
            try {
                ShortageNotifier.record(msg);
                System.out.println("Manager notified with high priority");
            } catch (Throwable e) {
                System.out.println("Failed to notify manager: " + e.getMessage());
            }

            // Offer to use only available SHELF quantity if any
            if (shelfStock > 0) {
                System.out.print("Use only available SHELF quantity (" + shelfStock + ")? [y/N]: ");
                if ("y".equalsIgnoreCase(sc.next())) {
                    return shelfStock;
                }
            }
            return 0; // Cannot add to cart
        }

        // Calculate how much we need from WEB
        int neededFromWeb = requestedQty - shelfStock;
        System.out.println("DEBUG: Need " + neededFromWeb + " from WEB");

        if (webStock < neededFromWeb) {
            // Not enough in WEB to fulfill the request
            System.out.println("Insufficient total stock!");
            System.out.println("  Available total: " + (shelfStock + webStock) + ", Required: " + requestedQty);
            String msg = String.format("Product %s insufficient stock (SHELF: %d + WEB: %d = %d, Required: %d)",
                code, shelfStock, webStock, shelfStock + webStock, requestedQty);
            System.out.println(msg);
            try {
                ShortageNotifier.record(msg);
                System.out.println("Manager notified");
            } catch (Throwable e) {
                System.out.println("Failed to notify manager: " + e.getMessage());
            }

            // Offer to use only available quantity
            if (shelfStock > 0) {
                System.out.print("Use only available SHELF quantity (" + shelfStock + ")? [y/N]: ");
                if ("y".equalsIgnoreCase(sc.next())) {
                    return shelfStock;
                }
            }
            return 0;
        }

        // WEB has enough stock, ask cashier to transfer
        System.out.println("WEB has sufficient stock (" + webStock + " available)");
        System.out.println("Need to transfer " + neededFromWeb + " units from WEB to SHELF");
        System.out.print("Transfer " + neededFromWeb + " units from WEB to SHELF? [y/N]: ");

        if ("y".equalsIgnoreCase(sc.next())) {
            try {
                System.out.println("Transferring " + neededFromWeb + " units from WEB to SHELF...");
                availability.transferFromWebToShelf(code, neededFromWeb);
                System.out.println("Transfer completed successfully!");
                return requestedQty;
            } catch (Exception e) {
                System.out.println("Transfer failed: " + e.getMessage());
                return 0;
            }
        } else {
            // Cashier declined transfer, offer alternatives
            System.out.println("Transfer declined.");
            if (shelfStock > 0) {
                System.out.print("Use only available SHELF quantity (" + shelfStock + ")? [y/N]: ");
                if ("y".equalsIgnoreCase(sc.next())) {
                    return shelfStock;
                }
            }
            return 0; // Item will not be added to cart
        }
    }

    private void continueNormalCheckout(Scanner sc, List<Item> cart) {
        // Default sell from SHELF (since we've handled restocking above)
        StockLocation loc = StockLocation.SHELF;

        // Discount policy
        DiscountPolicy policy = new NoDiscount();
        System.out.print("Apply discount? [y/N]: ");
        if ("y".equalsIgnoreCase(sc.next())) {
            System.out.print("Percent (0..100): ");
            int pct = Math.max(0, Math.min(100, sc.nextInt()));
            policy = new PercentDiscount(pct);
        }

        // PREVIEW (pre-bill)
        var q = quote.preview(cart, policy);
        BillPrinter.printPreview(q);

        // CASH LOOP (in cents)
        long needCents = q.total().amount().movePointRight(2).longValueExact();
        long cash;
        while (true) {
            System.out.print("Cash (cents, e.g., 10000 = Rs.100.00): ");
            cash = sc.nextLong();
            if (cash < needCents) {
                System.out.println("Insufficient cash. Need at least " + q.total().amount().toPlainString());
                continue;
            }
            break;
        }

        // Confirm & save
        System.out.print("Confirm checkout? [y/N]: ");
        if (!"y".equalsIgnoreCase(sc.next())) {
            System.out.println("Cancelled.");
            return;
        }

        try {
            var bill = checkout.handle(cart, cash, loc, policy, "COUNTER");
            BillPrinter.print(bill);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
    }
}
