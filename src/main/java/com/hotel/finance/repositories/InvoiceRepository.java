package com.hotel.finance.repositories;

import com.hotel.finance.Invoice;
import com.hotel.finance.enums.InvoiceStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Saves invoices in a text file (data/invoices.txt)
 */

@Repository
public class InvoiceRepository {

    private final String filePath;
    private static final String HEADER =
            "#invoiceId|bookingId|customerId|roomCharge|extraCharges|discountPercent|amountPaid|status|issuedDate";

    public InvoiceRepository(@Value("${app.data.invoice-file:data/invoices.txt}") String filePath) {
        this.filePath = filePath;
        ensureFileExists();
    }

    /** Creates the data folder and the file with its header line if they are missing */
    private synchronized void ensureFileExists() {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    writer.write(HEADER);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not create invoice file: " + e.getMessage());
        }
    }

    /** Reads every invoice from the file */
    public synchronized List<Invoice> findAll() {
        List<Invoice> invoices = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return invoices;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;   // skip empty lines and the header
                }
                Invoice invoice = parseLine(line);
                if (invoice != null) {
                    invoices.add(invoice);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read invoice file: " + e.getMessage());
        }
        return invoices;
    }

    public Optional<Invoice> findById(String invoiceId) {
        if (invoiceId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(i -> invoiceId.equalsIgnoreCase(i.getInvoiceId()))
                .findFirst();
    }

    /** The invoice of a booking that is not VOID (a booking has only one active invoice) */
    public Optional<Invoice> findActiveByBookingId(String bookingId) {
        if (bookingId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(i -> bookingId.trim().equalsIgnoreCase(i.getBookingId()))
                .filter(i -> i.getStatus() != InvoiceStatus.VOID)
                .findFirst();
    }

    /** Invoices whose invoice ID, booking ID or customer ID contains the keyword */
    public List<Invoice> search(String keyword) {
        String k = keyword.trim().toLowerCase();
        return findAll().stream()
                .filter(i -> contains(i.getInvoiceId(), k)
                        || contains(i.getBookingId(), k)
                        || contains(i.getCustomerId(), k))
                .collect(Collectors.toList());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    /** Adds a new invoice, or replaces the one with the same ID */
    public synchronized Invoice save(Invoice invoice) {
        List<Invoice> all = findAll();
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getInvoiceId().equalsIgnoreCase(invoice.getInvoiceId())) {
                all.set(i, invoice);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(invoice);
        }

        writeAll(all);
        return invoice;
    }

    public synchronized void delete(Invoice invoice) {
        List<Invoice> remaining = findAll().stream()
                .filter(i -> !i.getInvoiceId().equalsIgnoreCase(invoice.getInvoiceId()))
                .collect(Collectors.toList());
        writeAll(remaining);
    }

    /** Rewrites the whole file */
    private synchronized void writeAll(List<Invoice> invoices) {
        ensureFileExists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (Invoice i : invoices) {
                writer.write(formatInvoice(i));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write invoice file: " + e.getMessage());
        }
    }

    /** Invoice -> one line of text */
    private String formatInvoice(Invoice i) {
        return String.join("|",
                i.getInvoiceId(),
                i.getBookingId(),
                i.getCustomerId(),
                String.valueOf(i.getRoomCharge()),
                String.valueOf(i.getExtraCharges()),
                String.valueOf(i.getDiscountPercent()),
                String.valueOf(i.getAmountPaid()),
                i.getStatus().name(),
                i.getIssuedDate() != null ? i.getIssuedDate().toString() : ""
        );
    }

    /** One line of text -> Invoice */
    private Invoice parseLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 9) {
                return null;
            }
            return new Invoice(
                    p[0].trim(),
                    p[1].trim(),
                    p[2].trim(),
                    Double.parseDouble(p[3].trim()),
                    Double.parseDouble(p[4].trim()),
                    Double.parseDouble(p[5].trim()),
                    Double.parseDouble(p[6].trim()),
                    InvoiceStatus.valueOf(p[7].trim()),
                    p[8].trim().isEmpty() ? null : LocalDate.parse(p[8].trim())
            );
        } catch (Exception e) {
            System.err.println("Could not read invoice line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}