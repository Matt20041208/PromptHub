export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface UserDTO {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  role: string
  vip: boolean
}

export interface LoginResultDTO {
  token: string
  user: UserDTO
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  nickname?: string
}

export interface UserUpdateRequest {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  bio?: string
}

export interface PromptListVO {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  title: string
  description: string
  cover: string
  price: number
  status: string
  tags: string[]
  categoryName: string
  viewCount: number
  downloadCount: number
  avgRating: number
  createTime: string
}

export interface PromptDetailVO extends PromptListVO {
  content: string
  templateSchema: string
  categoryId: number
  versions: PromptVersionVO[]
  purchased: boolean
  contentLocked: boolean
}

export interface PromptVersionVO {
  id: number
  versionNo: string
  changelog: string
  createTime: string
}

export interface PromptCreateRequest {
  title: string
  description?: string
  content: string
  templateSchema?: string
  cover?: string
  price?: number
  categoryId?: number
  tagIds?: number[]
}

export interface PromptUpdateRequest {
  title?: string
  description?: string
  content?: string
  templateSchema?: string
  cover?: string
  price?: number
  categoryId?: number
  tagIds?: number[]
}

export interface PromptQueryDTO {
  page?: number
  size?: number
  keyword?: string
  sortField?: string
  sortOrder?: string
  categoryId?: number
  status?: string
  userId?: number
  tagId?: number
  minPrice?: number
  maxPrice?: number
  sortBy?: string
}

export interface CategoryTreeVO {
  id: number
  name: string
  sort: number
  children: CategoryTreeVO[]
}

export interface PromptTag {
  id: number
  name: string
}

export interface OrderVO {
  orderNo: string
  promptTitle: string
  amount: number
  status: string
  createTime: string
  payTime: string
  sellerNickname: string
  buyerNickname: string
}

export interface ReviewVO {
  id: number
  userId: number
  userNickname: string
  userAvatar: string
  promptId: number
  rating: number
  content: string
  createTime: string
}

export interface ReviewCreateRequest {
  promptId: number
  orderId?: number
  rating: number
  content?: string
}

export interface NotificationVO {
  id: number
  type: string
  title: string
  content: string
  isRead: number
  bizType: string
  bizId: number
  createTime: string
}

export interface UserBalance {
  id: number
  userId: number
  balance: number
  freeze: number
}

export interface AgentOptimizeRequest {
  prompt: string
  intent?: string
  style?: string
}

export interface AgentClassifyRequest {
  title: string
  description?: string
  content: string
}

export interface AgentEvaluateRequest {
  prompt_content: string
  test_cases?: { input: string; expected: string }[]
}

export interface AgentGenerateRequest {
  intent: string
  variables?: string[]
}

export interface AgentDebugRequest {
  prompt_content: string
  variables?: Record<string, string>
  model?: string
}
