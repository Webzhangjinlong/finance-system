import request from '@/utils/request'

// 示例接口：系统健康检查
export function getHealth() {
  return request({
    url: '/system/health',
    method: 'get'
  })
}
