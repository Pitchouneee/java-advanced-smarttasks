import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { CredentialResponse, GoogleLogin } from '@react-oauth/google';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function Login() {
  const { loginWithGoogle, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState<string | null>(null);
  const targetPath = (location.state as { from?: string })?.from ?? '/dashboard';
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  useEffect(() => {
    if (isAuthenticated) {
      navigate(targetPath, { replace: true });
    }
  }, [isAuthenticated, navigate, targetPath]);

  const handleSuccess = async (credentialResponse: CredentialResponse) => {
    setError(null);
    if (!credentialResponse.credential) {
      setError('Reponse Google invalide.');
      return;
    }
    try {
      await loginWithGoogle(credentialResponse.credential);
      navigate(targetPath, { replace: true });
    } catch {
      setError('Unable to complete the login. Please try again.');
    }
  };

  const handleError = () => {
    setError('Google login failed. Please try again.');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center space-y-2">
          <CardTitle className="text-3xl font-bold">SmartTasks</CardTitle>
          <CardDescription>Sign in with your Google account.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && (
            <div className="rounded-md border border-destructive/50 bg-destructive/10 px-3 py-2 text-sm text-destructive">
              {error}
            </div>
          )}
          {!googleClientId ? (
            <div className="rounded-md border border-yellow-500/40 bg-yellow-500/10 px-3 py-2 text-sm text-yellow-900">
              Set the VITE_GOOGLE_CLIENT_ID variable in your .env file to enable login.
            </div>
          ) : (
            <div className="flex justify-center">
              <GoogleLogin onSuccess={handleSuccess} onError={handleError} />
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
