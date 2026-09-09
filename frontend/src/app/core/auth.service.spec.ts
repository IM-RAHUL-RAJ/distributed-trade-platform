import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiService, AuthService } from './auth.service';
import { AuthUser } from './models';

describe('AuthService', () => {
  let service: AuthService;
  let http: jasmine.SpyObj<HttpClient>;

  const authResponse = {
    user: { id: 'u1', email: 'a@b.com', firstName: 'Ann', lastName: 'Bee', role: 'USER' } as AuthUser,
    accessToken: 'tok.access',
    refreshToken: 'tok.refresh',
  };

  beforeEach(() => {
    localStorage.clear();
    http = jasmine.createSpyObj('HttpClient', ['post', 'get']);
    TestBed.configureTestingModule({
      providers: [AuthService, { provide: HttpClient, useValue: http }],
    });
    service = TestBed.inject(AuthService);
  });

  it('posts login credentials to the auth endpoint', () => {
    http.post.and.returnValue(of(authResponse));
    let received: typeof authResponse | undefined;

    service.login('a@b.com', 'secret').subscribe((r) => (received = r));

    expect(http.post).toHaveBeenCalledWith('/api/v1/auth/login', {
      email: 'a@b.com',
      password: 'secret',
    });
    expect(received?.accessToken).toBe('tok.access');
  });

  it('posts register payload to the auth endpoint', () => {
    http.post.and.returnValue(of(authResponse));

    service.register('a@b.com', 'Ann', 'Bee', 'secret').subscribe();

    expect(http.post).toHaveBeenCalledWith('/api/v1/auth/register', {
      email: 'a@b.com',
      firstName: 'Ann',
      lastName: 'Bee',
      password: 'secret',
    });
  });

  it('persists the session and exposes the user', () => {
    service.saveSession(authResponse);

    expect(service.accessToken).toBe('tok.access');
    expect(service.refreshToken).toBe('tok.refresh');
    expect(service.isAuthenticated).toBeTrue();
    expect(service.user?.email).toBe('a@b.com');
  });

  it('clears the session on logout state', () => {
    service.saveSession(authResponse);
    service.clear();

    expect(service.isAuthenticated).toBeFalse();
    expect(service.user).toBeNull();
  });

  it('posts the stored refresh token when refreshing', () => {
    service.saveSession(authResponse);
    http.post.and.returnValue(of(authResponse));

    service.refresh(service.refreshToken!).subscribe();

    expect(http.post).toHaveBeenCalledWith('/api/v1/auth/refresh', {
      refreshToken: 'tok.refresh',
    });
  });
});

describe('ApiService', () => {
  let api: ApiService;
  let http: jasmine.SpyObj<HttpClient>;

  beforeEach(() => {
    http = jasmine.createSpyObj('HttpClient', ['get', 'post']);
    TestBed.configureTestingModule({
      providers: [ApiService, { provide: HttpClient, useValue: http }],
    });
    api = TestBed.inject(ApiService);
  });

  it('prefixes business routes with /api/v1', () => {
    http.get.and.returnValue(of([]));
    api.orders.list().subscribe();
    expect(http.get).toHaveBeenCalledWith('/api/v1/orders');
  });

  it('posts order creation to the business route', () => {
    http.post.and.returnValue(of({}));
    const body = { symbol: 'NVDA', side: 'BUY' as 'BUY', orderType: 'MARKET' as 'MARKET', quantity: 5 };
    api.orders.create(body).subscribe();
    expect(http.post).toHaveBeenCalledWith('/api/v1/orders', body);
  });

  it('posts cancel to the order cancel route', () => {
    http.post.and.returnValue(of({}));
    api.orders.cancel('ord-1').subscribe();
    expect(http.post).toHaveBeenCalledWith('/api/v1/orders/ord-1/cancel', {});
  });
});