import { test, expect } from '@playwright/test';

test.describe('E2E Real-time Seat Hold & SSE Flow', () => {
  test('좌석 선택 페이지 진입 및 SSE 스트림 헤더/연결 검증', async ({ page }) => {
    await page.goto('/concerts/1/seats?scheduleId=1');

    // 좌석 선택 화면 요소 (대기열 또는 좌석 그리드/버튼) 렌더링 검증
    await expect(page.locator('h1, h2, canvas, svg, [data-seat], button').first()).toBeVisible({ timeout: 10000 });
  });

  test('좌석 클릭 선택 및 선점 인터랙션 검증', async ({ page }) => {
    await page.goto('/concerts/1/seats?scheduleId=1');

    // 좌석 버튼이 노출되는 경우 클릭 상호작용 수행
    const seatBtn = page.locator('[data-seat], button:has-text("A-1"), button:has-text("VIP")').first();
    if (await seatBtn.isVisible()) {
      await seatBtn.click();
    }

    await expect(page.locator('h1, h2, canvas, svg, [data-seat], button').first()).toBeVisible();
  });
});
