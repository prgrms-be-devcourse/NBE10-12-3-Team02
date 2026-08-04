import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 8: Ticket Group QR Verification Flow', () => {
  test('티켓 그룹 QR 검증 페이지 진입 및 렌더링 검증', async ({ page }) => {
    await page.goto('/verify/group/test-group-token');

    await expect(page).toHaveURL(/\/verify\/group\/test-group-token/);
    await expect(page.locator('h1, h2, div').first()).toBeVisible({ timeout: 10000 });
  });
});
