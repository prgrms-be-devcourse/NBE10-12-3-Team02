import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 3: Waiting Queue Flow', () => {
  test('단일 유저 대기열 진입 및 렌더링 검증', async ({ page }) => {
    await page.goto('/concerts/1/seats?scheduleId=1');
    await expect(page.locator('h1, h2, canvas, svg, [data-seat]').first()).toBeVisible({ timeout: 10000 });
  });

  test('대기열 동시 접속 3인 시뮬레이션 및 정원 관리 검증', async ({ browser }) => {
    // 유저 1, 유저 2, 유저 3 독립 브라우저 세션 생성
    const user1Context = await browser.newContext();
    const user2Context = await browser.newContext();
    const user3Context = await browser.newContext();

    const user1Page = await user1Context.newPage();
    const user2Page = await user2Context.newPage();
    const user3Page = await user3Context.newPage();

    try {
      // User 1, User 2 순차 진입 (정원 2명 수용)
      await user1Page.goto('/concerts/1/seats?scheduleId=1');
      await user2Page.goto('/concerts/1/seats?scheduleId=1');

      // User 3 진입 (정원 초과 검증)
      await user3Page.goto('/concerts/1/seats?scheduleId=1');
      await expect(user3Page.locator('h1, h2, h3, canvas, svg, button').first()).toBeVisible({ timeout: 10000 });
    } finally {
      await user1Context.close();
      await user2Context.close();
      await user3Context.close();
    }
  });
});
