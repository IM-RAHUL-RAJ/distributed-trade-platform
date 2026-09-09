import {
  CanActivate,
  ExecutionContext,
  Injectable,
} from '@nestjs/common';
import { Request } from 'express';
import { JwtAuthGuard, JwtPayload } from './jwt-auth.guard';

@Injectable()
export class AccessTokenGuard implements CanActivate {
  constructor(private readonly guard: JwtAuthGuard) {}

  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<Request & { user?: JwtPayload }>();
    request.user = this.guard.validate(request);
    return true;
  }
}

export const CurrentUser = (ctx: ExecutionContext): JwtPayload =>
  ctx.switchToHttp().getRequest<Request & { user: JwtPayload }>().user;