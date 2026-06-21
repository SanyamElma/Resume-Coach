import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi, profileApi } from '../services';
import { tokenStore } from '../services/apiClient';

const AuthContext = createContext(null);

/**
 * Holds the authenticated user and exposes login/register/logout actions. Tokens live in
 * localStorage (via tokenStore); the user object is hydrated on mount when a token exists.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [initializing, setInitializing] = useState(true);

  const applyAuth = useCallback((auth) => {
    tokenStore.set(auth.accessToken, auth.refreshToken);
    setUser(auth.user);
  }, []);

  const logout = useCallback(async () => {
    const refresh = tokenStore.getRefresh();
    try {
      if (refresh) await authApi.logout(refresh);
    } catch {
      /* best-effort: revoke locally regardless */
    }
    tokenStore.clear();
    setUser(null);
  }, []);

  const login = useCallback(
    async (credentials) => {
      const auth = await authApi.login(credentials);
      applyAuth(auth);
      return auth;
    },
    [applyAuth],
  );

  const register = useCallback(
    async (payload) => {
      const auth = await authApi.register(payload);
      applyAuth(auth);
      return auth;
    },
    [applyAuth],
  );

  // Hydrate the session on first load if a token is present.
  useEffect(() => {
    const bootstrap = async () => {
      if (!tokenStore.getAccess()) {
        setInitializing(false);
        return;
      }
      try {
        setUser(await profileApi.get());
      } catch {
        tokenStore.clear();
      } finally {
        setInitializing(false);
      }
    };
    bootstrap();
  }, []);

  // React to forced logout from the axios refresh interceptor.
  useEffect(() => {
    const handler = () => setUser(null);
    window.addEventListener('auth:logout', handler);
    return () => window.removeEventListener('auth:logout', handler);
  }, []);

  const value = useMemo(
    () => ({
      user,
      setUser,
      initializing,
      isAuthenticated: Boolean(user),
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
    }),
    [user, initializing, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
};
