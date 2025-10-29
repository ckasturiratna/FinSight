import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Loader2 } from 'lucide-react';

const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const { toast } = useToast();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      const token = searchParams.get('token');
      
      if (!token) {
        toast({
          variant: "destructive",
          title: "Authentication failed",
          description: "No authentication token received from Google.",
        });
        navigate('/login');
        return;
      }

      try {
        // Decode JWT to get user info
        const payload = JSON.parse(atob(token.split('.')[1]));
        const userData = {
          id: payload.userId || 0,
          firstName: payload.firstName || '',
          lastName: payload.lastName || '',
          email: payload.sub || '',
          role: payload.role || 'USER',
          provider: payload.provider || 'google',
          createdAt: new Date().toISOString(),
        };

        // Login with the token
        loginWithToken(token, userData);
        
        toast({
          title: "Welcome to FinSight!",
          description: "You've successfully signed in with Google.",
        });
      } catch (error) {
        console.error('OAuth callback error:', error);
        toast({
          variant: "destructive",
          title: "Authentication failed",
          description: "Failed to process Google authentication. Please try again.",
        });
        navigate('/login');
      }
    };

    handleOAuthCallback();
  }, [searchParams, navigate, loginWithToken, toast]);

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl font-light">Completing sign in...</CardTitle>
        </CardHeader>
        <CardContent className="text-center">
          <div className="flex flex-col items-center space-y-4">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
            <p className="text-muted-foreground">
              Please wait while we complete your Google sign-in.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default OAuthCallback;

