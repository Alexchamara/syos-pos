package main.java.cli.bill;

import main.java.domain.billing.Bill;
import main.java.application.usecase.QuoteUseCase;
import main.java.domain.shared.Currency;
import java.time.format.DateTimeFormatter;

public final class BillPrinter {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void print(Bill b){
        System.out.println("\n----------- BILL -----------");
        System.out.println("Serial: " + b.serial());
        System.out.println("Date  : " + b.dateTime().format(DF));
        b.lines().forEach(l ->
                System.out.printf("%-10s x%-3d @ %s = %s%n",
                        l.productCode().value(), l.qty().value(),
                        Currency.formatSimple(l.unitPrice()),
                        Currency.formatSimple(l.lineTotal())));
        System.out.println("----------------------------");
        System.out.println("TOTAL   : " + Currency.formatSimple(b.total()));
        System.out.println("CASH    : " + Currency.formatSimple(b.cash()));
        System.out.println("CHANGE  : " + Currency.formatSimple(b.change()));
        System.out.println("----------------------------\n");
    }

    public static void printPreview(QuoteUseCase.Quote q) {
        System.out.println("\n-------- PRE-BILL (PREVIEW) --------");
        q.lines().forEach(l ->
                System.out.printf("%-10s x%-3d @ %s  = %s%n",
                        l.productCode().value(),
                        l.qty().value(),
                        Currency.formatSimple(l.unitPrice()),
                        Currency.formatSimple(l.lineTotal())));
        System.out.println("------------------------------------");
        System.out.println("Subtotal : " + Currency.formatSimple(q.subtotal()));
        System.out.println("Discount : " + Currency.formatSimple(q.discount()));
        System.out.println("TOTAL    : " + Currency.formatSimple(q.total()));
        System.out.println("(Pay this amount or more to proceed)");
        System.out.println("------------------------------------\n");
    }
}