import { useState, useEffect } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { X } from 'lucide-react';

interface ErrorDisplayProps {
  error: string | null;
  onDismiss?: () => void;
  duration?: number;
}

const ErrorDisplay = ({ error, onDismiss, duration = 10000 }: ErrorDisplayProps) => {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    if (error) {
      setIsVisible(true);
      if (duration > 0) {
        const timer = setTimeout(() => {
          setIsVisible(false);
          if (onDismiss) onDismiss();
        }, duration);
        return () => clearTimeout(timer);
      }
    } else {
      setIsVisible(false);
    }
  }, [error, duration, onDismiss]);

  if (!isVisible || !error) return null;

  return (
    <Alert variant="destructive" className="mb-4">
      <AlertDescription className="flex items-center justify-between">
        <span>{error}</span>
        {onDismiss && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setIsVisible(false);
              onDismiss();
            }}
            className="h-auto p-1 text-destructive-foreground hover:text-destructive-foreground/80"
          >
            <X className="h-4 w-4" />
          </Button>
        )}
      </AlertDescription>
    </Alert>
  );
};

export default ErrorDisplay;



