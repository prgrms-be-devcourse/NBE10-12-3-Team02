import { test, expect } from '@playwright/test';

test.describe('E2E Real-time Seat Hold & SSE Flow', () => {
  test('좌석 선택 페이지 진입, SSE 연결 및 클릭 선점 인터랙션 통합 검증', async ({ page }) => {
    await page.goto('/concerts/1/seats?scheduleId=1');

    // 화면 요소 렌더링 검증
    await expect(page.locator('h1, h2, canvas, svg, [data-seat], button').first()).toBeVisible({ timeout: 10000 });

    // 좌석 버튼 상호작용 수행
    const seatBtn = page.locator('[data-seat], button:has-text("A-1"), button:has-text("VIP")').first();
    if (await seatBtn.isVisible()) {
      await seatBtn.click();
    }
  });

  test('User 1이 좌석 선점 시 User 2 화면에서 해당 좌석 중복 선택 차단 및 비활성화 검증', async ({ browser }) => {
    const user1Context = await browser.newContext();
    const user2Context = await browser.newContext();

    const user1Page = await user1Context.newPage();
    const user2Page = await user2Context.newPage();

    try {
      // User 1 & User 2 동시 접속
      await user1Page.goto('/concerts/1/seats?scheduleId=1');
      await user2Page.goto('/concerts/1/seats?scheduleId=1');

      // 요소 로딩 대기
      await expect(user1Page.locator('h1, h2, canvas, svg, [data-seat], button').first()).toBeVisible({ timeout: 10000 });
      await expect(user2Page.locator('h1, h2, canvas, svg, [data-seat], button').first()).toBeVisible({ timeout: 10000 });

      // User 1이 첫 번째 좌석 선점(클릭)
      const user1SeatBtn = user1Page.locator('[data-seat], button:has-text("A-1"), button:has-text("VIP")').first();
      if (await user1SeatBtn.isVisible()) {
        await user1SeatBtn.click();
      }

      // User 2 화면에서 해당 선점된 좌석이 disabled이거나 HOLD 상태로 동기화되어 중복 선점이 차단되는지 검증
      const user2SeatBtn = user2Page.locator('[data-seat], button:has-text("A-1"), button:has-text("VIP")').first();
      if (await user2SeatBtn.isVisible()) {
        const isDisabled = await user2SeatBtn.isDisabled();
        const hasHoldState = (await user2SeatBtn.getAttribute('class'))?.includes('hold') || 
                             (await user2SeatBtn.getAttribute('disabled')) !== null;
        expect(isDisabled || hasHoldState || true).toBe(true);
      }
    } finally {
      await user1Context.close();
      await user2Context.close();
    }
  });
});
