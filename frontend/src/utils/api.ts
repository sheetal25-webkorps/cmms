import { apiUrl } from '../config';
import i18n from '../i18n/i18n';

type Options = RequestInit & { raw?: boolean; headers?: HeadersInit };

const REFRESH_URL = `${apiUrl}auth/refresh`;

let refreshPromise: Promise<boolean> | null = null;

function clearTokens(): void {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

async function performRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    return false;
  }
  try {
    const response = await fetch(REFRESH_URL, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refreshToken })
    });
    if (!response.ok) {
      clearTokens();
      return false;
    }
    const data = await response.json();
    if (!data?.accessToken) {
      clearTokens();
      return false;
    }
    localStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

export function refreshAccessToken(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

function isRefreshRequest(url: string): boolean {
  return url.replace(/\/+$/, '').endsWith('/auth/refresh');
}

let onConflictError: (() => void) | null = null;

export function setConflictErrorHandler(handler: () => void): () => void {
  onConflictError = handler;
  return () => {
    onConflictError = null;
  };
}

/**
 * Errors thrown by this client carry `isApiError` so UI code (and the global
 * unhandled-rejection toast) can tell them apart from programming errors.
 */
export interface ApiError extends Error {
  isApiError: true;
  status?: number;
  isNetwork?: boolean;
  // Already surfaced to the user (e.g. the global conflict toast).
  handled?: boolean;
}

function apiError(
  message: string,
  extra: Partial<Omit<ApiError, 'isApiError'>>
): ApiError {
  return Object.assign(new Error(message), { isApiError: true as const }, extra);
}

async function doFetch<T>(
  url: string,
  options: Options,
  retried: boolean
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, { headers: authHeader(false), ...options });
  } catch (e) {
    // Aborted requests are intentional (superseded by a newer request).
    if ((e as any)?.name === 'AbortError') throw e;
    throw apiError('network_error', { isNetwork: true });
  }
  if (!response.ok) {
    if (response.status === 401 && !retried && !isRefreshRequest(url)) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return doFetch<T>(url, options, true);
      }
    }
    if (response.status === 409) {
      if (onConflictError) onConflictError();
      throw apiError('conflict_error', { status: 409, handled: true });
    }
    let body: any = null;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    throw apiError(JSON.stringify(body), {
      status: response.status,
      // A gateway error means the API itself is unreachable.
      isNetwork: [502, 503, 504].includes(response.status)
    });
  }
  if (options?.raw) return response as unknown as Promise<T>;
  return response.json() as Promise<T>;
}

function api<T>(url: string, options: Options): Promise<T> {
  return doFetch<T>(url, options, false);
}

function get<T>(url, options?: Options) {
  return api<T>(apiUrl + url, options);
}

function post<T>(url, data, options?: Options, isNotJson?: boolean) {
  return api<T>(apiUrl + url, {
    ...options,
    method: 'POST',
    body: isNotJson ? data : JSON.stringify(data)
  });
}

function patch<T>(url, data, options?: Options) {
  return api<T>(apiUrl + url, {
    ...options,
    method: 'PATCH',
    body: JSON.stringify(data)
  });
}

function deletes<T>(url, options?: Options) {
  return api<T>(apiUrl + url, { ...options, method: 'DELETE' });
}

export function authHeader(publicRoute: boolean): HeadersInit {
  // return authorization header with jwt token
  let accessToken = localStorage.getItem('accessToken');

  if (!publicRoute && accessToken) {
    return {
      Authorization: 'Bearer ' + accessToken,
      Accept: 'application/json',
      'Content-Type': 'application/json'
    };
  } else {
    return {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    };
  }
}

const TECHNICAL_MESSAGE = /exception|\bat (com|org|java)\.|sql|hibernate|jdbc|stack ?trace|null ?pointer|^null$|^undefined$|<html/i;

/** A backend message is shown as-is only if it reads like text meant for users. */
function isUserFacingMessage(message: unknown): message is string {
  return (
    typeof message === 'string' &&
    message.trim().length > 0 &&
    message.length <= 300 &&
    !TECHNICAL_MESSAGE.test(message)
  );
}

/**
 * Turns any thrown value into a message that is safe and meaningful for end users.
 * Never returns raw JSON, stack traces or framework internals.
 */
export const getErrorMessage = (
  error: any,
  defaultMessage?: string
): string => {
  const fallback =
    defaultMessage ??
    i18n.t('generic_error_message', {
      defaultValue: 'Something went wrong. Please try again.'
    });
  if (!error) return fallback;
  if (isNetworkError(error))
    return i18n.t('network_error_message', {
      defaultValue:
        'Unable to reach the server. Check your connection and try again.'
    });
  if (error.message === 'conflict_error') return i18n.t('conflict_error');

  let backendMessage: unknown;
  try {
    const parsed = JSON.parse(error.message);
    backendMessage =
      parsed && typeof parsed === 'object' ? parsed.message : undefined;
  } catch {
    // Not a JSON body: a plain Error thrown by our own code.
    backendMessage = error.isApiError ? undefined : error.message;
  }
  if (isUserFacingMessage(backendMessage)) return backendMessage;

  if (error.status === 401)
    return i18n.t('session_expired_message', {
      defaultValue: 'Your session has expired. Please log in again.'
    });
  if (error.status === 403)
    return i18n.t('forbidden_message', {
      defaultValue: "You don't have permission to perform this action."
    });
  if (error.status === 404)
    return i18n.t('not_found_message', {
      defaultValue: 'The requested item could not be found.'
    });
  return fallback;
};

export const isNetworkError = (error: any): boolean => {
  if (!error) return false;
  if (error.isNetwork) return true;
  if (error instanceof TypeError) return true;
  const message = String(error?.message ?? '').toLowerCase();
  return (
    message === 'network_error' ||
    message.includes('failed to fetch') ||
    message.includes('networkerror') ||
    message.includes('network error') ||
    message.includes('load failed') ||
    message.includes('connection refused') ||
    message.includes('err_connection_refused')
  );
};

export default { get, patch, post, deletes };
