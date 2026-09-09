import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { Request } from 'express';

export interface JwtPayload {
  sub: string;
  email: string;
  type: string;
  iat?: number;
  exp?: number;
}

@Injectable()
export class JwtAuthGuard {
  constructor(private readonly jwtService: JwtService) {}

  validate(request: Request): JwtPayload {
    const header = request.headers['authorization'];
    if (!header || !header.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing bearer token');
    }
    const token = header.slice(7);
    const secret = process.env.JWT_SECRET || 'dev-only-secret-change-me-0123456789abcdef';
    try {
      const payload = this.jwtService.verify<JwtPayload>(token, { secret });
      if (payload.type !== 'access') {
        throw new UnauthorizedException('Not an access token');
      }
      return payload;
    } catch {
      throw new UnauthorizedException('Invalid or expired token');
    }
  }
}