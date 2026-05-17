package com.mianshiba.ai.utils;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Test
    void generateTokenAndParseTokenReturnsClaims() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        String token = jwtUtils.generateToken(1001L, "developer_001", "user");
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);

        assertThat(token).isNotBlank();
        assertThat(claims.userId()).isEqualTo(1001L);
        assertThat(claims.userAccount()).isEqualTo("developer_001");
        assertThat(claims.userRole()).isEqualTo("user");
    }

    @Test
    void parseTokenThrowsWhenTokenInvalid() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        assertThatThrownBy(() -> jwtUtils.parseToken("bad-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
    }

    @Test
    void resolveTokenReturnsBearerToken() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        String token = jwtUtils.resolveToken("Bearer abc.def.ghi");

        assertThat(token).isEqualTo("abc.def.ghi");
    }

    @Test
    void resolveTokenThrowsWhenHeaderMissing() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));

        assertThatThrownBy(() -> jwtUtils.resolveToken(""))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
    }
}
