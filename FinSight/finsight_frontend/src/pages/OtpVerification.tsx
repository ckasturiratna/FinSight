import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/contexts/AuthContext';
import { TrendingUp, Mail, ArrowLeft, RotateCcw } from 'lucide-react';
import { Link } from 'react-router-dom';
import PoweredBy from '@/components/PoweredBy';

interface LocationState {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}

const OtpVerification = () => {
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [isBlocked, setIsBlocked] = useState(false);
  const [remainingBlockMinutes, setRemainingBlockMinutes] = useState(0);
  const { toast } = useToast();
  const { loginWithToken } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  
  const state = location.state as LocationState;
  
  // Check verification status on load
  const checkVerificationStatus = async () => {
    if (!state?.email) return;

    try {
      const response = await fetch(`http://localhost:8080/api/users/check-verification/${state.email}`);
      if (response.ok) {
        const status = await response.json();
        if (status.isBlocked) {
          setIsBlocked(true);
          setRemainingBlockMinutes(status.remainingBlockMinutes || 0);
        } else {
          setIsBlocked(false);
          setRemainingBlockMinutes(0);
        }
      }
    } catch (error) {
      console.error('Failed to check verification status:', error);
    }
  };

  // Redirect if no registration data
  useEffect(() => {
    if (!state?.email || !state?.firstName || !state?.lastName || !state?.password) {
      toast({
        variant: "destructive",
        title: "Missing registration data",
        description: "Please complete registration first.",
      });
      navigate('/register');
    } else {
      checkVerificationStatus();
    }
  }, [state, navigate, toast]);

  // Countdown timer for resend
  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [countdown]);

  // Block countdown timer
  useEffect(() => {
    if (remainingBlockMinutes > 0) {
      const timer = setTimeout(() => {
        setRemainingBlockMinutes(prev => {
          if (prev <= 1) {
            setIsBlocked(false);
            // Refresh verification status
            checkVerificationStatus();
            return 0;
          }
          return prev - 1;
        });
      }, 60000); // Update every minute
      return () => clearTimeout(timer);
    }
  }, [remainingBlockMinutes]);

  const handleOtpChange = (index: number, value: string) => {
    if (value.length > 1) return; // Prevent multiple characters
    
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);

    // Auto-focus next input
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    const newOtp = pastedData.split('').concat(Array(6 - pastedData.length).fill(''));
    setOtp(newOtp);
    
    // Focus the last filled input
    const lastFilledIndex = Math.min(pastedData.length - 1, 5);
    inputRefs.current[lastFilledIndex]?.focus();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const otpString = otp.join('');
    if (otpString.length !== 6) {
      toast({
        variant: "destructive",
        title: "Invalid OTP",
        description: "Please enter all 6 digits.",
      });
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/verify-otp', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: state.email,
          otp: otpString,
          firstName: state.firstName,
          lastName: state.lastName,
          password: state.password,
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        const errorMessage = errorText || 'Invalid OTP';
        
        // Check if it's a blocked account error
        if (errorMessage.includes('blocked') || errorMessage.includes('too many')) {
          setIsBlocked(true);
          throw new Error('Account blocked due to too many failed attempts. Please click "Resend OTP" to get a new verification code.');
        }
        
        throw new Error(errorMessage);
      }

      const responseData = await response.json();
      
      // Extract user data and token from response
      const { user: userData, token } = responseData;
      
      toast({
        title: "Account created successfully!",
        description: "Welcome to FinSight! You are now logged in.",
      });
      
      // Automatically log in the user
      loginWithToken(token, userData);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Verification failed";
      toast({
        variant: "destructive",
        title: "Verification failed",
        description: errorMessage,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendOtp = async () => {
    setIsResending(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/resend-otp', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: state.email }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || 'Failed to resend OTP');
      }

      setCountdown(60); // 60 seconds countdown
      setIsBlocked(false); // Clear blocked state
      toast({
        title: "OTP resent",
        description: "A new verification code has been sent to your email.",
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "Failed to resend OTP";
      toast({
        variant: "destructive",
        title: "Failed to resend",
        description: errorMessage,
      });
    } finally {
      setIsResending(false);
    }
  };

  if (!state?.email) {
    return null;
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* OTP Verification Card */}
        <Card className="card-floating">
          <CardHeader className="space-y-2 pb-6">
            <CardTitle className="text-2xl font-light text-foreground">OTP Authentication</CardTitle>
            <CardDescription className="font-light">
              Enter the 6 digit OTP sent to your email
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-6">
              {/* Email Display */}
              <div className="text-center">
                <div className="flex items-center justify-center mb-2">
                  <Mail className="h-5 w-5 text-muted-foreground mr-2" />
                  <span className="text-sm text-muted-foreground">Sent to:</span>
                </div>
                <p className="font-medium text-foreground">{state.email}</p>
              </div>

              {/* OTP Input */}
              <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="otp">Verification Code</Label>
                <div className="flex justify-center space-x-2">
                  {otp.map((digit, index) => (
                    <Input
                      key={index}
                      ref={(el) => (inputRefs.current[index] = el)}
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]"
                      maxLength={1}
                      value={digit}
                      onChange={(e) => handleOtpChange(index, e.target.value)}
                      onKeyDown={(e) => handleKeyDown(index, e)}
                      onPaste={handlePaste}
                      className={`w-12 h-12 text-center text-lg font-semibold border-2 focus:border-primary ${
                        isBlocked ? 'border-red-500 bg-red-50' : ''
                      }`}
                      disabled={isLoading || isBlocked}
                    />
                  ))}
                </div>
                {isBlocked && (
                  <div className="text-center">
                    <p className="text-sm text-red-600">
                      Account blocked due to too many failed attempts.
                    </p>
                    {remainingBlockMinutes > 0 && (
                      <p className="text-xs text-red-500 mt-1">
                        Will be unblocked in {remainingBlockMinutes} minute{remainingBlockMinutes !== 1 ? 's' : ''}
                      </p>
                    )}
                    <p className="text-xs text-gray-600 mt-1">
                      Or click "Resend OTP" to unblock immediately
                    </p>
                  </div>
                )}
              </div>

                <Button
                  type="submit"
                  className="w-full btn-primary-minimal font-light text-lg py-6"
                  disabled={isLoading || otp.join('').length !== 6 || isBlocked}
                >
                  {isLoading ? 'Verifying...' : isBlocked ? 'Account Blocked' : 'Verify OTP'}
                </Button>
              </form>

              {/* Resend OTP */}
              <div className="text-center">
                <p className="text-sm text-muted-foreground mb-2">
                  Didn't receive the code?
                </p>
                <Button
                  variant={isBlocked ? "default" : "outline"}
                  size="sm"
                  onClick={handleResendOtp}
                  disabled={isResending || countdown > 0}
                  className={`w-full font-light ${isBlocked ? 'bg-red-600 hover:bg-red-700' : ''}`}
                >
                  <RotateCcw className="h-4 w-4 mr-2" />
                  {isResending 
                    ? 'Sending...' 
                    : countdown > 0 
                      ? `Resend in ${countdown}s` 
                      : isBlocked 
                        ? 'Resend OTP (Unblock Account)'
                        : 'Resend OTP'
                  }
                </Button>
              </div>


              {/* Back to Register */}
              <div className="text-center">
                <Link
                  to="/register"
                  state={{ 
                    formData: {
                      firstName: state.firstName,
                      lastName: state.lastName,
                      email: state.email,
                      password: state.password,
                      confirmPassword: state.password, // Use same password for confirmPassword
                    }
                  }}
                  className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors font-light"
                >
                  <ArrowLeft className="h-4 w-4 mr-1" />
                  Back to registration
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

export default OtpVerification;
