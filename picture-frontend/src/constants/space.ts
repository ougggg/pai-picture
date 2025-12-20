// 空间级别枚举
export const SPACE_LEVEL_ENUM = {
  COMMON: 0,
  PROFESSIONAL: 1,
  FLAGSHIP: 2,
} as const

// 空间级别文本映射
export const SPACE_LEVEL_MAP: Record<number, string> = {
  0: '普通版',
  1: '专业版',
  2: '旗舰版',
}

// 空间级别选项映射
export const SPACE_LEVEL_OPTIONS = Object.keys(SPACE_LEVEL_MAP).map((key) => {
  const value = Number(key) // Convert string key to number
  return {
    label: SPACE_LEVEL_MAP[value],
    value,
  }
})

/**
 * 空间权限常量
 */
export const SPACE_PERMISSION_ENUM = {
  SPACE_USER_MANAGE: 'spaceUser:manage',
  PICTURE_VIEW: 'picture:view',
  PICTURE_UPLOAD: 'picture:upload',
  PICTURE_EDIT: 'picture:edit',
  PICTURE_DELETE: 'picture:delete',
} as const

// 空间类型枚举
export const SPACE_TYPE_ENUM = {
    PRIVATE: 0,
}

// 空间类型文本映射
export const SPACE_TYPE_MAP: Record<number, string> = {
    0: '私有空间',
}
