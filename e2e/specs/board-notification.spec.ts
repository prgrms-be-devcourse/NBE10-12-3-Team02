import { test, expect } from '@playwright/test';
import { BoardPage } from '../pages/BoardPage.js';

test.describe('E2E Scenario 6: Board, Follow & Real-time Notification Flow', () => {
  test('게시판 목록 진입 및 글쓰기 버튼 렌더링 검증', async ({ page }) => {
    const boardPage = new BoardPage(page);
    await boardPage.goto();

    await expect(page).toHaveURL(/\/board/);
    await boardPage.expectBoardListVisible();
  });

  test('게시판 글쓰기 페이지 진입 및 입력 폼 렌더링 검증', async ({ page }) => {
    await page.goto('/board/write');

    await expect(page).toHaveURL(/\/board\/write/);
    await expect(page.locator('input, textarea, button:has-text("등록"), button:has-text("작성")').first()).toBeVisible({
      timeout: 10000,
    });
  });
});
