import { Navigate, Outlet, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { NavLink } from '@/components/NavLink';

export function AppLayout() {
  const { user, isAuthenticated, logout } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b bg-background sticky top-0 z-10">
        <div className="container mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/dashboard" className="text-xl font-bold">
              SmartTasks
            </Link>
            <nav className="hidden md:flex gap-4">
              <NavLink
                to="/dashboard"
                className="text-sm hover:text-foreground transition-colors"
                activeClassName="text-foreground font-medium"
              >
                Dashboard
              </NavLink>
              <NavLink
                to="/projects"
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                activeClassName="text-foreground font-medium"
              >
                Projets
              </NavLink>
              <NavLink
                to="/profile"
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
                activeClassName="text-foreground font-medium"
              >
                Profil
              </NavLink>
            </nav>
          </div>
          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-3">
              <Avatar className="h-8 w-8">
                <AvatarImage src={user?.picture} alt={user?.name} />
                <AvatarFallback>{user?.name.substring(0, 2).toUpperCase()}</AvatarFallback>
              </Avatar>
              <div className="text-sm">
                <p className="font-medium">{user?.name}</p>
                <p className="text-muted-foreground text-xs">{user?.email}</p>
              </div>
            </div>
            <Button variant="outline" size="sm" onClick={logout}>
              Déconnexion
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1 container mx-auto px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
