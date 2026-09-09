import { fetchHotPosts } from '@/api/hot'
import { fetchCategories, pagePosts, type Category, type PostCard } from '@/api/forum'
import { pageShares, type ShareCard } from '@/api/share'

/** T-FE-2：首页三区块缓存 TTL（与后端 /hot 60s 对齐） */
export const HOME_FEED_TTL_MS = 60_000

export interface HomeFeed {
  hot: PostCard[]
  shares: ShareCard[]
  posts: PostCard[]
  categories: Category[]
  fetchedAt: number
}

let cache: HomeFeed | null = null
let inflight: Promise<HomeFeed> | null = null

function isFresh(feed: HomeFeed): boolean {
  return Date.now() - feed.fetchedAt < HOME_FEED_TTL_MS
}

async function fetchHomeFeed(): Promise<HomeFeed> {
  const [hot, sharesPage, postsPage, categories] = await Promise.all([
    fetchHotPosts(5),
    pageShares(1, 6),
    pagePosts(1, 5, undefined, 'latest'),
    fetchCategories()
  ])
  return {
    hot,
    shares: sharesPage.records,
    posts: postsPage.records,
    categories,
    fetchedAt: Date.now()
  }
}

/**
 * 首页聚合数据：60s 内复用缓存；并发进入合并为单次请求。
 */
export function loadHomeFeed(force = false): Promise<HomeFeed> {
  if (!force && cache && isFresh(cache)) {
    return Promise.resolve(cache)
  }
  if (!force && inflight) {
    return inflight
  }
  inflight = fetchHomeFeed()
    .then((feed) => {
      cache = feed
      return feed
    })
    .finally(() => {
      inflight = null
    })
  return inflight
}

/** 测试/调试用 */
export function clearHomeFeedCache(): void {
  cache = null
  inflight = null
}
