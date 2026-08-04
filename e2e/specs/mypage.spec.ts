import { test, expect } from '@playwright/test';
import { MyPage } from '../pages/MyPage.js';

test.describe('E2E Scenario 7: MyPage & Profile Management Flow', () => {
  test('마이페이지 진입 및 내 정보/티켓/게시글 탭 전환 검증', async ({ page }) => {
    const myPage = new MyPage(page);
    await myPage.goto();

    await expect(page).toHaveURL(/\/mypage/);
    
    // 탭 전환 동작 검증
    await myPage.selectTab('info');
    await expect(page.locator('text=내 정보, text=소셜 계정, text=비밀번호').first()).toBeVisible({ timeout: 10000 });

    await myPage.selectTab('tickets');
    await expect(page.locator('text=내 티켓, text=예매 내역, text=티켓').first()).toBeVisible({ timeout: 10000 });

    await myPage.selectTab('posts');
    await expect(page.locator('text=내 게시글, text=북마크, text=좋아요').first()).toBeVisible({ timeout: 10000 });
  });
});
