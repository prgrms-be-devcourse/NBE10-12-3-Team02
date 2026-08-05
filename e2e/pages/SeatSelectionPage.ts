import { Page, expect } from '@playwright/test';

export class SeatSelectionPage {
  constructor(private page: Page) {}

  async goto(concertId: number, scheduleId: number) {
    await this.page.goto(`/concerts/${concertId}/seats?scheduleId=${scheduleId}`);
  }

  async selectAvailableSeat(seatNumber: string) {
    const seatLocator = this.page.locator(`[data-seat="${seatNumber}"], button:has-text("${seatNumber}")`);
    await seatLocator.click();
  }

  async expectSeatHold(seatNumber: string) {
    const seatLocator = this.page.locator(`[data-seat="${seatNumber}"]`);
    await expect(seatLocator).toHaveClass(/bg-amber|hold|selected/i);
  }

  async proceedToPayment() {
    await this.page.click('button:has-text("결제하기"), button:has-text("예매하기")');
  }
}
