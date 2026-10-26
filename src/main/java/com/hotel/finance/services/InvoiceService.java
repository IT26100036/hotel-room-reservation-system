package com.hotel.finance.services;

import com.hotel.customer.Customer;
import com.hotel.customer.CustomerService;
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
    private final CustomerService customerService;

    public InvoiceService(InvoiceRepository invoiceRepository, CustomerService customerService) {
        this.invoiceRepository = invoiceRepository;
        this.customerService = customerService;
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
        invoice.setStatus(InvoiceStatus.UNPAID);

        // 2. Rules of the Invoice class
        List<String> errors = new ArrayList<>(invoice.validate());

        // Customer must exist; their discount is copied onto the invoice
        if (invoice.getCustomerId() != null && !invoice.getCustomerId().trim().isEmpty()) {
            try {
                Customer customer = customerService.getById(invoice.getCustomerId().trim());
                invoice.setDiscountPercent(customer.getDiscount());
            } catch (NotFoundException e) {
                errors.add("Customer not found");
            }
        }

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

    /** Changes the charges of an invoice. ID, booking, customer and payments never change here. */
    public Invoice update(String invoiceId, Invoice form) {
        // 1. Load the existing invoice
        Invoice existing = getById(invoiceId);

        // 2. A voided invoice is closed
        if (existing.getStatus() == InvoiceStatus.VOID) {
            throw new ValidationException("A void invoice cannot be changed");
        }

        // 3. Copy only the editable fields
        existing.setRoomCharge(form.getRoomCharge());
        existing.setExtraCharges(form.getExtraCharges());

        // 4. Check the rules (this also stops the total going below what is already paid)
        List<String> errors = existing.validate();
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        // 5. UNPAID, PARTIALLY_PAID or PAID from the new balance, then save
        existing.updateStatus();
        return invoiceRepository.save(existing);
    }

    /** Cancels a wrong invoice. It stays in the file but is marked VOID. */
    public Invoice voidInvoice(String invoiceId) {
        Invoice existing = getById(invoiceId);

        if (existing.getStatus() == InvoiceStatus.VOID) {
            throw new ValidationException("This invoice is already void");
        }

        existing.setStatus(InvoiceStatus.VOID);
        return invoiceRepository.save(existing);
    }
}