# 04) BFF 완전형: 원타임 세션 코드 기반 FE 상호작용 쿡북

목표: 브라우저에는 토큰이 전혀 나타나지 않도록 하고, 브라우저는 일회용 `session_code`만 전달합니다. 토큰 교환은 Next.js API Route(서버)와 인증 서비스 간 서버-사이드로만 수행합니다.

## 공격 표면 변화 및 안전성 비교

| 공격 시나리오 | 현재: 폼 POST로 토큰 전달 | BFF 완전형: 세션 코드만 전달 | 설명 |
|---|---|---|---|
| URL/리퍼러로 토큰 유출 | ✅ 차단 (본문 전송) | ✅ 차단 | 토큰이 쿼리/해시에 실리지 않음 |
| 서버/프록시 로그 유출 | 🟡 설정에 따라 위험 (본문 로깅 시) | ✅ 더 안전 (서버 간 교환, 코드 단기) | 서버에서 요청 본문 로깅 금지 권장 |
| 브라우저 DevTools에서 토큰 노출 | ❌ 보임 (요청 페이로드에 토큰) | ✅ 안 보임 (코드만 존재) | BFF는 토큰이 JS/DevTools에 나타나지 않음 |
| XSS/서드파티 스크립트 탈취 | 🟡 가능 (제출 직전 토큰 읽힘) | 🟡 제한적 (세션 코드 탈취 가능, 단 TTL·1회성) | 코드 탈취 리스크는 짧은 TTL·단일사용·서버검증으로 완화 |
| 브라우저 확장프로그램에 의한 수집 | 🟡 토큰 노출 가능 | ✅ 토큰 미노출 | 요청 본문/헤더 수집 위험 감소 |
| 네트워크 MITM (TLS 가정) | ✅ 방어 | ✅ 방어 | HTTPS 전제 |
| 리플레이/재사용 | 🟡 토큰 훔치면 재사용 가능 | ✅ 세션 코드는 1회성·단기, 토큰은 서버 보관 | 서버-사이드 쿠키/로테이션 권장 |
| CSRF (쿠키 사용 단계) | 🟡 방어 필요 | 🟡 방어 필요 | Origin/Referer 검증 또는 CSRF 토큰 적용 |

요약: 현재 방식은 URL·로그·리퍼러 유출을 줄이지만, 토큰이 DevTools/확장프로그램/XSS에 노출될 수 있습니다. BFF 완전형은 브라우저에 토큰이 전혀 나타나지 않아 훨씬 안전하며, 남는 리스크(세션 코드)는 TTL·단일사용·서버 검증으로 크게 완화됩니다.

### 현재 구현(인증서버에서 Next.js 서버로 POST 자동 제출 폼 응답)의 기본 안전성과 남아있는 잠재적 보안 위협

- 현재 구현된 내용도 일반적인 상황에 대한 안전성을 충분히 구현함
  - 토큰이 **URL/리퍼러/서버 접근 로그**에 남지 않음(본문 POST 1회 전송)
  - **TLS(HTTPS)** 전제로 네트워크 구간에서 평문 노출 없음
  - **게이트웨이 전용 접근**(X-Gateway-Internal)으로 백엔드 직접 우회 차단
  - 브라우저 저장소(localStorage/쿠키 Non-HttpOnly)에 **영구 저장하지 않음**
  - Next 서버가 HttpOnly 쿠키로 보관, 쿠키는 HttpOnly(JS로 임의접근 불가)+Secure(HTTPS 강제)+SameSite(Lax)
  - 권장 보안 헤더(CSP, no-store, no-referrer) 적용 시 자동 제출 페이지에서 **외부 스크립트 실행 최소화**

- 다음과 같은 브라우저 환경 침해 발생 시 현재 구현에 대한 보안 위협 발생
  - **브라우저 메모리에 토큰 노출**: 네트워크 요청이 발생하므로 브라우저 메모리에서 처리됨. 단, DevTools/네트워크 페이로드 노출은 ‘사용자 본인 브라우저’ 내부로 한정
  - **XSS 또는 서드파티 스크립트 오염**: 동일 오리진 페이지에 악성 스크립트가 주입되어 폼 제출 직전 토큰 값을 읽어 외부로 전송
  - **악성 브라우저 확장프로그램**: 네트워크 요청/폼 데이터를 수집하는 확장 설치
  - **과도한 로깅/수집 도구 설정**: APM/분석 도구가 요청 본문을 수집하도록 오설정
  - **TLS 미적용/약화된 신뢰 체계**: 개발용 루트 인증서로 TLS를 가로채는 환경(기업 프록시 등)

- 운영 시 권장 보완
  - 자동 제출 응답에 `Cache-Control: no-store`, `Referrer-Policy: no-referrer`, **강한 CSP(form-action 화이트리스트)** 적용
  - 로깅/모니터링 도구에서 **요청 본문 PII 스크러빙** 활성화, 토큰 로깅 금지
  - 토큰 수명 단축, 리프레시 로테이션, 실패 시 알림/차단(레이트 리밋)

## 1. 시퀀스 개요

1) 사용자 → GET Gateway `/api/auth/google`
2) Google → Gateway → Auth 콜백 `/api/v1/auth/login/oauth2/code/google`
3) Auth: Google 코드 교환(서버) → JWT 번들 생성 → `session_code` 발급(1회성/TTL≈60s)
4) Auth → 브라우저: 자동 제출 폼으로 `POST FRONTEND_BASE_URL/api/session/finalize` (payload: `session_code`)
5) Next `/api/session/finalize`: 서버-사이드로 Auth `POST /api/v1/auth/session/finalize` 호출 → 토큰 수신 → HttpOnly 쿠키 저장 → 302 리다이렉트(`/` 등)
6) 이후 API는 Next Route Handler가 쿠키를 읽어 서버-사이드로 Authorization 헤더를 추가하여 Gateway에 프록시

