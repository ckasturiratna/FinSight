import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';
import { api } from '@/lib/api';
import ErrorDisplay from '@/components/ErrorDisplay';

const DebugForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<string | null>(null);
  const { toast } = useToast();

  const handleTest = async () => {
    setError(null);
    setResponse(null);
    
    if (!email) {
      setError("Please enter an email address.");
      return;
    }

    try {
      setIsLoading(true);
      console.log('🔍 Testing forgot password for email:', email);
      
      const result = await api.post('/api/users/forgot-password', { email });
      console.log('✅ Forgot password response:', result);
      
      setResponse(`Success: ${result}`);
      toast({
        title: "Test successful!",
        description: "Check console for details.",
        duration: 5000,
      });
    } catch (error: any) {
      console.error('❌ Forgot password error:', error);
      console.error('Error response:', error?.response);
      console.error('Error message:', error?.message);
      
      const errorMessage = error?.message || error?.response?.data || "Unknown error";
      setError(`Error: ${errorMessage}`);
      
      toast({
        variant: "destructive",
        title: "Test failed",
        description: errorMessage,
        duration: 10000,
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-2xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle>🔧 Debug Forgot Password</CardTitle>
            <CardDescription>
              Test the forgot password endpoint directly
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <ErrorDisplay 
              error={error} 
              onDismiss={() => setError(null)}
              duration={15000}
            />
            
            {response && (
              <div className="p-4 bg-green-100 border border-green-300 rounded-lg">
                <p className="text-green-800 font-medium">✅ Success Response:</p>
                <p className="text-green-700">{response}</p>
              </div>
            )}
            
            <div className="space-y-2">
              <Label htmlFor="email">Email Address</Label>
              <Input
                id="email"
                type="email"
                placeholder="Enter email to test"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <Button
              onClick={handleTest}
              disabled={isLoading}
              className="w-full"
            >
              {isLoading ? 'Testing...' : 'Test Forgot Password'}
            </Button>
            
            <div className="text-sm text-muted-foreground">
              <p><strong>Instructions:</strong></p>
              <ol className="list-decimal list-inside space-y-1 mt-2">
                <li>Open browser console (F12 → Console tab)</li>
                <li>Enter an email address that exists in your database</li>
                <li>Click "Test Forgot Password"</li>
                <li>Check console for detailed logs</li>
                <li>Check your email for the OTP</li>
              </ol>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default DebugForgotPassword;



