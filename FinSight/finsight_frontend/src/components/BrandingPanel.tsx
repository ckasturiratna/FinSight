import { useForm } from '@/contexts/FormContext';
import { useEffect, useState } from 'react';

const BrandingPanel = () => {
  const { currentForm } = useForm();
  const [rotation, setRotation] = useState(0);
  
  useEffect(() => {
    // Force a re-render by briefly setting to a different value, then to target
    setRotation(currentForm === 'register' ? 0.1 : -0.1);
    
    const timer = setTimeout(() => {
      setRotation(currentForm === 'register' ? 180 : 0);
    }, 10);
    
    return () => clearTimeout(timer);
  }, [currentForm]);
  
  return (
    <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
      {/* Hero Image Background with rotation */}
      <div 
        className="absolute inset-0 bg-cover bg-center bg-no-repeat transition-transform duration-700 ease-in-out"
        style={{
          backgroundImage: 'url(/Hero.jpg)',
          transform: `rotate(${rotation}deg)`,
          transformOrigin: 'center center'
        }}
      />
      
      {/* Optional overlay for better contrast if needed */}
      <div className="absolute inset-0 bg-black/5"></div>
    </div>
  );
};

export default BrandingPanel;
