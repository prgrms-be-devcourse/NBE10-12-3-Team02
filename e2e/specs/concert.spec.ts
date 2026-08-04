import { test, expect } from '@playwright/test';
import { ConcertListPage } from '../pages/ConcertListPage.js';

test.describe('E2E Concert Discovery & Detail Flow', () => {
  test('메인 페이지 공연 목록 및 마감 공연 탭 필터링 검증', async ({ page }) => {
    const listPage = new ConcertListPage(page);
    await listPage.goto();

    await expect(page).toHaveURL('/');
    await expect(page.locator('header, nav')).toBeVisible();

    // 마감된 공연 탭 클릭 검증
    await listPage.filterTab('closed');
    // 마감된 공연 목록에 '개미와 베짱이' 또는 '백설공주' 포함 여부 검증
    await expect(page.locator('text=개미와 베짱이, text=백설공주, text=한씨연대기').first()).toBeVisible({ timeout: 10000 });

    // 예매 가능 탭 전환
    await listPage.filterTab('bookable');
  });

  test('공연 키워드 검색 동작 검증', async ({ page }) => {
    const listPage = new ConcertListPage(page);
    await listPage.goto();

    await listPage.searchKeyword('난타');
    await expect(page.locator('text=난타')).toBeVisible({ timeout: 10000 });
  });
});
