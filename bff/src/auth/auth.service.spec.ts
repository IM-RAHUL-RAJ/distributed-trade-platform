import { ConflictException, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { Test } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import { AuthService } from './auth.service';
import { RefreshToken } from './refresh-token.entity';
import { User } from './user.entity';

describe('AuthService', () => {
  let service: AuthService;
  let users: jest.Mocked<Pick<Repository<User>, 'findOne' | 'save'>>;
  let refreshTokens: jest.Mocked<Pick<Repository<RefreshToken>, 'findOne' | 'save'>>;
  let jwtService: { sign: jest.Mock };

  const plainUser = (): User =>
    Object.assign(new User(), {
      id: 'u-1',
      email: 'a@b.com',
      firstName: 'Ann',
      lastName: 'Bee',
      passwordHash: 'hashed',
      role: 'USER',
      status: 'ACTIVE',
    });

  beforeEach(async () => {
    users = {
      findOne: jest.fn(),
      save: jest.fn(async (u) => u as any),
    };
    refreshTokens = {
      findOne: jest.fn(),
      save: jest.fn(async (t) => t as any),
    };
    jwtService = { sign: jest.fn(() => 'signed.access.token') };

    const moduleRef = await Test.createTestingModule({
      providers: [
        AuthService,
        { provide: getRepositoryToken(User), useValue: users },
        { provide: getRepositoryToken(RefreshToken), useValue: refreshTokens },
        { provide: JwtService, useValue: jwtService },
      ],
    }).compile();

    service = moduleRef.get<AuthService>(AuthService);
    process.env.JWT_SECRET = 'test-secret';
  });

  it('registers a new user with hashed password and issues tokens', async () => {
    users.findOne.mockResolvedValue(null);

    const result = await service.register({
      email: 'A@B.com',
      firstName: 'Ann',
      lastName: 'Bee',
      password: 'password123',
    });

    expect(users.save).toHaveBeenCalledTimes(1);
    const saved = (users.save as jest.Mock).mock.calls[0][0] as User;
    expect(saved.email).toBe('a@b.com');
    expect(saved.passwordHash).not.toBe('password123');
    expect(await bcrypt.compare('password123', saved.passwordHash)).toBe(true);
    expect(refreshTokens.save).toHaveBeenCalledTimes(1);
    expect(result.accessToken).toBe('signed.access.token');
    expect(result.user.email).toBe('a@b.com');
    expect(result.user).not.toHaveProperty('passwordHash');
  });

  it('rejects duplicate registration', async () => {
    users.findOne.mockResolvedValue(plainUser());

    await expect(
      service.register({
        email: 'a@b.com',
        firstName: 'A',
        lastName: 'B',
        password: 'password123',
      }),
    ).rejects.toBeInstanceOf(ConflictException);
  });

  it('logs a user in with correct credentials', async () => {
    process.env = { ...process.env, JWT_SECRET: 'test-secret' };
    const user = plainUser();
    user.passwordHash = await bcrypt.hash('password123', 4);
    users.findOne.mockResolvedValue(user);

    const result = await service.login({ email: 'a@b.com', password: 'password123' });

    expect(result.accessToken).toBe('signed.access.token');
    expect(refreshTokens.save).toHaveBeenCalledTimes(1);
  });

  it('rejects bad login credentials', async () => {
    users.findOne.mockResolvedValue(plainUser());

    await expect(
      service.login({ email: 'a@b.com', password: 'wrong' }),
    ).rejects.toBeInstanceOf(UnauthorizedException);
  });

  it('refuses to refresh an already-revoked token', async () => {
    const token = Object.assign(new RefreshToken(), {
      revokedAt: new Date(),
      expiresAt: new Date(Date.now() + 60_000),
      user: plainUser(),
    });
    refreshTokens.findOne.mockResolvedValue(token);

    await expect(service.refresh({ refreshToken: 'xyz' })).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
  });

  it('refreshes a valid token by rotating it', async () => {
    const token = Object.assign(new RefreshToken(), {
      revokedAt: null,
      expiresAt: new Date(Date.now() + 60_000),
      user: plainUser(),
    });
    refreshTokens.findOne.mockResolvedValue(token);

    const result = await service.refresh({ refreshToken: 'xyz' });

    expect(result.accessToken).toBe('signed.access.token');
    expect(token.revokedAt).not.toBeNull();
    expect(refreshTokens.save).toHaveBeenCalledTimes(2);
  });

  it('returns the user from me()', async () => {
    users.findOne.mockResolvedValue(plainUser());
    const result = await service.me('u-1');
    expect(result.user.id).toBe('u-1');
    expect(result.user).not.toHaveProperty('passwordHash');
  });
});