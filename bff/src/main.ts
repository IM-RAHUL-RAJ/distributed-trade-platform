import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.setGlobalPrefix('api/v1');
  app.enableCors({ origin: true, credentials: true });
  app.useGlobalPipes(
    new ValidationPipe({ whitelist: true, transform: true, forbidNonWhitelisted: false }),
  );
  const port = Number(process.env.PORT || 3000);
  await app.listen(port);
  new Logger('Bootstrap').log(`BFF listening on http://0.0.0.0:${port}/api/v1`);
}
bootstrap();