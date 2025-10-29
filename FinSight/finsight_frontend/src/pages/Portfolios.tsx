import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, Portfolio } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { useToast } from '@/hooks/use-toast';
import { Plus, Pencil, Trash2, Eye } from 'lucide-react';
import AppHeader from '@/components/AppHeader';

type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number; size: number };

const Portfolios = () => {
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [openCreate, setOpenCreate] = useState(false);
  const [openEditId, setOpenEditId] = useState<number | null>(null);
  const [form, setForm] = useState({ name: '', description: '' });
  const { toast } = useToast();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery<Page<Portfolio>>({
    queryKey: ['portfolios', page, size],
    queryFn: () => api.get(`/api/portfolios?page=${page}&size=${size}`),
  });

  const createMutation = useMutation({
    mutationFn: () => api.post<Portfolio>('/api/portfolios', form),
    onSuccess: () => {
      toast({ title: 'Portfolio created' });
      setOpenCreate(false);
      setForm({ name: '', description: '' });
      qc.invalidateQueries({ queryKey: ['portfolios'] });
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Create failed', description: e.message }),
  });

  const editMutation = useMutation({
    mutationFn: ({ id, body }: { id: number; body: { name: string; description: string } }) => api.put(`/api/portfolios/${id}`, body),
    onSuccess: () => {
      toast({ title: 'Portfolio updated' });
      setOpenEditId(null);
      qc.invalidateQueries({ queryKey: ['portfolios'] });
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Update failed', description: e.message }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.del(`/api/portfolios/${id}`),
    onSuccess: () => {
      toast({ title: 'Portfolio deleted' });
      qc.invalidateQueries({ queryKey: ['portfolios'] });
    },
    onError: (e: any) => toast({ variant: 'destructive', title: 'Delete failed', description: e.message }),
  });

  const current = data?.content || [];

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-foreground">Portfolios</h2>
          <Dialog open={openCreate} onOpenChange={setOpenCreate}>
            <DialogTrigger asChild>
              <Button className="gradient-primary text-primary-foreground"><Plus className="h-4 w-4 mr-2"/> New Portfolio</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create Portfolio</DialogTitle>
              </DialogHeader>
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label>Name</Label>
                  <Input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="My Portfolio" />
                </div>
                <div className="space-y-2">
                  <Label>Description</Label>
                  <Input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Optional" />
                </div>
              </div>
              <DialogFooter>
                <Button onClick={() => createMutation.mutate()} disabled={!form.name || createMutation.isPending}>Create</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle>Your portfolios</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="py-12 text-center text-muted-foreground">Loading...</div>
            ) : current.length === 0 ? (
              <div className="py-12 text-center text-muted-foreground">No portfolios yet. Create your first one.</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {current.map((p) => (
                    <TableRow key={p.id}>
                      <TableCell className="font-medium">{p.name}</TableCell>
                      <TableCell>{p.description}</TableCell>
                      <TableCell>{new Date(p.createdAt).toLocaleString()}</TableCell>
                      <TableCell className="text-right space-x-2">
                        <Button variant="outline" size="sm" onClick={() => navigate(`/portfolios/${p.id}`)}>
                          <Eye className="h-3.5 w-3.5 mr-1"/> View
                        </Button>
                        <Dialog open={openEditId === p.id} onOpenChange={(o) => {
                          setOpenEditId(o ? p.id : null);
                          if (o) setForm({ name: p.name, description: p.description || '' });
                        }}>
                          <DialogTrigger asChild>
                            <Button variant="outline" size="sm"><Pencil className="h-3.5 w-3.5 mr-1"/> Edit</Button>
                          </DialogTrigger>
                          <DialogContent>
                            <DialogHeader>
                              <DialogTitle>Edit Portfolio</DialogTitle>
                            </DialogHeader>
                            <div className="space-y-4">
                              <div className="space-y-2">
                                <Label>Name</Label>
                                <Input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
                              </div>
                              <div className="space-y-2">
                                <Label>Description</Label>
                                <Input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
                              </div>
                            </div>
                            <DialogFooter>
                              <Button onClick={() => editMutation.mutate({ id: p.id, body: { name: form.name, description: form.description } })} disabled={!form.name || editMutation.isPending}>Save</Button>
                            </DialogFooter>
                          </DialogContent>
                        </Dialog>
                        <Button variant="outline" size="sm" className="text-destructive" onClick={() => {
                          if (confirm(`Delete portfolio "${p.name}"?`)) deleteMutation.mutate(p.id)
                        }}>
                          <Trash2 className="h-3.5 w-3.5 mr-1"/> Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}

            <div className="flex items-center justify-between mt-4">
              <div className="text-sm text-muted-foreground">Page {page + 1} of {data?.totalPages || 1}</div>
              <div className="space-x-2">
                <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(p => Math.max(0, p - 1))}>Previous</Button>
                <Button variant="outline" size="sm" disabled={(data?.totalPages || 1) <= page + 1} onClick={() => setPage(p => p + 1)}>Next</Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </main>
    </div>
  );
};

export default Portfolios;
