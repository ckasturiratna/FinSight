import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';

const TestOtp = () => {
  const [email, setEmail] = useState('test@example.com');
  const [result, setResult] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const { toast } = useToast();

  const testRegister = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          firstName: 'Test',
          lastName: 'User',
          email: email,
          password: 'password123'
        }),
      });
      
      const data = await response.text();
      setResult({ type: 'register', data, status: response.status });
      toast({ title: 'Register Test', description: data });
    } catch (error) {
      setResult({ type: 'register', error: error.message });
    } finally {
      setIsLoading(false);
    }
  };

  const testCheckStatus = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/api/users/check-verification/${email}`);
      const data = await response.json();
      setResult({ type: 'check', data, status: response.status });
    } catch (error) {
      setResult({ type: 'check', error: error.message });
    } finally {
      setIsLoading(false);
    }
  };

  const testResend = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/resend-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
      
      const data = await response.text();
      setResult({ type: 'resend', data, status: response.status });
      toast({ title: 'Resend Test', description: data });
    } catch (error) {
      setResult({ type: 'resend', error: error.message });
    } finally {
      setIsLoading(false);
    }
  };

  const testVerify = async () => {
    const otp = prompt('Enter OTP:');
    if (!otp) return;
    
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/verify-otp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email,
          otp,
          firstName: 'Test',
          lastName: 'User',
          password: 'password123'
        }),
      });
      
      const data = response.ok ? await response.json() : await response.text();
      setResult({ type: 'verify', data, status: response.status });
      toast({ 
        title: 'Verify Test', 
        description: response.ok ? 'Success!' : data 
      });
    } catch (error) {
      setResult({ type: 'verify', error: error.message });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen p-8">
      <div className="max-w-2xl mx-auto">
        <Card>
          <CardHeader>
            <CardTitle>OTP Testing Panel</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-2">Email:</label>
              <Input
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="test@example.com"
              />
            </div>
            
            <div className="flex gap-2 flex-wrap">
              <Button onClick={testRegister} disabled={isLoading}>
                Test Register
              </Button>
              <Button onClick={testCheckStatus} disabled={isLoading}>
                Check Status
              </Button>
              <Button onClick={testResend} disabled={isLoading}>
                Resend OTP
              </Button>
              <Button onClick={testVerify} disabled={isLoading}>
                Verify OTP
              </Button>
            </div>

            {result && (
              <div className="mt-4 p-4 bg-gray-100 rounded">
                <h3 className="font-semibold mb-2">Result ({result.type}):</h3>
                <pre className="text-xs overflow-auto">
                  {JSON.stringify(result, null, 2)}
                </pre>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default TestOtp;


