package com.hotel.finance.services;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.finance.Invoice;
import com.hotel.finance.Payment;
import com.hotel.finance.enums.InvoiceStatus;
import com.hotel.finance.repositories.InvoiceRepository;
import com.hotel.finance.repositories.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
    }

    /** Next ID: finds the highest number and adds 1 -> PAY001, PAY002... */
    public synchronized String generateNextId() {
        int max = 0;
        for (Payment payment : paymentRepository.findAll()) {
            String id = payment.getPaymentId();
            if (id != null && id.toUpperCase().startsWith("PAY")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(3)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("PAY%03d", max + 1);
    }

    /**
     * Records a cash, card or online payment against an invoice.
     * The payment object is already a Cash, Card or Online (chosen on the form).
     */
    public synchronized Payment record(Payment payment) {
        // 1. Values the system sets
        payment.setPaymentId(generateNextId());
        payment.setPaymentDate(LocalDate.now());

        // 2. Rules of the payment (and of its own method: cash, card or online)
        List<String> errors = new ArrayList<>(payment.validate());

        // 3. Rules that need the invoice
        Invoice invoice = null;
        if (payment.getInvoiceId() != null && !payment.getInvoiceId().trim().isEmpty()) {
            try {
                invoice = invoiceService.getById(payment.getInvoiceId().trim());
            } catch (NotFoundException e) {
                errors.add("Invoice not found");
            }
        }

        if (invoice != null) {
            if (invoice.getStatus() == InvoiceStatus.VOID) {
                errors.add("Cannot pay a void invoice");
            } else if (invoice.getStatus() == InvoiceStatus.PAID) {
                errors.add("This invoice is already fully paid");
            } else if (payment.getAmount() > invoice.getBalance()) {
                errors.add(String.format("Amount cannot be more than the balance (LKR %.2f)", invoice.getBalance()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 4. Save the payment, then add it to the invoice (this updates the status too)
        assert invoice != null;
        payment.setInvoiceId(invoice.getInvoiceId());
        paymentRepository.save(payment);

        invoice.addPayment(payment.getAmount());
        invoiceRepository.save(invoice);

        return payment;
    }

    public List<Payment> getAll() {
        return paymentRepository.findAll();
    }

    public Payment getById(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    /** All payments of one invoice (shown on the invoice details page) */
    public List<Payment> getByInvoiceId(String invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }
}