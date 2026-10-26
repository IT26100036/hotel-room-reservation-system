package com.hotel.finance.controllers;

import com.hotel.common.NotFoundException;
import com.hotel.common.ValidationException;
import com.hotel.finance.Invoice;
import com.hotel.finance.Payment;
import com.hotel.finance.enums.PaymentMethod;
import com.hotel.finance.enums.PaymentType;
import com.hotel.finance.methods.Card;
import com.hotel.finance.methods.Cash;
import com.hotel.finance.methods.Online;
import com.hotel.finance.services.FinanceLog;
import com.hotel.finance.services.InvoiceService;
import com.hotel.finance.services.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/finance")
public class FinanceController {

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final FinanceLog financeLog;

    public FinanceController(InvoiceService invoiceService, PaymentService paymentService, FinanceLog financeLog) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.financeLog = financeLog;
    }

    /**
     * This creates Cash, Card or Online from the "paymentMethod" value,
     */
    @ModelAttribute("payment")
    public Payment paymentFromMethod(@RequestParam(value = "paymentMethod", required = false) String method) {
        if ("CARD".equalsIgnoreCase(method)) {
            return new Card();
        }
        if ("ONLINE".equalsIgnoreCase(method)) {
            return new Online();
        }
        return new Cash();
    }

    /* ---------- Invoices ---------- */

    @GetMapping
    public String listInvoices(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("invoiceList", invoiceService.search(keyword));
        model.addAttribute("keyword", keyword);
        return "finance/list";
    }

    @GetMapping("/invoice/view/{id}")
    public String viewInvoice(@PathVariable("id") String id, Model model) {
        Invoice invoice = invoiceService.getById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", paymentService.getByInvoiceId(invoice.getInvoiceId()));
        return "finance/details";
    }

    @GetMapping("/invoice/create")
    public String showCreateInvoice(Model model) {
        model.addAttribute("invoice", new Invoice());
        model.addAttribute("isEdit", false);
        return "finance/invoice-form";
    }

    @PostMapping("/invoice/create")
    public String createInvoice(@ModelAttribute("invoice") Invoice invoice, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Invoice saved = invoiceService.create(invoice);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice " + saved.getInvoiceId() + " created successfully!");
            return "redirect:/finance/invoice/view/" + saved.getInvoiceId();
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("isEdit", false);
            return "finance/invoice-form";
        }
    }

    @GetMapping("/invoice/edit/{id}")
    public String showEditInvoice(@PathVariable("id") String id, Model model) {
        model.addAttribute("invoice", invoiceService.getById(id));
        model.addAttribute("isEdit", true);
        return "finance/invoice-form";
    }

    @PostMapping("/invoice/edit/{id}")
    public String updateInvoice(@PathVariable("id") String id, @ModelAttribute("invoice") Invoice form,
                                Model model, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice updated successfully!");
            return "redirect:/finance/invoice/view/" + id;
        } catch (ValidationException e) {
            // show the saved invoice again with the user's new numbers and the errors
            Invoice existing = invoiceService.getById(id);
            existing.setRoomCharge(form.getRoomCharge());
            existing.setExtraCharges(form.getExtraCharges());
            model.addAttribute("invoice", existing);
            model.addAttribute("errors", e.getErrors());
            model.addAttribute("isEdit", true);
            return "finance/invoice-form";
        }
    }

    @PostMapping("/invoice/void/{id}")
    public String voidInvoice(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.voidInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice " + id + " is now void.");
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/finance/invoice/view/" + id;
    }

    /* ---------- Payments ---------- */

    @GetMapping("/payment/create")
    public String showPaymentForm(@RequestParam("invoiceId") String invoiceId,
                                  @ModelAttribute("payment") Payment payment, Model model) {
        Invoice invoice = invoiceService.getById(invoiceId);
        payment.setInvoiceId(invoice.getInvoiceId());
        payment.setAmount(invoice.getBalance());   // suggest the full balance
        addPaymentFormData(model, invoice);
        return "finance/payment-form";
    }

    @PostMapping("/payment/create")
    public String recordPayment(@ModelAttribute("payment") Payment payment, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            Payment saved = paymentService.record(payment);
            redirectAttributes.addFlashAttribute("successMessage", "Payment " + saved.getPaymentId() + " recorded successfully!");
            return "redirect:/finance/invoice/view/" + saved.getInvoiceId();
        } catch (ValidationException e) {
            model.addAttribute("errors", e.getErrors());
            Invoice invoice = null;
            try {
                invoice = invoiceService.getById(payment.getInvoiceId());
            } catch (NotFoundException ignored) {
            }
            addPaymentFormData(model, invoice);
            return "finance/payment-form";
        }
    }

    private void addPaymentFormData(Model model, Invoice invoice) {
        model.addAttribute("invoice", invoice);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("paymentTypes", PaymentType.values());
    }

    /* ---------- History ---------- */

    @GetMapping("/history")
    public String viewHistory(Model model) {
        model.addAttribute("logs", financeLog.readAll());
        return "finance/history";
    }
}