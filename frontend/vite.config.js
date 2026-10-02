import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// 개발 서버는 /api 요청을 Spring Boot(기본 8080)로 넘긴다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.VITE_API_PROXY || 'http://localhost:8080',
    },
  },
});
