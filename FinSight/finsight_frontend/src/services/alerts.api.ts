const API_BASE_URL = 'http://localhost:8080';

/**
 * Deletes an alert by its ID.
 */
export const deleteAlert = async (alertId: number): Promise<void> => {
  const token = localStorage.getItem('finsight_token');
  if (!token) {
    throw new Error('No authentication token found');
  }

  const response = await fetch(`${API_BASE_URL}/api/alerts/${alertId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to delete alert');
  }
};
