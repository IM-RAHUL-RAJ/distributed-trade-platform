import { IsEmail, IsNotEmpty, Length, MinLength } from 'class-validator';

export class RegisterDto {
  @IsEmail()
  email: string;

  @IsNotEmpty()
  @MinLength(2)
  firstName: string;

  @IsNotEmpty()
  @MinLength(2)
  lastName: string;

  @IsNotEmpty()
  @MinLength(8)
  @Length(8, 128)
  password: string;
}

export class LoginDto {
  @IsEmail()
  email: string;

  @IsNotEmpty()
  password: string;
}

export class RefreshDto {
  @IsNotEmpty()
  refreshToken: string;
}