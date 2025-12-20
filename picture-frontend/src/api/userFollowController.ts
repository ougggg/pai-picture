// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** unfollowUser POST /api/user/follow/cancel */
export async function unfollowUserUsingPost(
  body: API.UserFollowRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/follow/cancel', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** followUser POST /api/user/follow/do */
export async function followUserUsingPost(
  body: API.UserFollowRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/follow/do', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listFollowersByPage POST /api/user/follow/followers/list/page */
export async function listFollowersByPageUsingPost(
  body: API.UserFollowQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO_>('/api/user/follow/followers/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listFollowingByPage POST /api/user/follow/following/list/page */
export async function listFollowingByPageUsingPost(
  body: API.UserFollowQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO_>('/api/user/follow/following/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** isFollowing GET /api/user/follow/isFollowing */
export async function isFollowingUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.isFollowingUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/follow/isFollowing', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
