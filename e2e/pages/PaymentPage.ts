import { Page, expect } from '@playwright/test';

export class PaymentPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/payment');
  }

  async expectPaymentSummaryVisible() {
    await expect(this.page.locator('text=결제, text=주문, text=티켓, button:has-text("결제하기")').first()).toBeVisible({ timeout: 10000 });
  }

  async completePayment() {
    await this.page.click('button:has-text("결제하기"), button:has-text("동의하고 결제하기")');
  }
}
