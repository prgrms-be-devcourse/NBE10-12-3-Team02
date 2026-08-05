import { test, expect } from '@playwright/test';
import { PaymentPage } from '../pages/PaymentPage.js';

test.describe('E2E Scenario 5: Payment & Ticket Generation Flow', () => {
  test('결제 페이지 진입 및 요약 정보 렌더링 검증', async ({ page }) => {
    const paymentPage = new PaymentPage(page);
    await paymentPage.goto();

    await expect(page).toHaveURL(/\/payment/);
    await paymentPage.expectPaymentSummaryVisible();
  });
});
