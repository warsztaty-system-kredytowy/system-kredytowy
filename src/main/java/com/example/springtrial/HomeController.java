package com.example.springtrial;

import com.example.springtrial.LoanApplication.LoanApplication;
import com.example.springtrial.LoanApplication.LoanApplicationService;
import com.example.springtrial.LoanApplication.LoanStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class HomeController {

    private final LoanApplicationService service;

    public HomeController(LoanApplicationService service) {
        this.service = service;
    }

    @GetMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    @GetMapping("/applications")
    public String applications(Model model) {
        model.addAttribute("applications", service.findAllForCurrentUser());
        return "applications";
    }

    @GetMapping("/submit")
    public String submit() {
        return "submit";
    }

    @GetMapping("/about-us")
    public String aboutUs() {
        return "about-us";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping("/employee/login")
    public String employeeLogin() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    @GetMapping("/employee/dashboard")
    public String employeeDashboard(Model model) {
        List<LoanApplication> apps = service.findAll();

        long pending = apps.stream()
                .filter(a -> a.getStatus() == LoanStatus.NEW
                        || a.getStatus() == LoanStatus.IN_PROCESSING
                        || a.getStatus() == LoanStatus.APPEALED)
                .count();
        long approved = apps.stream().filter(a -> a.getStatus() == LoanStatus.APPROVED).count();
        long denied = apps.stream().filter(a -> a.getStatus() == LoanStatus.DENIED).count();
        BigDecimal totalAmount = apps.stream()
                .map(LoanApplication::getLoanAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("applications", apps);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("approvedCount", approved);
        model.addAttribute("deniedCount", denied);
        model.addAttribute("totalAmount", totalAmount);
        return "employee-dashboard";
    }

    @GetMapping("/employee/review/{id}")
    public String employeeReview(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        if (service.isCurrentUserOwnerOf(id)) {
            redirectAttributes.addFlashAttribute("error", "You cannot review your own credit application!");
            return "redirect:/employee/dashboard";
        }
        service.startReview(id);
        return service.findById(id)
                .map(app -> {
                    model.addAttribute("app", app);
                    return "employee-review";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Application not found with id: " + id);
                    return "redirect:/employee/dashboard";
                });
    }
}
