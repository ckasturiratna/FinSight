import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  provider?: string;
  createdAt: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  loginWithToken: (token: string, userData: User) => void;
  register: (userData: RegisterData) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

interface RegisterData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  // Check for stored token on initialization
  useEffect(() => {
    const storedToken = localStorage.getItem('finsight_token');
    const storedUser = localStorage.getItem('finsight_user');
    // Validate token expiry on boot; logout if expired
    if (storedToken && storedUser) {
      let expired = false;
      try {
        const payload = JSON.parse(atob(storedToken.split('.')[1] || '')) || {};
        if (payload?.exp && typeof payload.exp === 'number') {
          expired = payload.exp * 1000 < Date.now();
        }
        // Ensure provider and userId are synced from token payload even if old localStorage lacks them
        if (!expired) {
          const parsed = JSON.parse(storedUser);
          const merged = {
            ...parsed,
            id: typeof payload.userId === 'number' ? payload.userId : parsed.id,
            provider: payload.provider || parsed.provider,
          };
          localStorage.setItem('finsight_user', JSON.stringify(merged));
        }
      } catch {
        // if decode fails, treat as invalid
        expired = true;
      }
      if (!expired) {
        setToken(storedToken);
        setUser(JSON.parse(localStorage.getItem('finsight_user') || storedUser));
      } else {
        localStorage.removeItem('finsight_token');
        localStorage.removeItem('finsight_user');
      }
    }

    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: email.trim(), password: password.trim() }),
      });

      if (!response.ok) {
        throw new Error('Invalid credentials');
      }

      const token = await response.text();
      // Decode JWT to get user info (basic decoding - in production use a proper JWT library)
      const payload = JSON.parse(atob(token.split('.')[1]));
      const userData = {
        id: payload.userId || 0,
        firstName: payload.firstName || '',
        lastName: payload.lastName || '',
        email: payload.sub || email,
        role: payload.role || 'USER',
        provider: payload.provider || undefined,
        createdAt: new Date().toISOString(),
      };

      setToken(token);
      setUser(userData);
      
      localStorage.setItem('finsight_token', token);
      localStorage.setItem('finsight_user', JSON.stringify(userData));
      
      navigate('/dashboard');
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const loginWithToken = (token: string, userData: User) => {
    setToken(token);
    setUser(userData);
    
    localStorage.setItem('finsight_token', token);
    localStorage.setItem('finsight_user', JSON.stringify(userData));
    
    navigate('/dashboard');
  };

  const register = async (userData: RegisterData) => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/users/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(userData),
      });

      if (!response.ok) {
        throw new Error('Registration failed');
      }

      const newUser = await response.json();
      
      // Auto-login after registration
      await login(userData.email, userData.password);
    } catch (error) {
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('finsight_token');
    localStorage.removeItem('finsight_user');
    navigate('/login');
  };

  const value: AuthContextType = {
    user,
    token,
    login,
    loginWithToken,
    register,
    logout,
    isLoading,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
