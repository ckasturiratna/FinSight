import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { TrendingUp, Mail, ArrowLeft } from 'lucide-react';
import { api } from '@/lib/api';
import ErrorDisplay from '@/components/ErrorDisplay';
import PoweredBy from '@/components/PoweredBy';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null); // Clear previous errors
    
    if (!email) {
      setError("Please enter your email address.");
      return;
    }

    try {
      setIsLoading(true);
      console.log('Sending forgot password request for email:', email);
      
      const response = await api.post('/api/users/forgot-password', { email });
      console.log('Forgot password response:', response);
      
      toast({
        title: "Reset code sent!",
        description: "Please check your email for the password reset code.",
        duration: 5000,
      });
      
      // Navigate to reset password page with email
      navigate('/reset-password', { state: { email } });
    } catch (error: any) {
      console.error('Forgot password error:', error);
      console.error('Error response:', error?.response);
      console.error('Error message:', error?.message);
      
      const errorMessage = error?.message || error?.response?.data || "Please try again later.";
      setError(errorMessage);
      
      toast({
        variant: "destructive",
        title: "Failed to send reset code",
        description: errorMessage,
        duration: 10000, // 10 seconds for errors
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Forgot Password Card */}
        <Card className="card-floating">
          <CardHeader className="space-y-2 pb-6">
            <CardTitle className="text-2xl font-light text-foreground">Forgot Password?</CardTitle>
            <CardDescription className="font-light">
              Enter your email address and we'll send you a code to reset your password
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
                  <Input
                    id="email"
                    type="email"
                    placeholder="your@email.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="pl-11"
                    required
                  />
                </div>
              </div>

              <Button
                type="submit"
                className="w-full btn-primary-minimal font-light text-lg py-6"
                disabled={isLoading}
              >
                {isLoading ? 'Sending...' : 'Send Reset Code'}
              </Button>
            </form>

            <div className="mt-6 text-center">
              <Link 
                to="/login" 
                className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-smooth font-light"
              >
                <ArrowLeft className="h-4 w-4 mr-1" />
                Back to Login
              </Link>
            </div>
          </CardContent>
        </Card>

        <PoweredBy />
      </div>
    </div>
  );
};

export default ForgotPassword;
