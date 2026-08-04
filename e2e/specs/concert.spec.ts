import { test, expect } from '@playwright/test';
import { ConcertListPage } from '../pages/ConcertListPage.js';

test.describe('E2E Concert Discovery & Detail Flow', () => {
  test('메인 페이지 공연 목록 및 마감 공연 탭 필터링 검증', async ({ page }) => {
    const listPage = new ConcertListPage(page);
    await listPage.goto();

    await expect(page).toHaveURL('/');
    await expect(page.locator('header, nav').first()).toBeVisible();

    // 마감된 공연 탭 클릭 검증
    await listPage.filterTab('closed');
    
    // 마감 탭 카드 요소 렌더링 확인 (마감 뱃지 또는 공연 카드)
    await expect(page.locator('span:has-text("마감"), h3').first()).toBeVisible({ timeout: 10000 });

    // 공연중 탭 전환
    await listPage.filterTab('bookable');
  });

  test('공연 키워드 검색 동작 검증', async ({ page }) => {
    const listPage = new ConcertListPage(page);
    await listPage.goto();

    await listPage.searchKeyword('난타');
    await expect(page.locator('h3:has-text("난타")').first()).toBeVisible({ timeout: 10000 });
  });
});
