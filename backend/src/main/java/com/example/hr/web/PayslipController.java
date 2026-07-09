package com.example.hr.web;

import com.example.hr.model.Employee;
import com.example.hr.model.Payslip;
import com.example.hr.model.PayslipLine;
import com.example.hr.repository.EmployeeRepository;
import com.example.hr.repository.PayslipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Endpoints des bulletins de paie (liste, détail, téléchargement PDF simulé).
 */
@RestController
@RequestMapping("/api/payslips")
@CrossOrigin(origins = "*")
public class PayslipController {

    private static final Logger log = LoggerFactory.getLogger(PayslipController.class);

    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;

    public PayslipController(PayslipRepository payslipRepository,
                             EmployeeRepository employeeRepository) {
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
    }

    /** Liste des bulletins d'un employé (par mois/année). */
    @GetMapping
    public List<Payslip> list(@RequestParam Long employeeId) {
        log.info("[API] GET /api/payslips?employeeId={}", employeeId);
        return payslipRepository.findByEmployeeId(employeeId);
    }

    /** Détail d'un bulletin. */
    @GetMapping("/{id}")
    public ResponseEntity<Payslip> detail(@PathVariable Long id) {
        log.info("[API] GET /api/payslips/{}", id);
        return payslipRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Téléchargement "PDF" simulé.
     * On génère un pseudo-PDF texte (Content-Type application/pdf) — suffisant pour
     * valider le flux de téléchargement de bout en bout sans dépendance lourde
     * (iText/PDFBox) qui augmenterait la surface d'attaque scannée par Trivy.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        log.info("[API] GET /api/payslips/{}/download (PDF simulé)", id);
        Payslip p = payslipRepository.findById(id).orElse(null);
        if (p == null) {
            return ResponseEntity.notFound().build();
        }
        Employee e = employeeRepository.findById(p.getEmployeeId()).orElse(null);
        byte[] content = renderPseudoPdf(p, e);

        String filename = "bulletin_" + (e == null ? "emp" : e.getMatricule())
                + "_" + p.getYear() + "-" + String.format("%02d", p.getMonth()) + ".pdf";

        log.info("[API] Bulletin #{} téléchargé : {} ({} octets)", id, filename, content.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    /**
     * Construit un document PDF minimal valide (un seul flux texte) à la main.
     * Objectif démo : le navigateur reçoit bien un fichier .pdf ouvrable.
     */
    private byte[] renderPseudoPdf(Payslip p, Employee e) {
        StringBuilder body = new StringBuilder();
        body.append("BULLETIN DE PAIE (SIMULATION - DONNEES FICTIVES)\n");
        body.append("Periode : ").append(p.getPeriod()).append("\n");
        if (e != null) {
            body.append("Employe : ").append(e.getFullName())
                    .append(" (").append(e.getMatricule()).append(")\n");
            body.append("Poste   : ").append(e.getJobTitle()).append("\n");
        }
        body.append("------------------------------------------\n");
        for (PayslipLine l : p.getLines()) {
            body.append(String.format("%-32s %12.2f EUR%n", l.getLabel(), l.getAmount()));
        }
        body.append("------------------------------------------\n");
        body.append(String.format("Salaire brut        : %.2f EUR%n", p.getGrossSalary()));
        body.append(String.format("Total cotisations   : %.2f EUR%n", p.getTotalContributions()));
        body.append(String.format("Net a payer         : %.2f EUR%n", p.getNetSalary()));
        body.append(String.format("Cumul net (annee)   : %.2f EUR%n", p.getCumulativeNet()));

        return buildMinimalPdf(body.toString());
    }

    /** Assemble une structure PDF 1.4 minimale contenant du texte brut. */
    private byte[] buildMinimalPdf(String text) {
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 10 Tf 40 780 Td 12 TL\n");
        for (String line : text.split("\n")) {
            content.append("(").append(escapePdf(line)).append(") Tj T*\n");
        }
        content.append("ET");

        String stream = content.toString();
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        pdf.append("1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n");
        pdf.append("2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n");
        pdf.append("3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] ")
                .append("/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>endobj\n");
        pdf.append("4 0 obj<< /Length ").append(stream.length()).append(" >>stream\n");
        pdf.append(stream).append("\nendstream endobj\n");
        pdf.append("5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>endobj\n");
        pdf.append("trailer<< /Root 1 0 R >>\n");
        pdf.append("%%EOF");

        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escapePdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
