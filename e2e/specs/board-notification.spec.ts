import { test, expect } from '@playwright/test';
import { BoardPage } from '../pages/BoardPage.js';

test.describe('E2E Scenario 6: Board, Follow & Real-time Notification Flow', () => {
  test('게시판 목록 진입 및 글쓰기 버튼 렌더링 검증', async ({ page }) => {
    const boardPage = new BoardPage(page);
    await boardPage.goto();

    await expect(page).toHaveURL(/\/board/);
    await boardPage.expectBoardListVisible();
  });
});
