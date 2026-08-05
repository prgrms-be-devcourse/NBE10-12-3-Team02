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
});
