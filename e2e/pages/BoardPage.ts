import { Page, expect } from '@playwright/test';

export class BoardPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/board');
  }

  async expectBoardListVisible() {
    await expect(this.page.locator('h1, h2, a[href*="/board/"]').first()).toBeVisible({ timeout: 10000 });
  }

  async clickWritePostButton() {
    await this.page.click('button:has-text("글쓰기"), a:has-text("글쓰기")');
  }
}
