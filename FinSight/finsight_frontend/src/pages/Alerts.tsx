import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom'; // <-- I HAVE ADDED THIS LINE
import { api, Alert, CreateAlertRequest, Company } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { Plus, Trash2, BellRing, History } from 'lucide-react'; // <-- I HAVE ADDED HISTORY ICON
import AppHeader from '@/components/AppHeader';
import CompanyPicker from '@/components/CompanyPicker';

const Alerts = () => {
    const navigate = useNavigate(); // <-- I HAVE ADDED THIS LINE
    const [openCreate, setOpenCreate] = useState(false);
    const [form, setForm] = useState<CreateAlertRequest>({ ticker: '', conditionType: 'GT', threshold: 0 });
    const { toast } = useToast();
    const queryClient = useQueryClient();

    // Query to fetch the user's alerts
    const { data: alerts, isLoading } = useQuery<Alert[]>({
        queryKey: ['alerts'],
        queryFn: () => api.get('/api/alerts'),
    });

    // Mutation for creating a new alert
    const createMutation = useMutation({
        mutationFn: (newAlert: CreateAlertRequest) => api.post<Alert>('/api/alerts', newAlert),
        onSuccess: () => {
            toast({ title: 'Alert Created', description: 'You will be notified when the price target is hit.' });
            setOpenCreate(false);
            setForm({ ticker: '', conditionType: 'GT', threshold: 0 });
            queryClient.invalidateQueries({ queryKey: ['alerts'] });
        },
        onError: (e: any) => toast({ variant: 'destructive', title: 'Failed to create alert', description: e.message }),
    });

    // Mutation for deleting an alert
    const deleteMutation = useMutation({
        mutationFn: (alertId: number) => api.del(`/api/alerts/${alertId}`),
        onSuccess: () => {
            toast({ title: 'Alert Deleted' });
            queryClient.invalidateQueries({ queryKey: ['alerts'] });
        },
        onError: (e: any) => toast({ variant: 'destructive', title: 'Failed to delete alert', description: e.message }),
    });

    const handleCreate = () => {
        if (!form.ticker || form.threshold <= 0) {
            toast({ variant: 'destructive', title: 'Invalid Input', description: 'Please select a company and enter a valid threshold.' });
            return;
        }
        createMutation.mutate(form);
    };

    return (
        <div className="min-h-screen bg-background">
            <AppHeader />
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-4">
                        <h2 className="text-2xl font-bold text-foreground">Price Alerts</h2>
                        <Button variant="outline" size="sm" onClick={() => navigate('/alert-history')}>
                            <History className="h-4 w-4 mr-2" />
                            Alert History
                        </Button>
                    </div>
                    <Dialog open={openCreate} onOpenChange={setOpenCreate}>
                        <DialogTrigger asChild>
                            <Button className="gradient-primary text-primary-foreground">
                                <Plus className="h-4 w-4 mr-2" /> New Alert
                            </Button>
                        </DialogTrigger>
                        <DialogContent>
                            <DialogHeader>
                                <DialogTitle>Create a New Price Alert</DialogTitle>
                            </DialogHeader>
                            <div className="space-y-4 py-4">
                                <div className="space-y-2">
                                    <Label>Company</Label>
                                    <CompanyPicker
                                        value={form.ticker ? { ticker: form.ticker } as Company : null}
                                        onChange={(company) => setForm({ ...form, ticker: company.ticker })}
                                    />
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-2">
                                        <Label>Condition</Label>
                                        <Select value={form.conditionType} onValueChange={(value) => setForm({ ...form, conditionType: value as 'GT' | 'LT' })}>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="GT">Price is Greater Than (&gt;)</SelectItem>
                                                <SelectItem value="LT">Price is Less Than (&lt;)</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </div>
                                    <div className="space-y-2">
                                        <Label>Threshold</Label>
                                        <Input
                                            type="number"
                                            value={form.threshold}
                                            onChange={(e) => setForm({ ...form, threshold: parseFloat(e.target.value) || 0 })}
                                            placeholder="e.g., 150.00"
                                        />
                                    </div>
                                </div>
                            </div>
                            <DialogFooter>
                                <Button onClick={handleCreate} disabled={createMutation.isPending}>
                                    {createMutation.isPending ? 'Creating...' : 'Create Alert'}
                                </Button>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>
                </div>

                <Card className="shadow-card border-0">
                    <CardHeader>
                        <CardTitle className="flex items-center space-x-2">
                            <BellRing className="h-5 w-5 text-primary" />
                            <span>Your Active Alerts</span>
                        </CardTitle>
                    </CardHeader>
                    <CardContent>
                        {isLoading ? (
                            <div className="py-12 text-center text-muted-foreground">Loading alerts...</div>
                        ) : alerts && alerts.length > 0 ? (
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableHead>Ticker</TableHead>
                                        <TableHead>Condition</TableHead>
                                        <TableHead className="text-right">Threshold</TableHead>
                                        <TableHead>Created At</TableHead>
                                        <TableHead className="text-right">Actions</TableHead>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {alerts.map((alert) => (
                                        <TableRow key={alert.id}>
                                            <TableCell className="font-medium">{alert.ticker}</TableCell>
                                            <TableCell>
                                                Price {alert.conditionType === 'GT' ? '>' : '<'}
                                            </TableCell>
                                            <TableCell className="text-right">${alert.threshold.toFixed(2)}</TableCell>
                                            <TableCell>{new Date(alert.createdAt).toLocaleDateString()}</TableCell>
                                            <TableCell className="text-right">
                                                <Button
                                                    variant="outline"
                                                    size="sm"
                                                    className="text-destructive"
                                                    onClick={() => {
                                                        if (confirm(`Delete alert for ${alert.ticker}?`)) {
                                                            deleteMutation.mutate(alert.id);
                                                        }
                                                    }}
                                                    disabled={deleteMutation.isPending}
                                                >
                                                    <Trash2 className="h-3.5 w-3.5" />
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        ) : (
                            <div className="py-12 text-center text-muted-foreground">
                                You have no active alerts. Create one to get started.
                            </div>
                        )}
                    </CardContent>
                </Card>
            </main>
        </div>
    );
};

export default Alerts;
