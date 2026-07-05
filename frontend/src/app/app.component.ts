import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HrService } from './hr.service';
import { Employee, LeaveRequest, LeaveType, Payslip } from './models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnInit {

  employees: Employee[] = [];
  selected?: Employee;
  view: 'leaves' | 'payslips' = 'leaves';

  // Congés
  leaves: LeaveRequest[] = [];
  form: { type: LeaveType; startDate: string; endDate: string; comment: string } = {
    type: 'CP', startDate: '', endDate: '', comment: '',
  };

  // Bulletins
  payslips: Payslip[] = [];
  openedPayslip?: Payslip;

  toast = '';

  constructor(private hr: HrService) {}

  ngOnInit(): void {
    console.info('[HR-UI] Démarrage du portail RH — chargement des employés…');
    this.hr.getEmployees().subscribe(list => {
      this.employees = list;
      console.info(`[HR-UI] ${list.length} employé(s) chargé(s)`, list);
      if (list.length) {
        this.selectEmployee(list[0].id);
      }
    });
  }

  selectEmployee(id: number): void {
    this.selected = this.employees.find(e => e.id === Number(id));
    console.info('[HR-UI] Employé sélectionné :', this.selected?.fullName);
    this.openedPayslip = undefined;
    this.loadLeaves();
    this.loadPayslips();
  }

  setView(v: 'leaves' | 'payslips'): void {
    this.view = v;
    console.info('[HR-UI] Vue active :', v);
  }

  initials(e?: Employee): string {
    if (!e) return '?';
    return (e.firstName[0] + e.lastName[0]).toUpperCase();
  }

  // ─── Congés ─────────────────────────────────────────────
  loadLeaves(): void {
    if (!this.selected) return;
    this.hr.getLeaves(this.selected.id).subscribe(l => {
      this.leaves = l;
      console.info(`[HR-UI][CONGES] Historique chargé : ${l.length} demande(s)`, l);
    });
  }

  submitLeave(): void {
    if (!this.selected) return;
    if (!this.form.startDate || !this.form.endDate) {
      this.showToast('Veuillez saisir les dates de début et de fin.');
      return;
    }
    const payload: Partial<LeaveRequest> = {
      employeeId: this.selected.id,
      type: this.form.type,
      startDate: this.form.startDate,
      endDate: this.form.endDate,
      comment: this.form.comment,
    };
    console.info('[HR-UI][CONGES] Soumission de la demande…', payload);
    this.hr.submitLeave(payload).subscribe({
      next: saved => {
        console.info('[HR-UI][CONGES] Demande enregistrée :', saved);
        this.showToast(`Demande enregistrée — ${saved.workingDays} jour(s) ouvré(s), statut : En attente`);
        this.form = { type: 'CP', startDate: '', endDate: '', comment: '' };
        this.loadLeaves();
      },
      error: err => {
        console.error('[HR-UI][CONGES] Erreur soumission :', err);
        this.showToast('Erreur : ' + (err?.error?.error ?? 'soumission impossible'));
      },
    });
  }

  /** Estimation locale des jours ouvrés (aperçu avant envoi ; le backend recalcule). */
  get estimatedDays(): number | null {
    if (!this.form.startDate || !this.form.endDate) return null;
    const start = new Date(this.form.startDate);
    const end = new Date(this.form.endDate);
    if (end < start) return null;
    let count = 0;
    const cursor = new Date(start);
    while (cursor <= end) {
      const day = cursor.getDay();
      if (day !== 0 && day !== 6) count++;
      cursor.setDate(cursor.getDate() + 1);
    }
    return count;
  }

  typeLabel(t: LeaveType): string {
    return { CP: 'Congés payés', RTT: 'RTT', SANS_SOLDE: 'Sans solde' }[t];
  }

  statusLabel(s: string): string {
    return { EN_ATTENTE: 'En attente', VALIDE: 'Validé', REFUSE: 'Refusé' }[s] ?? s;
  }

  statusClass(s: string): string {
    return { EN_ATTENTE: 'warn', VALIDE: 'ok', REFUSE: 'ko' }[s] ?? 'warn';
  }

  // ─── Bulletins ──────────────────────────────────────────
  loadPayslips(): void {
    if (!this.selected) return;
    this.hr.getPayslips(this.selected.id).subscribe(p => {
      this.payslips = p;
      console.info(`[HR-UI][PAIE] ${p.length} bulletin(s) chargé(s)`, p);
    });
  }

  openPayslip(id: number): void {
    console.info('[HR-UI][PAIE] Ouverture du bulletin', id);
    this.hr.getPayslip(id).subscribe(p => {
      this.openedPayslip = p;
      console.info('[HR-UI][PAIE] Détail chargé :', p);
    });
  }

  closePayslip(): void {
    this.openedPayslip = undefined;
  }

  downloadPayslip(id: number): void {
    const url = this.hr.payslipDownloadUrl(id);
    console.info('[HR-UI][PAIE] Téléchargement PDF simulé :', url);
    this.showToast('Téléchargement du bulletin PDF…');
    window.open(url, '_blank');
  }

  eur(v: number): string {
    return v.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
  }

  private showToast(msg: string): void {
    this.toast = msg;
    setTimeout(() => (this.toast = ''), 3500);
  }
}
