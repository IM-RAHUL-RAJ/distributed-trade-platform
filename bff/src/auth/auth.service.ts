import {
  ConflictException,
  Injectable,
  NotFoundException,
  UnauthorizedException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import * as crypto from 'crypto';
import { RegisterDto, LoginDto, RefreshDto } from './auth.dto';
import { User } from './user.entity';
import { RefreshToken } from './refresh-token.entity';

@Injectable()
export class AuthService {
  constructor(
    @InjectRepository(User) private readonly users: Repository<User>,
    @InjectRepository(RefreshToken) private readonly refreshTokens: Repository<RefreshToken>,
    private readonly jwtService: JwtService,
  ) {}

  async register(dto: RegisterDto) {
    const existing = await this.users.findOne({ where: { email: dto.email.toLowerCase() } });
    if (existing) {
      throw new ConflictException('Email already registered');
    }
    const user = new User();
    user.id = crypto.randomUUID();
    user.email = dto.email.toLowerCase();
    user.firstName = dto.firstName;
    user.lastName = dto.lastName;
    user.passwordHash = await bcrypt.hash(dto.password, 12);
    await this.users.save(user);
    const tokens = await this.issueTokens(user);
    return { user: user.toSafeUser(), accessToken: tokens.accessToken, refreshToken: tokens.refreshToken };
  }

  async login(dto: LoginDto) {
    const user = await this.users.findOne({ where: { email: dto.email.toLowerCase() } });
    if (!user) {
      throw new UnauthorizedException('Invalid credentials');
    }
    const ok = await bcrypt.compare(dto.password, user.passwordHash);
    if (!ok) {
      throw new UnauthorizedException('Invalid credentials');
    }
    const tokens = await this.issueTokens(user);
    return { user: user.toSafeUser(), accessToken: tokens.accessToken, refreshToken: tokens.refreshToken };
  }

  async refresh(dto: RefreshDto) {
    const hash = this.hash(dto.refreshToken);
    const token = await this.refreshTokens.findOne({
      where: { tokenHash: hash },
      relations: ['user'],
    });
    if (!token || token.revokedAt) {
      throw new UnauthorizedException('Invalid refresh token');
    }
    if (token.expiresAt.getTime() < Date.now()) {
      throw new UnauthorizedException('Refresh token expired');
    }
    token.revokedAt = new Date();
    await this.refreshTokens.save(token);
    const tokens = await this.issueTokens(token.user);
    return { user: token.user.toSafeUser(), accessToken: tokens.accessToken, refreshToken: tokens.refreshToken };
  }

  async logout(dto: RefreshDto) {
    const hash = this.hash(dto.refreshToken);
    const token = await this.refreshTokens.findOne({ where: { tokenHash: hash } });
    if (token && !token.revokedAt) {
      token.revokedAt = new Date();
      await this.refreshTokens.save(token);
    }
    return { success: true };
  }

  async me(userId: string) {
    const user = await this.users.findOne({ where: { id: userId } });
    if (!user) {
      throw new NotFoundException('User not found');
    }
    return { user: user.toSafeUser() };
  }

  private async issueTokens(user: User) {
    const secret = process.env.JWT_SECRET || 'dev-only-secret-change-me-0123456789abcdef';
    const accessExpires = process.env.ACCESS_TOKEN_TTL || '15m';
    const refreshExpiresSeconds = Number(process.env.REFRESH_TOKEN_TTL_SECONDS || 7 * 24 * 3600);

    const accessToken = this.jwtService.sign(
      { sub: user.id, email: user.email, type: 'access' },
      { secret, expiresIn: accessExpires },
    );

    const refreshTokenValue = crypto.randomUUID();
    const token = new RefreshToken();
    token.id = crypto.randomUUID();
    token.userId = user.id;
    token.tokenHash = this.hash(refreshTokenValue);
    token.expiresAt = new Date(Date.now() + refreshExpiresSeconds * 1000);
    await this.refreshTokens.save(token);

    return { accessToken, refreshToken: refreshTokenValue };
  }

  private hash(value: string): string {
    return crypto.createHash('sha256').update(value).digest('hex');
  }
}