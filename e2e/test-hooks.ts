import { APIRequestContext } from '@playwright/test';

/**
 * Resets H2 In-Memory Database and Redis cache before E2E test runs
 */
export async function resetTestDatabase(request: APIRequestContext) {
  const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';
  try {
    const response = await request.post(`${backendUrl}/api/v1/test/reset-db`, {
      timeout: 5000,
    });
    if (response.ok()) {
      console.log('✅ [E2E] Database & Redis Clean Reset Completed');
    }
  } catch (error) {
    // Graceful fallback if backend is not yet reachable or reset endpoint disabled
    console.warn('⚠️ [E2E] DB Reset API skipped:', error instanceof Error ? error.message : error);
  }
}
