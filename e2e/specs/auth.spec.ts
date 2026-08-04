import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage.js';
import { MyPage } from '../pages/MyPage.js';

test.describe('E2E Auth & User Onboarding Flow', () => {
  test('로그인 페이지 렌더링 및 입력 폼 동작 검증', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator('h1, h2, form')).toBeVisible();
  });

  test('비정상 유저 로그인 시 오류 얼럿/메시지 노출 검증', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('invalid_user_id', 'wrong_password');

    // 실패 알림/팝업 확인
    await loginPage.expectErrorMessage();
  });
});
