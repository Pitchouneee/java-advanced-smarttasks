import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';

export default function Profile() {
  const { user, token } = useAuth();

  if (!user) {
    return <div className="text-center py-12">Not authenticated</div>;
  }

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-3xl font-bold">Profile</h1>
        <p className="text-muted-foreground">Your account information</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Personal information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-4">
            <Avatar className="h-16 w-16">
              <AvatarImage src={user.picture} alt={user.name} />
              <AvatarFallback>{user.name.substring(0, 2).toUpperCase()}</AvatarFallback>
            </Avatar>
            <div>
              <p className="font-medium text-lg">{user.name}</p>
              <p className="text-muted-foreground">{user.email}</p>
            </div>
          </div>

          <div className="border-t pt-4 space-y-2">
            <div>
              <p className="text-sm font-medium">User ID</p>
              <p className="text-sm text-muted-foreground">{user.id}</p>
            </div>
            <div>
              <p className="text-sm font-medium">Current tenant</p>
              <p className="text-sm text-muted-foreground">{user.tenantId}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Authentication token</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="bg-muted p-3 rounded-md overflow-x-auto">
            <code className="text-sm">{token}</code>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
