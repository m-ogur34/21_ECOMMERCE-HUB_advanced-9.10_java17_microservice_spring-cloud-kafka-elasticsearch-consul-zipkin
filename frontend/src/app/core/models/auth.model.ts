// Auth servisi için type tanımları — API sözleşmesiyle eşleşmeli

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;     // "Bearer"
  expiresIn: number;     // ms cinsinden erişim token süresi
  email: string;
  roles: string[];
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

// JWT payload decode'u için (jose veya manuel decode)
export interface JwtPayload {
  sub: string;           // email
  roles: string[];
  iat: number;           // issued at
  exp: number;           // expiration
}
