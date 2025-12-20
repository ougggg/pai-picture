// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** unfavoritePicture POST /api/picture/favorite/cancel */
export async function unfavoritePictureUsingPost(
  body: API.PictureInteractionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/favorite/cancel', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** favoritePicture POST /api/picture/favorite/do */
export async function favoritePictureUsingPost(
  body: API.PictureInteractionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/favorite/do', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listFavoritedPictureByPage POST /api/picture/favorite/list/page */
export async function listFavoritedPictureByPageUsingPost(
  body: API.PictureInteractionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>('/api/picture/favorite/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
