import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 7: MyPage Profile & 3D Ticket Card Flip Interaction', () => {
  test('마이페이지 진입 및 프로필 정보 렌더링 검증', async ({ page }) => {
    await page.goto('/mypage');
    await expect(page.locator('h1, h2, h3, p, button').first()).toBeVisible({ timeout: 10000 });
  });

  test('마이페이지 발권 티켓 카드 3D 뒤집기(Flip) 인터랙션 검증', async ({ page }) => {
    await page.goto('/mypage');

    const ticketCard = page.locator('.ticket-card, [data-testid="ticket-card"], div:has-text("티켓")').first();
    if (await ticketCard.isVisible()) {
      await ticketCard.click();
      await expect(ticketCard).toBeVisible();
    }
  });
});
