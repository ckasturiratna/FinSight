import AppHeader from '@/components/AppHeader';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import { userApi, UpdateUserRequest } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible';
import { ChevronDown, Pencil, Save, X } from 'lucide-react';

export default function Profile() {
  const { user, logout } = useAuth();
  const { toast } = useToast();
  const [form, setForm] = useState<UpdateUserRequest>({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
  });
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [editing, setEditing] = useState(false);
  const [pw, setPw] = useState<{ otp: string; newPassword: string; confirmPassword: string }>({ otp: '', newPassword: '', confirmPassword: '' });
  const [sendingCode, setSendingCode] = useState(false);
  const [changingPw, setChangingPw] = useState(false);

  if (!user) return null;

  const onSave = async () => {
    if (!form.firstName.trim() || !form.lastName.trim()) {
      toast({ variant: 'destructive', title: 'First and last name are required' });
      return;
    }
    try {
      setSaving(true);
      const updated = await userApi.update(user.id, form);
      const storedUser = { ...user, firstName: updated.firstName, lastName: updated.lastName };
      localStorage.setItem('finsight_user', JSON.stringify(storedUser));
      toast({ title: 'Profile updated' });
      // Optimistically refresh page state
      window.dispatchEvent(new StorageEvent('storage', { key: 'finsight_user' } as any));
      setEditing(false);
    } catch (e: any) {
      toast({ variant: 'destructive', title: 'Update failed', description: e?.message || 'Unknown error' });
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async () => {
    if (!confirm('This will permanently delete your account. Continue?')) return;
    try {
      setDeleting(true);
      await userApi.delete(user.id);
      toast({ title: 'Account deleted' });
      logout();
    } catch (e: any) {
      toast({ variant: 'destructive', title: 'Delete failed', description: e?.message || 'Unknown error' });
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <AppHeader />
      <main className="max-w-2xl mx-auto p-6">
        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle>Profile</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label>Email</Label>
              <Input value={user.email} disabled />
            </div>

            {/* Profile info - read-only by default */}
            {!editing ? (
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>First name</Label>
                    <div className="h-10 px-3 flex items-center rounded-md border bg-muted/40 text-foreground">{form.firstName || '—'}</div>
                  </div>
                  <div className="space-y-1">
                    <Label>Last name</Label>
                    <div className="h-10 px-3 flex items-center rounded-md border bg-muted/40 text-foreground">{form.lastName || '—'}</div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Button onClick={() => setEditing(true)} className="inline-flex items-center gap-2">
                    <Pencil className="h-4 w-4" /> Edit profile
                  </Button>
                  <Button variant="outline" className="text-destructive border-destructive" onClick={onDelete} disabled={deleting}>
                    {deleting ? 'Deleting…' : 'Delete account'}
                  </Button>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>First name</Label>
                    <Input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                  </div>
                  <div className="space-y-2">
                    <Label>Last name</Label>
                    <Input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Button onClick={onSave} disabled={saving} className="inline-flex items-center gap-2">
                    <Save className="h-4 w-4" /> {saving ? 'Saving…' : 'Save changes'}
                  </Button>
                  <Button variant="outline" onClick={() => { setEditing(false); setForm({ firstName: user.firstName, lastName: user.lastName }); }} className="inline-flex items-center gap-2">
                    <X className="h-4 w-4" /> Cancel
                  </Button>
                </div>
              </div>
            )}

            {user.provider !== 'google' && (
              <>
                <div className="h-px bg-border my-4" />
                <Collapsible>
                  <div className="flex items-center justify-between">
                    <h3 className="text-lg">Change Password</h3>
                    <CollapsibleTrigger asChild>
                      <Button variant="outline" className="inline-flex items-center gap-2">
                        <ChevronDown className="h-4 w-4" /> Toggle
                      </Button>
                    </CollapsibleTrigger>
                  </div>
                  <CollapsibleContent className="space-y-4 mt-4">
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div className="space-y-2 md:col-span-2">
                        <Label>New password</Label>
                        <Input type="password" value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
                      </div>
                      <div className="space-y-2 md:col-span-1">
                        <Label>OTP</Label>
                        <div className="flex gap-2">
                          <Input value={pw.otp} onChange={(e) => setPw({ ...pw, otp: e.target.value })} />
                          <Button type="button" variant="outline" onClick={async () => {
                            try {
                              setSendingCode(true);
                              await userApi.initiatePasswordChange();
                              toast({ title: 'Verification code sent' });
                            } catch (e: any) {
                              toast({ variant: 'destructive', title: 'Failed to send code', description: e?.message || 'Unknown error' });
                            } finally {
                              setSendingCode(false);
                            }
                          }} disabled={sendingCode}>
                            {sendingCode ? 'Sending…' : 'Send code'}
                          </Button>
                        </div>
                      </div>
                      <div className="space-y-2 md:col-span-2">
                        <Label>Confirm new password</Label>
                        <Input type="password" value={pw.confirmPassword} onChange={(e) => setPw({ ...pw, confirmPassword: e.target.value })} />
                      </div>
                    </div>
                    <div>
                      <Button onClick={async () => {
                        if (!pw.newPassword || !pw.confirmPassword || !pw.otp) {
                          toast({ variant: 'destructive', title: 'Fill password and OTP' });
                          return;
                        }
                        try {
                          setChangingPw(true);
                          await userApi.confirmPasswordChange({ otp: pw.otp, newPassword: pw.newPassword, confirmPassword: pw.confirmPassword });
                          setPw({ otp: '', newPassword: '', confirmPassword: '' });
                          toast({ title: 'Password changed' });
                        } catch (e: any) {
                          toast({ variant: 'destructive', title: 'Change failed', description: e?.message || 'Unknown error' });
                        } finally {
                          setChangingPw(false);
                        }
                      }} disabled={changingPw}>
                        {changingPw ? 'Changing…' : 'Change password'}
                      </Button>
                    </div>
                  </CollapsibleContent>
                </Collapsible>
              </>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}


