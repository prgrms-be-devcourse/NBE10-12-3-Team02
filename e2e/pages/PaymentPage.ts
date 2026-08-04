import { Page, expect } from '@playwright/test';

export class PaymentPage {
  constructor(private page: Page) {}

  async goto() {
    const seatsParam = encodeURIComponent(
      JSON.stringify([{ seatNumber: 'A-1', occupyToken: 'test-token', price: 150000 }])
    );
    await this.page.addInitScript(() => {
      window.sessionStorage.setItem('paymentActive', Date.now().toString());
    });
    await this.page.goto(`/payment?concertId=1&scheduleId=1&seats=${seatsParam}`);
  }

  async expectPaymentSummaryVisible() {
    await expect(this.page.locator('h1:has-text("예매 정보 입력"), text=예매 정보').first()).toBeVisible({
      timeout: 10000,
    });
  }

  async completePayment() {
    await this.page.click('button:has-text("결제하기"), button:has-text("동의하고 결제하기")');
  }
}
