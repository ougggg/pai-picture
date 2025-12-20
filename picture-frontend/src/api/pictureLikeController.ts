// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** unlikePicture POST /api/picture/like/cancel */
export async function unlikePictureUsingPost(
  body: API.PictureInteractionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/like/cancel', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** likePicture POST /api/picture/like/do */
export async function likePictureUsingPost(
  body: API.PictureInteractionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/like/do', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listLikedPictureByPage POST /api/picture/like/list/page */
export async function listLikedPictureByPageUsingPost(
  body: API.PictureInteractionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>('/api/picture/like/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
