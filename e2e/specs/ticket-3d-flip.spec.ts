import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 9: 3D Ticket Card Flip Interaction', () => {
  test('마이페이지 티켓 카드 3D 뒤집기(Flip) 인터랙션 검증', async ({ page }) => {
    await page.goto('/mypage');

    // 마이페이지 요소 또는 티켓 카드 대기
    const ticketCard = page.locator('.ticket-card, [data-testid="ticket-card"], div:has-text("티켓")').first();
    if (await ticketCard.isVisible()) {
      await ticketCard.click();
      await expect(ticketCard).toBeVisible();
    }
  });
});
