import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getNotifications } from '@/services/notifications.api';
import { deleteAlert } from '@/services/alerts.api';
import { Notification } from '@/types/notification';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { History, ArrowLeft, Bell, CheckCircle, Trash2 } from 'lucide-react';
import AppHeader from '@/components/AppHeader';

// Transformed alert history type
export type AlertHistory = {
  id: number;
  alertId: number | null;
  ticker: string;
  condition: string;
  triggeredPrice: string;
  threshold: string;
  triggeredAt: string;
};

// Function to parse the notification message into a structured format
const parseNotificationMessage = (notification: Notification): AlertHistory | null => {
  const message = notification.message;
  const match = message.match(
    /Price Alert: (.*) is now (\$\d+\.\d{2}), triggering your alert for (.*) (\$\d+\.\d{2})\./
  );

  if (match) {
    const [, ticker, triggeredPrice, condition, threshold] = match;
    return {
      id: notification.id,
      alertId: notification.alertId || null,
      ticker,
      triggeredPrice,
      condition,
      threshold,
      triggeredAt: notification.createdAt,
    };
  }
  return null;
};

const AlertHistoryPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Query to fetch notifications and transform them into alert history
  const { data: alertHistory, isLoading, error } = useQuery<AlertHistory[]>({
    queryKey: ['alert-history'],
    queryFn: async () => {
      const notifications = await getNotifications();
      return notifications
        .map(parseNotificationMessage)
        .filter((item): item is AlertHistory => item !== null);
    },
    retry: 1,
  });

  // Mutation for deleting an alert
  const deleteMutation = useMutation({
    mutationFn: deleteAlert,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alert-history'] });
    },
  });

  const handleDelete = (alertId: number) => {
    if (window.confirm('Are you sure you want to delete this alert?')) {
      deleteMutation.mutate(alertId);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <Button 
              variant="outline" 
              size="sm" 
              onClick={() => navigate('/alerts')}
              className="flex items-center gap-2"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Alerts
            </Button>
            <h2 className="text-2xl font-bold text-foreground">Alert History</h2>
          </div>
        </div>

        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <History className="h-5 w-5 text-primary" />
              <span>Triggered Alerts</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="py-12 text-center text-muted-foreground">Loading alert history...</div>
            ) : error ? (
              <div className="py-12 text-center text-destructive">
                Failed to load alert history. Please try again later.
              </div>
            ) : alertHistory && alertHistory.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Ticker</TableHead>
                    <TableHead>Condition</TableHead>
                    <TableHead className="text-right">Threshold</TableHead>
                    <TableHead className="text-right">Triggered Price</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Triggered At</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {alertHistory.map((history) => (
                    <TableRow key={history.id}>
                      <TableCell className="font-medium">{history.ticker}</TableCell>
                      <TableCell>
                        Price {history.condition}
                      </TableCell>
                      <TableCell className="text-right">{history.threshold}</TableCell>
                      <TableCell className="text-right">{history.triggeredPrice}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <CheckCircle className="h-4 w-4 text-green-500" />
                          <span className="text-green-600">Triggered</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {new Date(history.triggeredAt).toLocaleString()}
                      </TableCell>
                      <TableCell>
                        {history.alertId && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDelete(history.alertId!)}
                            disabled={deleteMutation.isPending}
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <div className="py-12 text-center text-muted-foreground">
                <Bell className="h-12 w-12 mx-auto mb-4 text-muted-foreground/50" />
                <p className="text-lg font-medium mb-2">No Alert History</p>
                <p>You haven't triggered any alerts yet. Create some alerts to see their history here.</p>
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
};

export default AlertHistoryPage;
