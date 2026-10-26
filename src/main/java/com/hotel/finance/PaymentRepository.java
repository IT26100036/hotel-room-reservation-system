package com.hotel.finance;

import com.hotel.finance.enums.PaymentMethod;
import com.hotel.finance.enums.PaymentType;
import com.hotel.finance.methods.Card;
import com.hotel.finance.methods.Cash;
import com.hotel.finance.methods.Online;
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
 * Saves payments in a text file (data/payments.txt)
 * detail1 / detail2 hold the extra fields of each payment method:
 *   CASH   -> amountReceived | (empty)
 *   CARD   -> cardHolderName | cardLastFour
 *   ONLINE -> referenceNo    | (empty)
 */

@Repository
public class PaymentRepository {

    private final String filePath;
    private static final String HEADER =
            "#paymentId|invoiceId|paymentMethod|amount|paymentType|paymentDate|detail1|detail2";

    public PaymentRepository(@Value("${app.data.payment-file:data/payments.txt}") String filePath) {
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
            System.err.println("Could not create payment file: " + e.getMessage());
        }
    }

    /** Reads every payment from the file */
    public synchronized List<Payment> findAll() {
        List<Payment> payments = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return payments;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;   // skip empty lines and the header
                }
                Payment payment = parseLine(line);
                if (payment != null) {
                    payments.add(payment);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read payment file: " + e.getMessage());
        }
        return payments;
    }

    public Optional<Payment> findById(String paymentId) {
        if (paymentId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(p -> paymentId.equalsIgnoreCase(p.getPaymentId()))
                .findFirst();
    }

    /** All payments made against one invoice */
    public List<Payment> findByInvoiceId(String invoiceId) {
        return findAll().stream()
                .filter(p -> p.getInvoiceId().equalsIgnoreCase(invoiceId))
                .collect(Collectors.toList());
    }

    /** Adds a new payment, or replaces the one with the same ID */
    public synchronized Payment save(Payment payment) {
        List<Payment> all = findAll();
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPaymentId().equalsIgnoreCase(payment.getPaymentId())) {
                all.set(i, payment);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(payment);
        }

        writeAll(all);
        return payment;
    }

    public synchronized void delete(Payment payment) {
        List<Payment> remaining = findAll().stream()
                .filter(p -> !p.getPaymentId().equalsIgnoreCase(payment.getPaymentId()))
                .collect(Collectors.toList());
        writeAll(remaining);
    }

    /** Rewrites the whole file */
    private synchronized void writeAll(List<Payment> payments) {
        ensureFileExists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (Payment p : payments) {
                writer.write(formatPayment(p));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write payment file: " + e.getMessage());
        }
    }

    /** Payment -> one line of text */
    private String formatPayment(Payment p) {
        String detail1 = "";
        String detail2 = "";

        if (p instanceof Cash cash) {
            detail1 = String.valueOf(cash.getAmountReceived());
        } else if (p instanceof Card card) {
            detail1 = clean(card.getCardHolderName());
            detail2 = clean(card.getCardLastFour());
        } else if (p instanceof Online online) {
            detail1 = clean(online.getReferenceNo());
        }

        return String.join("|",
                p.getPaymentId(),
                p.getInvoiceId(),
                p.getPaymentMethod().name(),
                String.valueOf(p.getAmount()),
                p.getPaymentType().name(),
                p.getPaymentDate() != null ? p.getPaymentDate().toString() : "",
                detail1,
                detail2
        );
    }

    /** Removes | and new lines so a value can't break the line format */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "-").replace("\r", "").replace("\n", " ").trim();
    }

    /** One line of text -> Cash, Card or Online (decided by the paymentMethod column) */
    private Payment parseLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 8) {
                return null;
            }

            String paymentId = p[0].trim();
            String invoiceId = p[1].trim();
            PaymentMethod method = PaymentMethod.valueOf(p[2].trim());
            double amount = Double.parseDouble(p[3].trim());
            PaymentType type = PaymentType.valueOf(p[4].trim());
            LocalDate date = p[5].trim().isEmpty() ? null : LocalDate.parse(p[5].trim());

            switch (method) {
                case CASH:
                    return new Cash(paymentId, invoiceId, amount, type, date,
                            Double.parseDouble(p[6].trim()));
                case CARD:
                    return new Card(paymentId, invoiceId, amount, type, date,
                            p[6].trim(), p[7].trim());
                case ONLINE:
                    return new Online(paymentId, invoiceId, amount, type, date,
                            p[6].trim());
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Could not read payment line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}