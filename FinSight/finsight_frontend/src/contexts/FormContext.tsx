import React, { createContext, useContext, useState, ReactNode } from 'react';

type FormType = 'login' | 'register';

interface FormContextType {
  currentForm: FormType;
  setCurrentForm: (form: FormType) => void;
}

const FormContext = createContext<FormContextType | undefined>(undefined);

export const useForm = () => {
  const context = useContext(FormContext);
  if (context === undefined) {
    throw new Error('useForm must be used within a FormProvider');
  }
  return context;
};

interface FormProviderProps {
  children: ReactNode;
}

export const FormProvider: React.FC<FormProviderProps> = ({ children }) => {
  const [currentForm, setCurrentForm] = useState<FormType>('login');

  return (
    <FormContext.Provider value={{ currentForm, setCurrentForm }}>
      {children}
    </FormContext.Provider>
  );
};
