package com.hotel.finance.services;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.finance.Invoice;
import com.hotel.finance.enums.InvoiceStatus;
import com.hotel.finance.repositories.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /** Next ID: finds the highest number and adds 1 -> INV001, INV002... */
    public synchronized String generateNextId() {
        int max = 0;
        for (Invoice invoice : invoiceRepository.findAll()) {
            String id = invoice.getInvoiceId();
            if (id != null && id.toUpperCase().startsWith("INV")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(3)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return String.format("INV%03d", max + 1);
    }

    /** Creates a new invoice for a booking */
    public Invoice create(Invoice invoice) {
        // 1. Values the system sets, not the user
        invoice.setInvoiceId(generateNextId());
        invoice.setIssuedDate(LocalDate.now());
        invoice.setAmountPaid(0);
        invoice.setDiscountPercent(0);   // customer discount is added in a later step
        invoice.setStatus(InvoiceStatus.UNPAID);

        // 2. Rules of the Invoice class
        List<String> errors = new ArrayList<>(invoice.validate());

        // 3. A booking can have only one active (not VOID) invoice
        if (invoice.getBookingId() != null
                && invoiceRepository.findActiveByBookingId(invoice.getBookingId()).isPresent()) {
            errors.add("This booking already has an invoice");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 4. Clean the IDs and save
        invoice.setBookingId(invoice.getBookingId().trim().toUpperCase());
        invoice.setCustomerId(invoice.getCustomerId().trim().toUpperCase());
        return invoiceRepository.save(invoice);
    }

    public List<Invoice> getAll() {
        return invoiceRepository.findAll();
    }

    public Invoice getById(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    /** Empty keyword -> all invoices, else search by invoice, booking or customer ID */
    public List<Invoice> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }
        return invoiceRepository.search(keyword);
    }
}