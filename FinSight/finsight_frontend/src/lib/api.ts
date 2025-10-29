const API_BASE = (import.meta as any).env?.VITE_API_BASE || 'http://localhost:8080';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

async function apiFetch<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('finsight_token');
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  console.log(`API Request: ${options.method || 'GET'} ${API_BASE}${path}`);
  console.log('Request headers:', headers);
  if (options.body) {
    console.log('Request body:', options.body);
  }

  try {
    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    const contentType = res.headers.get('content-type') || '';
    const data = contentType.includes('application/json') ? await res.json().catch(() => ({})) : await res.text();
    
    console.log(`API Response: ${res.status} ${res.statusText}`);
    console.log('Response data:', data);
    
    if (!res.ok) {
      // Auto-logout on auth errors
      if (res.status === 401 || res.status === 403) {
        try {
          localStorage.removeItem('finsight_token');
          localStorage.removeItem('finsight_user');
        } catch { }
        // Redirect to login to refresh auth
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
      }
      const message = typeof data === 'string' ? data : (data as any)?.error || `Request failed (${res.status})`;
      console.error('API Error:', message);
      throw new Error(message);
    }
    return data as T;
  } catch (error) {
    console.error('API Fetch Error:', error);
    if (error instanceof Error) {
      throw error;
    }
    throw new Error('Network error occurred');
  }
}

export const api = {
  get: <T = any>(path: string) => apiFetch<T>(path, { method: 'GET' }),
  post: <T = any>(path: string, body?: any) => apiFetch<T>(path, { method: 'POST', body: JSON.stringify(body || {}) }),
  put: <T = any>(path: string, body?: any) => apiFetch<T>(path, { method: 'PUT', body: JSON.stringify(body || {}) }),
  del: <T = any>(path: string) => apiFetch<T>(path, { method: 'DELETE' }),
};

export type Portfolio = {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
};

export type PortfolioHolding = {
  id: number;
  ticker: string;
  name: string;
  quantity: number;
  averagePrice: number;
  minThreshold?: number | null;
  maxThreshold?: number | null;
};

export type Company = {
  ticker: string;
  name: string;
  sector?: string;
  country?: string;
  marketCap?: number;
  description?: string;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type HoldingValuation = {
  ticker: string;
  name: string;
  quantity: number;
  averagePrice: number;
  lastPrice?: number | null;
  invested: number;
  marketValue?: number | null;
  pnlAbs?: number | null;
  pnlPct?: number | null;
  priceAsOf?: string | null;
  stale: boolean;
  minThreshold?: number | null;
  maxThreshold?: number | null;
};

export type PortfolioValuation = {
  portfolioId: number;
  currency: string;
  updatedAt: string;
  totals: {
    invested: number;
    marketValue: number;
    pnlAbs: number;
    pnlPct: number;
    staleCount: number;
  };
  holdings: HoldingValuation[];
};

export const valuationApi = {
  getPortfolioValue: (id: number) => api.get<PortfolioValuation>(`/api/portfolios/${id}/value`),
};

export type PortfolioHistoryPoint = {
  id: number;
  snapshotDate: string;
  capturedAt: string;
  invested: number;
  marketValue: number;
  pnlAbs: number;
  pnlPct: number;
  staleCount: number;
};

export const portfolioApi = {
  getHistory: (id: number, backfillDays: number = 90) =>
    api.get<PortfolioHistoryPoint[]>(`/api/portfolios/${id}/history?backfillDays=${encodeURIComponent(String(backfillDays))}`),
};

export type Alert = {
  id: number;
  ticker: string;
  conditionType: 'GT' | 'LT';
  threshold: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type CreateAlertRequest = {
  ticker: string;
  conditionType: 'GT' | 'LT';
  threshold: number;
};

export type IndicatorDefinition = {
  key: string;
  label: string;
  type: string;
  period: number;
};

export type IndicatorPoint = {
  timestamp: number;
  close?: number;
  overlays: Record<string, number>;
};

export type IndicatorResponse = {
  ticker: string;
  resolution: string;
  overlays: IndicatorDefinition[];
  points: IndicatorPoint[];
};

export const indicatorsApi = {
  get: (
    ticker: string,
    params: Partial<{
      resolution: string;
      count: number;
      sma: string;
      ema: string;
      rsi: string;
    }> = {},
  ) => {
    const search = new URLSearchParams();
    if (params.resolution) search.set('resolution', params.resolution);
    if (params.count) search.set('count', String(params.count));
    if (params.sma) search.set('sma', params.sma);
    if (params.ema) search.set('ema', params.ema);
    if (params.rsi) search.set('rsi', params.rsi);
    const suffix = search.toString();
    const path = `/api/indicators/${encodeURIComponent(ticker)}${suffix ? `?${suffix}` : ''}`;
    return api.get<IndicatorResponse>(path);
  },
};

// IPO types and API
export type Ipo = {
  id: number;
  symbol: string;
  name: string;
  date: string; // ISO date
  exchange?: string | null;
  numberOfShares?: number | null;
  price?: string | null;
  status?: string | null;
  totalSharesValue?: number | null;
  createdAt?: string;
  updatedAt?: string;
};

export const ipoApi = {
  upcoming: () => api.get<Ipo[]>(`/api/ipo/upcoming`),
  all: () => api.get<Ipo[]>(`/api/ipo/all`),
  fetchNow: () => api.post<string>(`/api/ipo/fetch`),
};

// Users API
export type UpdateUserRequest = {
  firstName: string;
  lastName: string;
};

export type UserResponse = {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  createdAt: string;
};

export const userApi = {
  update: (id: number, body: UpdateUserRequest) => api.put<UserResponse>(`/api/users/${id}`, body),
  delete: (id: number) => api.del<void>(`/api/users/${id}`),
  initiatePasswordChange: () => api.post<string>(`/api/users/change-password/initiate`, {}),
  confirmPasswordChange: (body: { otp: string; newPassword: string; confirmPassword: string }) =>
    api.post<string>(`/api/users/change-password/confirm`, body),
};