export interface TicketSummary {
  ticketId: number;
  ticketNumber: string;
  groupToken?: string;
  seatNumber: string;
  gradeName: string;
  ticketPrice: number;
  isValid: boolean;
  createdAt: string;
}

export interface LegacyTicketSummary extends TicketSummary {
  valid?: boolean;
}

export interface TicketGroupInfo {
  scheduleId: number;
  concertName: string;
  posterUrl: string;
  startDate: string;
  endDate: string;
  round: number;
  totalPrice: number;
  tickets: TicketSummary[];
}

export interface MyPageData {
  name: string;
  id: string;
  email: string;
  loginType: string;
  profileImageUrl: string;
  ticketGroups: TicketGroupInfo[];
}