## 2. Auth 서비스 변경 포인트

- 세션 코드 저장소(권장: Redis) 도입. 개발용으로 인메모리 대체 가능
- 콜백은 `session_code`만 브라우저로 전달(토큰 직접 전달 금지)
- 세션 교환 엔드포인트 `POST /api/v1/auth/session/finalize` 추가

### 2.1 SessionCodeService (Redis)

```java
@Service
@RequiredArgsConstructor
public class SessionCodeService {
  private static final String KEY_PREFIX = "session_code:";
  private static final Duration TTL = Duration.ofSeconds(60);
  private final StringRedisTemplate redis;

  public String issueSessionCode(String tokenBundleJson) {
    String code = UUID.randomUUID().toString().replace("-", "");
    redis.opsForValue().set(KEY_PREFIX + code, tokenBundleJson, TTL);
    return code;
  }

  public String consumeTokenBundle(String code) {
    String key = KEY_PREFIX + code;
    String json = redis.opsForValue().get(key);
    if (json != null) redis.delete(key); // single-use
    return json; // null: 만료/미존재/이미 사용됨
  }
}
```

### 2.2 AuthController 콜백 (세션 코드 발급 후 폼 반환)

```java
// 의존성: private final SessionCodeService sessionCodeService;

@GetMapping("/login/oauth2/code/google")
public void googleCallback(@RequestParam(value = "code", required = false) String code,
                           HttpServletResponse response) throws IOException {
  if (code == null || code.isBlank()) {
    response.sendRedirect(frontendBaseUrl + "/auth/callback?error=missing_code");
    return;
  }
  try {
    AuthResponse tokens = authService.signInWithGoogle(code); // 서버-사이드 교환
    String tokenJson = String.format(
      "{\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"tokenType\":\"%s\",\"expiresIn\":%d}",
      escapeJson(tokens.getAccessToken()), escapeJson(tokens.getRefreshToken()),
      escapeJson(tokens.getTokenType()), tokens.getExpiresIn()
    );
    String sessionCode = sessionCodeService.issueSessionCode(tokenJson);

    String html = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Signing in...</title></head><body>"
      + "<form id='f' method='POST' action='" + escapeHtml(frontendBaseUrl + "/api/session/finalize") + "'>"
      + "<input type='hidden' name='session_code' value='" + escapeHtml(sessionCode) + "'/>"
      + "</form><script>document.getElementById('f').submit();</script></body></html>";

    byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
    response.setStatus(200);
    response.setContentType("text/html; charset=UTF-8");
    response.setContentLength(bytes.length);
    response.getOutputStream().write(bytes);
  } catch (Exception e) {
    response.sendRedirect(frontendBaseUrl + "/auth/callback?error=auth_failed");
  }
}
```

### 2.3 세션 교환 엔드포인트

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class SessionController {
  private final SessionCodeService sessionCodeService;

  @PostMapping("/session/finalize")
  public ResponseEntity<?> finalizeSession(@RequestBody Map<String, String> body) {
    String code = body.get("sessionCode");
    if (code == null || code.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "missing_session_code"));
    String tokenJson = sessionCodeService.consumeTokenBundle(code);
    if (tokenJson == null) return ResponseEntity.status(400).body(Map.of("error", "invalid_or_expired_session_code"));
    return ResponseEntity.ok(tokenJson);
  }
}
```

보안: `GatewayOnlyFilter`로 보호되며, 레이트 리밋/감사 로깅/토큰 마스킹 권장.

## 3. Next.js 최종화 라우트

```ts
// app/api/session/finalize/route.ts
import { NextResponse } from 'next/server'

export async function POST(req: Request) {
  const form = await req.formData()
  const code = String(form.get('session_code') || '')
  if (!code) return NextResponse.redirect(new URL('/login?error=missing_code', req.url))

  const r = await fetch('http://localhost:8000/api/auth/session/finalize', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionCode: code }), cache: 'no-store',
  })
  if (!r.ok) return NextResponse.redirect(new URL('/login?error=finalize_failed', req.url))

  const { accessToken, refreshToken } = await r.json()
  const res = NextResponse.redirect(new URL('/', req.url))
  if (accessToken) res.cookies.set('access_token', accessToken, { httpOnly: true, secure: true, sameSite: 'lax', path: '/' })
  if (refreshToken) res.cookies.set('refresh_token', refreshToken, { httpOnly: true, secure: true, sameSite: 'lax', path: '/' })
  return res
}
```

## 4. 보안 체크리스트

- 세션 코드: TTL 60s, single-use, 사용 즉시 삭제
- 콜백 HTML: no-store, no-referrer, 강한 CSP(form-action 화이트리스트)
- 게이트웨이 전용 접근: `X-Gateway-Internal` 검증(+IP 화이트리스트 옵션)
- 토큰/코드 로깅 금지(마스킹)
- 토큰 수명 단축, 리프레시 로테이션
- CSRF: 쿠키 인증 구간에 Origin/Referer 또는 CSRF 토큰 적용

## 5. 마이그레이션 노트

- 현 상태: 콜백이 `access_token/refresh_token`을 `/api/session`에 직접 폼 POST
- 전환: 콜백이 `session_code`만 전달, `POST /api/v1/auth/session/finalize` 신설
- FE: 폼 대상 `/api/session/finalize`로 변경, 서버가 코드로 토큰 교환

## 6. 참고

- OAuth 2.0 BFF 패턴, PKCE(RFC 7636), OAuth for Native Apps(RFC 8252)
- CSP/CSRF 모범사례, 로그 민감정보 마스킹

