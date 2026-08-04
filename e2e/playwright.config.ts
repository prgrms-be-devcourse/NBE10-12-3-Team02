import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E Integration Test Configuration
 * NBE10-12-3-Team02 E2E System Test Suite
 */
export default defineConfig({
  testDir: './specs',
  timeout: 30 * 1000,
  expect: {
    timeout: 5000,
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    ['list']
  ],
  use: {
    baseURL: process.env.FRONT_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10 * 1000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: process.platform === 'win32' ? '.\\gradlew.bat bootRun --no-daemon' : './gradlew bootRun --no-daemon',
      cwd: '../back',
      url: 'http://localhost:8080/api/v1/concerts',
      reuseExistingServer: !process.env.CI,
      timeout: 180 * 1000,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: 'pnpm dev',
      cwd: '../front',
      url: 'http://localhost:3000',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
      stdout: 'ignore',
      stderr: 'pipe',
    },
  ],
});
