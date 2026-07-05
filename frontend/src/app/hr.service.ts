import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee, LeaveRequest, Payslip } from './models';

/**
 * Service d'accès à l'API RH.
 * Toutes les URL sont relatives : Nginx (frontend) proxy /api/* -> backend:8081.
 */
@Injectable({ providedIn: 'root' })
export class HrService {

  constructor(private http: HttpClient) {}

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>('/api/employees');
  }

  // ── Congés ──────────────────────────────────────────────
  getLeaves(employeeId: number): Observable<LeaveRequest[]> {
    return this.http.get<LeaveRequest[]>(`/api/leaves?employeeId=${employeeId}`);
  }

  submitLeave(req: Partial<LeaveRequest>): Observable<LeaveRequest> {
    return this.http.post<LeaveRequest>('/api/leaves', req);
  }

  // ── Bulletins ───────────────────────────────────────────
  getPayslips(employeeId: number): Observable<Payslip[]> {
    return this.http.get<Payslip[]>(`/api/payslips?employeeId=${employeeId}`);
  }

  getPayslip(id: number): Observable<Payslip> {
    return this.http.get<Payslip>(`/api/payslips/${id}`);
  }

  /** Renvoie l'URL de téléchargement PDF (ouverte directement par le navigateur). */
  payslipDownloadUrl(id: number): string {
    return `/api/payslips/${id}/download`;
  }
}
