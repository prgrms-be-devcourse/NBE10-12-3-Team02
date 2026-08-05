import { test, expect, devices } from '@playwright/test';

test.describe('E2E Scenario 8: Ticket Group QR Verification Flow', () => {
  test('데스크톱 환경에서 티켓 그룹 QR 검증 페이지 렌더링 검증', async ({ page }) => {
    await page.goto('/verify/group/test-group-token-12345');
    await expect(page.locator('h1, h2, h3, p, button').first()).toBeVisible({ timeout: 10000 });
  });

  test('모바일 환경(Pixel 5 뷰포트)에서 현장 스태프용 QR 검증 페이지 렌더링 검증', async ({ browser }) => {
    const context = await browser.newContext({ ...devices['Pixel 5'] });
    const page = await context.newPage();

    try {
      await page.goto('/verify/group/test-group-token-12345');
      await expect(page.locator('h1, h2, h3, p, button').first()).toBeVisible({ timeout: 10000 });
    } finally {
      await context.close();
    }
  });
});
