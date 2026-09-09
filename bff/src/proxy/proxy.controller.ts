import {
  All,
  Body,
  Controller,
  Get,
  Param,
  Post,
  Put,
  Req,
  UseGuards,
} from '@nestjs/common';
import { Request } from 'express';
import { AccessTokenGuard } from '../auth/access-token.guard';
import { ProxyService } from './proxy.service';

/**
 * Business API surface. Every route is JWT-protected and forwarded to
 * Service 1 verbatim. No business logic lives here — this is transport only.
 */
@Controller()
@UseGuards(AccessTokenGuard)
export class ProxyController {
  constructor(private readonly proxy: ProxyService) {}

  private tokenOf(req: Request): string {
    return (req.headers['authorization'] as string).slice(7);
  }

  @Get('preferences')
  async getPreferences(@Req() req: Request) {
    return this.proxy.forward('GET', 'preferences', this.tokenOf(req));
  }

  @Post('preferences')
  async createPreferences(@Req() req: Request, @Body() body: unknown) {
    return this.proxy.forward('POST', 'preferences', this.tokenOf(req), body);
  }

  @Put('preferences')
  async updatePreferences(@Req() req: Request, @Body() body: unknown) {
    return this.proxy.forward('PUT', 'preferences', this.tokenOf(req), body);
  }

  @Get('dashboard')
  async dashboard(@Req() req: Request) {
    return this.proxy.forward('GET', 'dashboard', this.tokenOf(req));
  }

  @Get('account')
  async account(@Req() req: Request) {
    return this.proxy.forward('GET', 'account', this.tokenOf(req));
  }

  @Get('portfolio')
  async portfolio(@Req() req: Request) {
    return this.proxy.forward('GET', 'portfolio', this.tokenOf(req));
  }

  @Get('positions')
  async positions(@Req() req: Request) {
    return this.proxy.forward('GET', 'positions', this.tokenOf(req));
  }

  @Get('instruments')
  async instruments(@Req() req: Request) {
    return this.proxy.forward('GET', 'instruments', this.tokenOf(req));
  }

  @Get('market-data')
  async marketData(@Req() req: Request) {
    return this.proxy.forward('GET', 'market-data', this.tokenOf(req));
  }

  @Get('watchlist')
  async watchlist(@Req() req: Request) {
    return this.proxy.forward('GET', 'watchlist', this.tokenOf(req));
  }

  @Get('orders')
  async listOrders(@Req() req: Request) {
    return this.proxy.forward('GET', 'orders', this.tokenOf(req));
  }

  @Post('orders')
  async createOrder(@Req() req: Request, @Body() body: unknown) {
    return this.proxy.forward('POST', 'orders', this.tokenOf(req), body);
  }

  @Get('orders/:id')
  async getOrder(@Req() req: Request, @Param('id') id: string) {
    return this.proxy.forward('GET', `orders/${id}`, this.tokenOf(req));
  }

  @Post('orders/:id/cancel')
  async cancelOrder(@Req() req: Request, @Param('id') id: string) {
    return this.proxy.forward('POST', `orders/${id}/cancel`, this.tokenOf(req), {});
  }

  @Get('trades')
  async trades(@Req() req: Request) {
    return this.proxy.forward('GET', 'trades', this.tokenOf(req));
  }

  @Get('transactions')
  async transactions(@Req() req: Request) {
    return this.proxy.forward('GET', 'transactions', this.tokenOf(req));
  }
}