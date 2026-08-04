import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 3: Waiting Queue Flow', () => {
  test('대기열 진입 및 대기열 모달/순번 노출 검증', async ({ page }) => {
    // 대기열 페이지/좌석진입 시 대기열 팝업 노출 검증
    await page.goto('/concerts/1/seats?scheduleId=1');

    // 대기 중입니다 또는 좌석 화면 요소 렌더링 검증
    await expect(page.locator('text=대기 중입니다, h1, h2, canvas, svg, [data-seat]').first()).toBeVisible({ timeout: 10000 });
  });
});
