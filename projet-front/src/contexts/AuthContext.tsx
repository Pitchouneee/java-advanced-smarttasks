import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface User {
  id: string;
  email: string;
  name: string;
  picture?: string;
  tenantId: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, name: string) => Promise<void>;
  logout: () => void;
  setTenant: (tenantId: string) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    const storedUser = localStorage.getItem('smarttasks_user');
    const storedToken = localStorage.getItem('smarttasks_token');
    if (storedUser && storedToken) {
      setUser(JSON.parse(storedUser));
      setToken(storedToken);
    }
  }, []);

  const login = async (email: string, name: string) => {
    const mockUser: User = {
      id: `user_${Date.now()}`,
      email,
      name,
      picture: `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}`,
      tenantId: 'tenant_default',
    };
    const mockToken = `mock_jwt_${Date.now()}`;
    
    setUser(mockUser);
    setToken(mockToken);
    localStorage.setItem('smarttasks_user', JSON.stringify(mockUser));
    localStorage.setItem('smarttasks_token', mockToken);
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('smarttasks_user');
    localStorage.removeItem('smarttasks_token');
  };

  const setTenant = (tenantId: string) => {
    if (user) {
      const updatedUser = { ...user, tenantId };
      setUser(updatedUser);
      localStorage.setItem('smarttasks_user', JSON.stringify(updatedUser));
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user && !!token,
        login,
        logout,
        setTenant,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
