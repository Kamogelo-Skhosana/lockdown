import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const origin = env.LOCKDOWN_API_ORIGIN;
  if (!origin) return {};
  const url = new URL(origin);
  if (url.protocol !== 'https:' || url.origin !== origin) {
    throw new Error('LOCKDOWN_API_ORIGIN must be an HTTPS origin without a path.');
  }
  return {
    define: { __LOCKDOWN_DEV_PROXY__: JSON.stringify(true) },
    server: {
      proxy: {
        '/api/assessment': {
          target: origin,
          changeOrigin: true,
          rewrite: () => '/assessment',
        },
      },
    },
  };
});
