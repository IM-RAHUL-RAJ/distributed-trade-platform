import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const cloneWithToken = () => {
    const token = auth.accessToken;
    if (!token) {
      return req;
    }
    return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  };

  return next(cloneWithToken()).pipe(
    catchError((err) => {
      if (err.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/refresh')) {
        const rt = auth.refreshToken;
        if (rt) {
          return auth.refresh(rt).pipe(
            switchMap((res) => {
              auth.saveSession(res);
              return next(req.clone({ setHeaders: { Authorization: `Bearer ${res.accessToken}` } }));
            }),
            catchError((refreshErr) => {
              auth.clear();
              router.navigate(['/login']);
              return throwError(() => refreshErr);
            }),
          );
        }
        auth.clear();
        router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};