import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { User } from './user.entity';
import { RefreshToken } from './refresh-token.entity';
import { JwtAuthGuard } from './jwt-auth.guard';
import { AccessTokenGuard } from './access-token.guard';

@Module({
  imports: [
    TypeOrmModule.forFeature([User, RefreshToken]),
    JwtModule.register({}),
  ],
  providers: [AuthService, JwtAuthGuard, AccessTokenGuard],
  controllers: [AuthController],
  exports: [JwtAuthGuard, AccessTokenGuard],
})
export class AuthModule {}