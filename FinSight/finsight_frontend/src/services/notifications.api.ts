import { Notification } from '@/types/notification';

const API_BASE_URL = 'http://localhost:8080';

/**
 * Fetches all notifications for the currently authenticated user.
 */
export const getNotifications = async (): Promise<Notification[]> => {
  const token = localStorage.getItem('finsight_token');
  if (!token) {
    console.error('No authentication token found');
    return []; // Return empty array if not authenticated
  }

  const response = await fetch(`${API_BASE_URL}/api/notifications`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to fetch notifications');
  }

  return response.json();
};

/**
 * Marks all of the user's notifications as read.
 */
export const markAllNotificationsAsRead = async (): Promise<void> => {
  const token = localStorage.getItem('finsight_token');
  if (!token) {
    console.error('No authentication token found');
    return; // Do nothing if not authenticated
  }

  const response = await fetch(`${API_BASE_URL}/api/notifications/mark-all-as-read`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to mark notifications as read');
  }
};
