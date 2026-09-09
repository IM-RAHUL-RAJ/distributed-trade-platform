import {
  HttpException,
  Injectable,
  Logger,
} from '@nestjs/common';

export interface ForwardResult {
  status: number;
  body: unknown;
}

/**
 * Thin forwarding layer: authenticated business requests are replayed to
 * Service 1 with the SAME access token. NestJS makes zero business decisions.
 */
@Injectable()
export class ProxyService {
  private readonly logger = new Logger(ProxyService.name);

  private getService1Url(): string {
    return (process.env.SERVICE1_URL || 'http://localhost:8080').replace(/\/$/, '');
  }

  async forward(
    method: string,
    path: string,
    token: string,
    body?: unknown,
  ): Promise<ForwardResult> {
    const url = `${this.getService1Url()}/api/v1/${path}`;
    const headers: Record<string, string> = {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    };
    const init: RequestInit = { method, headers };
    if (body !== undefined && method !== 'GET') {
      init.body = JSON.stringify(body);
    }
    const response = await fetch(url, init as any).catch((err) => {
      this.logger.error(`Forward to ${url} failed: ${err.message}`);
      throw new HttpException('Service 1 unreachable', 502);
    });
    const text = await response.text();
    let json: unknown = null;
    if (text) {
      try {
        json = JSON.parse(text);
      } catch {
        json = text;
      }
    }
    if (!response.ok) {
      const message =
        json && typeof json === 'object' && 'message' in (json as any)
          ? (json as any).message
          : `Service 1 returned ${response.status}`;
      throw new HttpException(message, response.status);
    }
    return { status: response.status, body: json };
  }
}