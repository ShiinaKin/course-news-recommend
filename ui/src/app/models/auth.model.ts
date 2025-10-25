export interface UserTag {
  id: number;
  name: string;
  weight: number;
}

export interface UserProfile {
  id: number;
  username: string;
  nickname: string;
  tags: UserTag[];
}

export interface SimpleMessageResponse {
  message: string;
}

