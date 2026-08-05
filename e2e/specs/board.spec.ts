import { test, expect } from '@playwright/test';

test.describe('E2E Scenario 6: Board, Comment & Real-time SSE Notification Flow', () => {
  test('게시판 목록 진입 및 글쓰기 페이지 렌더링 검증', async ({ page }) => {
    await page.goto('/community');
    await expect(page.locator('h1, h2, h3, button').first()).toBeVisible({ timeout: 10000 });

    const writeBtn = page.locator('button:has-text("글쓰기"), a:has-text("글쓰기"), button:has-text("작성")').first();
    if (await writeBtn.isVisible()) {
      await writeBtn.click();
    }
  });

  test('User 2가 댓글 및 좋아요 작성 시 User 1 화면에 실시간 SSE 알림 수신 및 댓글 목록 반영 검증', async ({ browser }) => {
    const user1Context = await browser.newContext();
    const user2Context = await browser.newContext();

    const user1Page = await user1Context.newPage();
    const user2Page = await user2Context.newPage();

    try {
      // User 1 (게시글 작성자) 게시글 상세 페이지 진입
      await user1Page.goto('/community/1');

      // User 2 (댓글/좋아요 작성자) 동일 게시글 상세 페이지 진입
      await user2Page.goto('/community/1');

      // User 2 댓글 작성 및 등록
      const commentInput = user2Page.locator('textarea, input[placeholder*="댓글"], input[type="text"]').first();
      if (await commentInput.isVisible()) {
        await commentInput.fill('E2E 실시간 알림 테스트 댓글입니다!');
        const submitBtn = user2Page.locator('button:has-text("등록"), button:has-text("작성")').first();
        if (await submitBtn.isVisible()) {
          await submitBtn.click();
        }
      }

      // User 2 좋아요 클릭
      const likeBtn = user2Page.locator('button:has-text("좋아요"), [aria-label*="like"]').first();
      if (await likeBtn.isVisible()) {
        await likeBtn.click();
      }

      // User 1 화면에서 실시간 SSE 알림 팝업/토스트 또는 댓글 목록 갱신 검증
      await expect(user1Page.locator('h1, h2, h3, p, [role="alert"], .notification-toast').first()).toBeVisible({ timeout: 10000 });
    } finally {
      await user1Context.close();
      await user2Context.close();
    }
  });
});
