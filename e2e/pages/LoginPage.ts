import { Page, expect } from '@playwright/test';

export class LoginPage {
  constructor(private page: Page) {}

  async goto() {
    await this.page.goto('/login');
  }

  async login(id: string, pass: string) {
    await this.page.fill('input[name="id"], input[type="text"]', id);
    await this.page.fill('input[name="password"], input[type="password"]', pass);
    await this.page.click('button[type="submit"]');
  }

  async expectLoggedIn(userName: string) {
    await expect(this.page.locator(`text=${userName}님`)).toBeVisible({ timeout: 10000 });
  }

  async expectErrorMessage() {
    await expect(this.page.locator('.swal2-popup').first()).toBeVisible();
  }
}
