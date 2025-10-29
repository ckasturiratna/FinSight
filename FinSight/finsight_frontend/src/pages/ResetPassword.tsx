import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { TrendingUp, Lock, Mail, ArrowLeft, Eye, EyeOff } from 'lucide-react';
import { api } from '@/lib/api';
import ErrorDisplay from '@/components/ErrorDisplay';
import PoweredBy from '@/components/PoweredBy';

const ResetPassword = () => {
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  // Get email from navigation state
  useEffect(() => {
    if (location.state?.email) {
      setEmail(location.state.email);
    }
  }, [location.state]);

  const handleResendOtp = async () => {
    setError(null); // Clear previous errors
    
    if (!email) {
      setError("Please enter your email address first.");
      return;
    }

    try {
      setIsLoading(true);
      console.log('Resending OTP for email:', email);
      
      const response = await api.post('/api/users/forgot-password', { email });
      console.log('Resend OTP response:', response);
      
      toast({
        title: "Reset code resent!",
        description: "Please check your email for the new password reset code.",
        duration: 5000,
      });
    } catch (error: any) {
      console.error('Resend OTP error:', error);
      console.error('Error response:', error?.response);
      console.error('Error message:', error?.message);
      
      const errorMessage = error?.message || error?.response?.data || "Please try again later.";
      setError(errorMessage);
      
      toast({
        variant: "destructive",
        title: "Failed to resend code",
        description: errorMessage,
        duration: 10000,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null); // Clear previous errors
    
    // Validation
    if (!email || !otp || !newPassword || !confirmPassword) {
      setError("Please fill in all fields.");
      return;
    }

    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters long.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords don't match. Please make sure both passwords are the same.");
      return;
    }

    try {
      setIsLoading(true);
      console.log('Resetting password for email:', email, 'OTP:', otp);
      
      const response = await api.post('/api/users/reset-password', {
        email,
        otp,
        newPassword,
        confirmPassword,
      });
      console.log('Reset password response:', response);
      
      toast({
        title: "Password reset successfully!",
        description: "You can now login with your new password.",
        duration: 5000,
      });
      
      // Navigate to login page
      navigate('/login');
    } catch (error: any) {
      console.error('Reset password error:', error);
      console.error('Error response:', error?.response);
      console.error('Error message:', error?.message);
      
      const errorMessage = error?.message || error?.response?.data || "Please try again later.";
      setError(errorMessage);
      
      toast({
        variant: "destructive",
        title: "Failed to reset password",
        description: errorMessage,
        duration: 10000,
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Reset Password Card */}
        <Card className="card-floating">
          <CardHeader className="space-y-2 pb-6">
            <CardTitle className="text-2xl font-light text-foreground">Reset Password</CardTitle>
            <CardDescription className="font-light">
              Enter the code from your email and create a new password
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ErrorDisplay 
              error={error} 
              onDismiss={() => setError(null)}
              duration={15000}
            />
            
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email Address</Label>
                <div className="relative">
                  <Mail className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                  <div className="flex items-center h-10 px-3 pl-11 border border-input bg-muted rounded-md text-sm">
                    {email || "No email provided"}
                  </div>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="otp">Verification Code</Label>
                <Input
                  id="otp"
                  type="text"
                  placeholder="Enter 6-digit code"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxLength={6}
                  className="text-center text-lg tracking-widest"
                  required
                />
                <p className="text-xs text-muted-foreground">
                  Check your email for the verification code
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="newPassword">New Password</Label>
                <div className="relative">
                  <Lock className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="newPassword"
                    type={showPassword ? "text" : "password"}
                    placeholder="••••••••"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="pl-11 pr-11"
                    required
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm New Password</Label>
                <div className="relative">
                  <Lock className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="confirmPassword"
                    type={showConfirmPassword ? "text" : "password"}
                    placeholder="••••••••"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="pl-11 pr-11"
                    required
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>

              <Button
                type="submit"
                className="w-full btn-primary-minimal font-light text-lg py-6"
                disabled={isLoading}
              >
                {isLoading ? 'Resetting...' : 'Reset Password'}
              </Button>
            </form>

            <div className="mt-6 text-center space-y-3">
              <div>
                <Button
                  type="button"
                  variant="link"
                  onClick={handleResendOtp}
                  disabled={isLoading}
                  className="text-sm text-muted-foreground hover:text-primary transition-smooth p-0 h-auto font-light"
                >
                  Didn't receive the code? Resend
                </Button>
              </div>
              <div>
                <Link 
                  to="/login" 
                  className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-smooth font-light"
                >
                  <ArrowLeft className="h-4 w-4 mr-1" />
                  Back to Login
                </Link>
              </div>
            </div>
          </CardContent>
        </Card>

        <PoweredBy />
      </div>
    </div>
  );
};

export default ResetPassword;
