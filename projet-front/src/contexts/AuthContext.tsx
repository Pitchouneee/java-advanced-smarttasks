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
  loginWithGoogle: (credential: string) => Promise<void>;
  logout: () => void;
  setTenant: (tenantId: string) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface GoogleIdTokenPayload {
  sub: string;
  email: string;
  name?: string;
  picture?: string;
}

function decodeGoogleCredential(credential: string): GoogleIdTokenPayload {
  const parts = credential.split('.');
  if (parts.length < 2) {
    throw new Error('Invalid Google token');
  }
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
  const payload = atob(padded);
  return JSON.parse(payload);
}

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

    const handleForcedLogout = () => {
      logout();
    };

    window.addEventListener('smarttasks:logout', handleForcedLogout);
    return () => {
      window.removeEventListener('smarttasks:logout', handleForcedLogout);
    };
  }, []);

  const loginWithGoogle = async (credential: string) => {
    try {
      const payload = decodeGoogleCredential(credential);
      const googleUser: User = {
        id: payload.sub,
        email: payload.email,
        name: payload.name ?? payload.email,
        picture: payload.picture,
        tenantId: 'tenant_default',
      };

      setUser(googleUser);
      setToken(credential);
      localStorage.setItem('smarttasks_user', JSON.stringify(googleUser));
      localStorage.setItem('smarttasks_token', credential);
    } catch (error) {
      console.error('Google login failed', error);
      throw new Error('Unable to validate Google login.');
    }
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
        loginWithGoogle,
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
