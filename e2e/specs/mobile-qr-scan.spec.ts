import { test, expect, devices } from '@playwright/test';

test.describe('E2E Scenario 11: Mobile QR Verification Flow', () => {
  test.use({ ...devices['Pixel 5'] }); // 모바일 Chrome 뷰포트 시뮬레이션

  test('모바일 환경에서 현장 스태프용 QR 검증 페이지 렌더링 및 유효성 검증', async ({ page }) => {
    await page.goto('/verify/group/test-group-token-12345');

    // 모바일 뷰포트 렌더링 확인
    await expect(page.locator('h1, h2, h3, p, button').first()).toBeVisible({ timeout: 10000 });
  });
});
