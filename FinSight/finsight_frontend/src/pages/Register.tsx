import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { useForm } from '@/contexts/FormContext';
import { useToast } from '@/hooks/use-toast';
import { TrendingUp, User, Mail, Lock, Loader2 } from 'lucide-react';
import PoweredBy from '@/components/PoweredBy';
import BrandingPanel from '@/components/BrandingPanel';

const Register = () => {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { isLoading } = useAuth();
  const { setCurrentForm } = useForm();
  const { toast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  // Set form context and restore form data from location state if coming back from OTP verification
  useEffect(() => {
    setCurrentForm('register');
    const state = location.state as { formData?: typeof formData } | null;
    if (state?.formData) {
      setFormData(state.formData);
      toast({
        title: "Form data restored",
        description: "Your registration information has been restored.",
      });
      // Clear the location state to prevent showing toast again
      window.history.replaceState({}, document.title);
    }
  }, [location.state, toast, setCurrentForm]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Prevent multiple submissions
    if (isSubmitting) {
      return;
    }
    
    if (formData.password !== formData.confirmPassword) {
      toast({
        variant: "destructive",
        title: "Password mismatch",
        description: "Please ensure both passwords match.",
      });
      return;
    }

    if (formData.password.length < 6) {
      toast({
        variant: "destructive",
        title: "Password too short",
        description: "Password must be at least 6 characters long.",
      });
      return;
    }

    setIsSubmitting(true);
    
    try {
      const response = await fetch('http://localhost:8080/api/users/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email,
          password: formData.password,
        }),
      });

      if (!response.ok) {
        throw new Error('Registration failed');
      }

      // Show success message immediately
      toast({
        title: "Verification code sent!",
        description: "Please check your email and enter the verification code.",
      });

      // Small delay to ensure user sees the success message before navigation
      setTimeout(() => {
        navigate('/verify-otp', {
          state: {
            email: formData.email,
            firstName: formData.firstName,
            lastName: formData.lastName,
            password: formData.password,
          }
        });
      }, 500);
    } catch (error) {
      toast({
        variant: "destructive",
        title: "Registration failed",
        description: "Email may already be in use or server error occurred.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex">
      {/* Branding Panel - Desktop Only */}
      <BrandingPanel />
      
      {/* Register Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-4">
        <div className="w-full max-w-md">

        {/* Register Card */}
        <Card className="card-floating">
          <CardHeader className="space-y-2 pb-6">
            <CardTitle className="text-2xl font-light text-foreground">Create account</CardTitle>
            <CardDescription className="font-light">
              Get started with your financial dashboard
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName">First name</Label>
                  <div className="relative">
                     <User className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                     <Input
                       id="firstName"
                       name="firstName"
                       type="text"
                       placeholder="John"
                       value={formData.firstName}
                       onChange={handleChange}
                       className="pl-11"
                       required
                     />
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label htmlFor="lastName">Last name</Label>
                   <Input
                     id="lastName"
                     name="lastName"
                     type="text"
                     placeholder="Doe"
                     value={formData.lastName}
                     onChange={handleChange}
                     className=""
                     required
                   />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <div className="relative">
                   <Mail className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                   <Input
                     id="email"
                     name="email"
                     type="email"
                     placeholder="your@email.com"
                     value={formData.email}
                     onChange={handleChange}
                     className="pl-11"
                     required
                   />
                </div>
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <div className="relative">
                   <Lock className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                   <Input
                     id="password"
                     name="password"
                     type="password"
                     placeholder="••••••••"
                     value={formData.password}
                     onChange={handleChange}
                     className="pl-11"
                     required
                   />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm password</Label>
                <div className="relative">
                   <Lock className="absolute left-4 top-4 h-4 w-4 text-muted-foreground" />
                   <Input
                     id="confirmPassword"
                     name="confirmPassword"
                     type="password"
                     placeholder="••••••••"
                     value={formData.confirmPassword}
                     onChange={handleChange}
                     className="pl-11"
                     required
                   />
                </div>
              </div>

               <Button
                 type="submit"
                 className="w-full btn-primary-minimal font-light text-lg py-6"
                 disabled={isSubmitting || isLoading}
               >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Sending verification code...
                  </>
                ) : (
                  'Create account'
                )}
              </Button>
            </form>

            <div className="mt-8 text-center">
              <p className="text-sm text-muted-foreground font-light">
                Already have an account?{' '}
                <Link 
                  to="/" 
                  className="text-primary hover:text-primary-glow font-light transition-smooth"
                >
                  Sign in
                </Link>
              </p>
            </div>
          </CardContent>
        </Card>

        <PoweredBy />
        </div>
      </div>
    </div>
  );
};

export default Register;