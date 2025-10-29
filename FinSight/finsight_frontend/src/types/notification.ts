export interface Notification {
  id: number;
  message: string;
  isRead: boolean;
  createdAt: string;
  alertId?: number;
}
