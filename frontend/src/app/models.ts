// Modèles TypeScript alignés sur les DTO du backend RH.

export interface Employee {
  id: number;
  matricule: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  department: string;
  jobTitle: string;
  monthlyGrossSalary: number;
  leaveBalanceCp: number;
  leaveBalanceRtt: number;
}

export type LeaveType = 'CP' | 'RTT' | 'SANS_SOLDE';
export type LeaveStatus = 'EN_ATTENTE' | 'VALIDE' | 'REFUSE';

export interface LeaveRequest {
  id?: number;
  employeeId: number;
  type: LeaveType;
  startDate: string;   // yyyy-MM-dd
  endDate: string;
  comment?: string;
  workingDays: number;
  status: LeaveStatus;
  requestedAt?: string;
  decisionComment?: string;
}

export interface PayslipLine {
  label: string;
  base?: string;
  rate?: string;
  amount: number;
}

export interface Payslip {
  id: number;
  employeeId: number;
  year: number;
  month: number;
  period: string;
  grossSalary: number;
  totalContributions: number;
  netSalary: number;
  netBeforeTax: number;
  incomeTax: number;
  cumulativeGross: number;
  cumulativeNet: number;
  cumulativeTax: number;
  lines: PayslipLine[];
}
