import { Page, expect } from '@playwright/test';

export class ConcertListPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/');
  }

  async searchKeyword(keyword: string) {
    await this.page.fill('input[placeholder*="검색"], input[type="search"]', keyword);
    await this.page.keyboard.press('Enter');
  }

  async filterTab(tabName: 'bookable' | 'closed') {
    if (tabName === 'closed') {
      await this.page.click('button:has-text("마감된 공연")');
    } else {
      await this.page.click('button:has-text("공연중")');
    }
  }

  async expectConcertVisible(concertTitle: string) {
    await expect(this.page.locator(`text=${concertTitle}`)).toBeVisible();
  }

  async clickConcert(concertTitle: string) {
    await this.page.click(`text=${concertTitle}`);
  }
}
