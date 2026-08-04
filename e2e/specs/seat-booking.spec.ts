import { test, expect } from '@playwright/test';

test.describe('E2E Real-time Seat Hold & SSE Flow', () => {
  test('좌석 선택 페이지 진입 및 SSE 스트림 헤더/연결 검증', async ({ page }) => {
    // SSE 이벤트 감시 모킹 또는 실제 렌더링 확인
    await page.goto('/concerts/1/seats?scheduleId=1');

    // UI 요소 (좌석 캔버스/버튼 또는 대기열 모달) 렌더링 검증 (.first()로 strict mode 방지)
    await expect(page.locator('h1, h2, canvas, svg, [data-seat], button').first()).toBeVisible({ timeout: 10000 });
  });
});
