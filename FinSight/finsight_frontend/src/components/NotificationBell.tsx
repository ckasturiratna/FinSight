import { useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

import { Button } from '@/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { getNotifications, markAllNotificationsAsRead } from '@/services/notifications.api';
import { useAuth } from '@/contexts/AuthContext';
import { Notification } from '@/types/notification';

const NotificationBell = () => {
  const { token } = useAuth();
  const queryClient = useQueryClient();

  // React Query is now the single source of truth for notifications.
  const { data: notifications = [] } = useQuery<Notification[]>({
    queryKey: ['notifications'],
    queryFn: getNotifications,
    enabled: !!token, // Only run if the user is logged in
  });

  // Derive unread count directly from the query data.
  const unreadCount = notifications.filter((n) => !n.read).length;

  // Effect to handle WebSocket connection
  useEffect(() => {
    if (!token) return;

    const ws = new WebSocket(`ws://localhost:8080/ws/notifications`);

    ws.onopen = () => {
      console.log('Notification WebSocket connected');
    };

    ws.onmessage = (event) => {
      const newNotification = JSON.parse(event.data) as Notification;
      // Instead of using local state, we update the React Query cache directly.
      queryClient.setQueryData(['notifications'], (oldData: Notification[] | undefined) => {
        return [newNotification, ...(oldData || [])];
      });
    };

    ws.onclose = () => {
      console.log('Notification WebSocket disconnected');
    };

    ws.onerror = (error) => {
      console.error('Notification WebSocket error:', error);
    };

    // Cleanup on component unmount
    return () => {
      ws.close();
    };
    // We remove queryClient from dependencies as it's stable
  }, [token, queryClient]);

  // Mutation for marking all notifications as read
  const markAsReadMutation = useMutation({
    mutationFn: markAllNotificationsAsRead,
    onSuccess: () => {
      // Optimistically update the UI by modifying the cache
      queryClient.setQueryData(['notifications'], (oldData: Notification[] | undefined) => {
        return (oldData || []).map(n => ({ ...n, read: true }));
      });
      // Optionally, refetch to be perfectly in sync with the backend
      // queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
    onError: (error) => {
      console.error('Failed to mark notifications as read:', error);
    },
  });

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white">
              {unreadCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0">
        <div className="p-4 border-b">
          <h4 className="font-medium leading-none">Notifications</h4>
        </div>
        <div className="p-2 max-h-96 overflow-y-auto">
          {notifications.length > 0 ? (
            notifications.map((notification) => (
              <div key={notification.id} className={`p-2 rounded-lg ${!notification.read ? 'bg-blue-50' : ''}`}>
                <p className="text-sm text-gray-700">{notification.message}</p>
                <p className="text-xs text-gray-500 mt-1">
                  {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true })}
                </p>
              </div>
            ))
          ) : (
            <p className="text-sm text-center text-gray-500 py-8">No notifications yet.</p>
          )}
        </div>
        {notifications.length > 0 && unreadCount > 0 && (
          <div className="p-2 border-t">
            <Button
              variant="ghost"
              size="sm"
              className="w-full justify-center"
              onClick={() => markAsReadMutation.mutate()}
              disabled={markAsReadMutation.isPending}
            >
              <CheckCheck className="h-4 w-4 mr-2" /> Mark all as read
            </Button>
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
};

export default NotificationBell;
