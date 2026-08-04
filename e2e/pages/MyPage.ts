import { Page, expect } from '@playwright/test';

export class MyPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/mypage');
  }

  async selectTab(tabName: 'info' | 'tickets' | 'posts') {
    const labelMap = {
      info: '내 정보',
      tickets: '내 티켓',
      posts: '내 게시글',
    };
    await this.page.click(`button:has-text("${labelMap[tabName]}")`);
  }

  async expectUserInfo(name: string) {
    await expect(this.page.locator(`text=${name}님`)).toBeVisible();
  }
}
