import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 7: MyPage & Profile Management Flow', () => {
  test('마이페이지 진입 및 페이지 렌더링 검증', async ({ page }) => {
    await page.goto('/mypage');

    await expect(page).toHaveURL(/\/mypage/);
    await expect(page.locator('h1, h2, nav, button, form').first()).toBeVisible({ timeout: 10000 });
  });
});
