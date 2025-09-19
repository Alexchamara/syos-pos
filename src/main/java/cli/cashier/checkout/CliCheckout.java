package main.java.cli.cashier.checkout;

import main.java.application.services.AvailabilityService;
import main.java.application.services.MainStoreService;
import main.java.application.services.ShortageEventService;
import main.java.application.usecase.CheckoutCashUseCase;
import main.java.application.usecase.CheckoutCashUseCase.Item;
import main.java.application.usecase.QuoteUseCase;
import main.java.cli.bill.BillPrinter;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.pricing.DiscountPolicy;
import main.java.domain.pricing.NoDiscount;
import main.java.domain.pricing.PercentDiscount;
import main.java.domain.shared.Currency;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class CliCheckout {
    private final CheckoutCashUseCase checkout;
    private final BatchSelectionStrategy strategyDefault;
    private final QuoteUseCase quote;
    private final AvailabilityService availability;
    private final MainStoreService mainStoreService;
    private final ShortageEventService shortageEvents;

    public CliCheckout(CheckoutCashUseCase checkout,
                       BatchSelectionStrategy strategyDefault,
                       QuoteUseCase quote,
                       AvailabilityService availability,
                       MainStoreService mainStoreService,
                       ShortageEventService shortageEvents) {
        this.checkout = checkout;
        this.strategyDefault = strategyDefault;
        this.quote = quote;
        this.availability = availability;
        this.mainStoreService = mainStoreService;
        this.shortageEvents = shortageEvents;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SYOS Checkout (CLI) ===");
        System.out.println("Commands:");
        System.out.println("  - Enter product code to add items");
        System.out.println("  - 'view' to see cart contents");
        System.out.println("  - 'remove' to remove items from cart");
        System.out.println("  - 'done' to proceed to checkout");

        List<Item> cart = new ArrayList<>();

        while (true) {
            System.out.print("\nCart (" + cart.size() + " items) > Enter command or product code: ");
            String input = sc.next().trim();

            if (input.equalsIgnoreCase("done")) {
                break;
            } else if (input.equalsIgnoreCase("view")) {
                viewCart(cart);
                continue;
            } else if (input.equalsIgnoreCase("remove")) {
                removeFromCart(sc, cart);
                continue;
            }

            // Handle product code input
            String code = input;
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
                addToCart(cart, code, finalQuantity);
                System.out.println("Added " + finalQuantity + " x " + code + " to cart");
            } else {
                System.out.println("Item not added to cart");
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
     * Display cart contents with item details
     */
    private void viewCart(List<Item> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty");
            return;
        }

        System.out.println("\n=== CART CONTENTS ===");
        System.out.printf("%-5s %-15s %-10s%n", "No.", "Product Code", "Quantity");
        System.out.println("------------------------------");

        for (int i = 0; i < cart.size(); i++) {
            Item item = cart.get(i);
            System.out.printf("%-5d %-15s %-10d%n", (i + 1), item.code(), item.qty());
        }

        System.out.println("------------------------------");
        System.out.println("Total items: " + cart.size());

        // Show preview if items exist
        try {
            var preview = quote.preview(cart, new NoDiscount());
            System.out.println("Estimated total: " + Currency.formatSimple(preview.total()));
        } catch (Exception e) {
            System.out.println("Could not calculate preview total");
        }
    }

    /**
     * Remove items from cart
     */
    private void removeFromCart(Scanner sc, List<Item> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty - nothing to remove");
            return;
        }

        viewCart(cart);

        System.out.print("Enter item number to remove (1-" + cart.size() + ") or 'cancel': ");
        String input = sc.next().trim();

        if (input.equalsIgnoreCase("cancel")) {
            return;
        }

        try {
            int itemNumber = Integer.parseInt(input);
            if (itemNumber < 1 || itemNumber > cart.size()) {
                System.out.println("Invalid item number");
                return;
            }

            Item removedItem = cart.remove(itemNumber - 1);
            System.out.println("Removed " + removedItem.qty() + " x " + removedItem.code() + " from cart");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number");
        }
    }

    /**
     * Add item to cart, consolidating quantities if the same product already exists
     */
    private void addToCart(List<Item> cart, String code, int quantity) {
        // Check if product already exists in cart
        for (int i = 0; i < cart.size(); i++) {
            Item existingItem = cart.get(i);
            if (existingItem.code().equals(code)) {
                // Replace with combined quantity
                int newQuantity = existingItem.qty() + quantity;
                cart.set(i, new Item(code, newQuantity));
                System.out.println("Updated existing item. New quantity: " + newQuantity);
                return;
            }
        }

        // Product not in cart, add new item
        cart.add(new Item(code, quantity));
    }

    /**
     * Handles the restocking logic
     */
    private int handleItemRestocking(Scanner sc, String code, int requestedQty) {
        // Step 1: Check SHELF stock
        int shelfStock = availability.available(code, StockLocation.SHELF);

        System.out.println("=== Stock Check for " + code + " ===");
        System.out.println("SHELF stock: " + shelfStock + ", Requested: " + requestedQty);

        if (shelfStock >= requestedQty) {
            System.out.println("Sufficient stock on SHELF, proceeding with checkout");
            return requestedQty;
        }

        // Step 2: SHELF insufficient, check MAIN_STORE
        System.out.println("Insufficient stock on SHELF (need " + (requestedQty - shelfStock) + " more)");

        int mainStoreStock = mainStoreService.getAvailableQuantity(code);
        System.out.println("MAIN_STORE stock: " + mainStoreStock);

        int neededFromMain = requestedQty - shelfStock;

        if (mainStoreStock >= neededFromMain) {
            // MAIN_STORE has enough, ask cashier to transfer
            System.out.println("MAIN_STORE has sufficient stock to fulfill the request");
            System.out.print("Transfer " + neededFromMain + " units from MAIN_STORE to SHELF? [y/N]: ");

            if ("y".equalsIgnoreCase(sc.next())) {
                try {
                    System.out.println("Transferring " + neededFromMain + " units from MAIN_STORE to SHELF...");
                    // Use existing transfer method from availability service
                    transferFromMainToShelf(code, neededFromMain);
                    System.out.println("Transfer completed successfully!");
                    return requestedQty;
                } catch (Exception e) {
                    System.out.println("Transfer failed: " + e.getMessage());
                    return handlePartialAvailability(sc, code, shelfStock);
                }
            } else {
                System.out.println("Transfer declined by cashier");
                return handlePartialAvailability(sc, code, shelfStock);
            }
        }

        // Step 3: MAIN_STORE insufficient, check WEB
        System.out.println("MAIN_STORE insufficient (has " + mainStoreStock + ", need " + neededFromMain + ")");

        int webStock = availability.available(code, StockLocation.WEB);
        System.out.println("WEB stock: " + webStock);

        int totalAvailable = shelfStock + mainStoreStock + webStock;
        System.out.println("Total available across all locations: " + totalAvailable);

        if (totalAvailable < requestedQty) {
            // Not enough stock anywhere
            System.out.println("Insufficient total stock across all locations!");
            String msg = String.format("URGENT: Product %s insufficient stock (SHELF: %d + MAIN: %d + WEB: %d = %d, Required: %d)",
                    code, shelfStock, mainStoreStock, webStock, totalAvailable, requestedQty);
            System.out.println(msg);

            try {
                shortageEvents.record(msg);
                System.out.println("Manager notified with high priority");
            } catch (Throwable e) {
                System.out.println("Failed to notify manager: " + e.getMessage());
            }

            return handlePartialAvailability(sc, code, totalAvailable);
        }

        // Calculate how much we need from WEB
        int stillNeedFromWeb = neededFromMain - mainStoreStock;

        if (webStock >= stillNeedFromWeb) {
            // WEB has enough, ask cashier for two-step transfer
            System.out.println("WEB has sufficient stock (" + webStock + " available)");
            System.out.println("This requires a two-step transfer:");
            System.out.println("  Step 1: WEB → MAIN_STORE (" + stillNeedFromWeb + " units)");
            System.out.println("  Step 2: MAIN_STORE → SHELF (" + neededFromMain + " units)");
            System.out.print("Proceed with two-step transfer? [y/N]: ");

            if ("y".equalsIgnoreCase(sc.next())) {
                try {
                    // Step 1: Transfer from WEB to MAIN_STORE
                    System.out.println("Step 1: Transferring " + stillNeedFromWeb + " units from WEB to MAIN_STORE...");
                    transferFromWebToMain(code, stillNeedFromWeb);
                    System.out.println("Step 1 completed");

                    // Step 2: Transfer from MAIN_STORE to SHELF
                    System.out.println("Step 2: Transferring " + neededFromMain + " units from MAIN_STORE to SHELF...");
                    transferFromMainToShelf(code, neededFromMain);
                    System.out.println("Step 2 completed");

                    System.out.println("Two-step transfer completed successfully!");
                    return requestedQty;

                } catch (Exception e) {
                    System.out.println("Transfer failed: " + e.getMessage());
                    return handlePartialAvailability(sc, code, shelfStock);
                }
            } else {
                System.out.println("Two-step transfer declined by cashier");
                return handlePartialAvailability(sc, code, shelfStock);
            }
        } else {
            // Even WEB doesn't have enough
            System.out.println("Even WEB doesn't have sufficient stock");
            return handlePartialAvailability(sc, code, totalAvailable);
        }
    }

    /**
     * Handles partial availability scenarios - offers cashier to use available quantity
     */
    private int handlePartialAvailability(Scanner sc, String code, int availableQty) {
        if (availableQty > 0) {
            System.out.print("Use only available quantity (" + availableQty + ") from SHELF? [y/N]: ");
            if ("y".equalsIgnoreCase(sc.next())) {
                return availableQty;
            }
        }
        return 0; // Item will not be added to cart
    }

    /**
     * Transfer stock from MAIN_STORE to SHELF
     */
    private void transferFromMainToShelf(String productCode, int quantity) throws Exception {
        // Use the existing inventory repository transfer method
        availability.transferStock(productCode, StockLocation.MAIN_STORE, StockLocation.SHELF, quantity);
    }

    /**
     * Transfer stock from WEB to MAIN_STORE
     */
    private void transferFromWebToMain(String productCode, int quantity) throws Exception {
        // Use the existing inventory repository transfer method
        availability.transferStock(productCode, StockLocation.WEB, StockLocation.MAIN_STORE, quantity);
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
            System.out.print("Cash (cents, e.g., 10000 = " + Currency.SYMBOL + "100.00): ");
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
