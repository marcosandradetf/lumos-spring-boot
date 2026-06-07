import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './useAuth';

interface ProtectedRouteProps {
  children: ReactNode;
  roles?: string[];
}

export const ProtectedRoute = ({ children, roles = [] }: ProtectedRouteProps) => {
  const { user, isLoggedIn, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex h-full items-center justify-center">
        <i className="pi pi-spin pi-spinner text-xl text-indigo-500" />
      </div>
    );
  }

  if (!isLoggedIn) {
    return <Navigate to="/auth/login" state={{ from: location }} replace />;
  }

  if (roles.length > 0) {
    const userRoles: string[] = user?.roles ?? [];
    const hasAccess = roles.some(role => userRoles.includes(role));
    if (!hasAccess) {
      return <Navigate to="/status/403" replace />;
    }
  }

  return <>{children}</>;
};
