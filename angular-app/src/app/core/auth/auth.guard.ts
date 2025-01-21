// app/core/guards/auth.guard.ts
import {Injectable, Input} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {AuthService} from './auth.service';
import {map, Observable} from 'rxjs';

@Injectable({providedIn: 'root'})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {
  }

  canActivate(next: ActivatedRouteSnapshot): Observable<boolean> {
    const requiredRole: string[] = Array.isArray(next.data['role']) ? next.data['role'] : [];
    const path: string[] = next.data['path'];
    let hasAcess: boolean = false;

    if (requiredRole.length > 1) {
      const roles: string[] = this.authService.user?.getRoles() || [];
      hasAcess = roles.some(role => requiredRole.includes(role));
    }


    return this.authService.isLoggedIn$.pipe(
      map(isLoggedIn => {
        if (isLoggedIn) {

          if (requiredRole.length > 1) {
            if (hasAcess) {
              return true;
            } else {
              this.router.navigate(['/acesso-negado', path]);
              return false;
            }
          }

          return true;

        } else {
          this.router.navigate(['/auth/login']);
          return false;
        }
      })
    );
  }
}
