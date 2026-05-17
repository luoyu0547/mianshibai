package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private UserMapper userMapper;

    private BCryptPasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtUtils);
    }

    @Test
    void registerCreatesUserWithEncryptedPassword() {
        UserRegisterRequest request = registerRequest("developer_001", "password123", "password123");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1001L);
            return 1;
        });

        Long userId = userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User savedUser = captor.getValue();
        assertThat(userId).isEqualTo(1001L);
        assertThat(savedUser.getUserAccount()).isEqualTo("developer_001");
        assertThat(savedUser.getUserPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getUserPassword())).isTrue();
        assertThat(savedUser.getUserRole()).isEqualTo("user");
        assertThat(savedUser.getUserStatus()).isZero();
    }

    @Test
    void registerThrowsWhenAccountExists() {
        UserRegisterRequest request = registerRequest("developer_001", "password123", "password123");
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号已存在")
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerThrowsWhenPasswordsDifferent() {
        UserRegisterRequest request = registerRequest("developer_001", "password123", "password456");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("两次输入的密码不一致")
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginReturnsTokenAndMaskedUser() {
        User user = normalUser();
        user.setUserPassword(passwordEncoder.encode("password123"));
        when(userMapper.selectOne(any())).thenReturn(user);

        UserLoginVO loginVO = userService.login(loginRequest("developer_001", "password123"));

        assertThat(loginVO.getToken()).isNotBlank();
        assertThat(loginVO.getUser().getId()).isEqualTo(1001L);
        assertThat(loginVO.getUser().getUserAccount()).isEqualTo("developer_001");
    }

    @Test
    void loginThrowsWhenPasswordWrong() {
        User user = normalUser();
        user.setUserPassword(passwordEncoder.encode("password123"));
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login(loginRequest("developer_001", "wrongpass")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("账号或密码错误")
                .extracting("code")
                .isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    @Test
    void loginThrowsWhenUserDisabled() {
        User user = normalUser();
        user.setUserPassword(passwordEncoder.encode("password123"));
        user.setUserStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThatThrownBy(() -> userService.login(loginRequest("developer_001", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
    }

    @Test
    void getCurrentUserReturnsMaskedUser() {
        User user = normalUser();
        when(userMapper.selectById(1001L)).thenReturn(user);
        String authorization = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");

        LoginUserVO loginUserVO = userService.getCurrentUser(authorization);

        assertThat(loginUserVO.getId()).isEqualTo(1001L);
        assertThat(loginUserVO.getUserAccount()).isEqualTo("developer_001");
    }

    @Test
    void updateProfileUpdatesAllowedFields() {
        User user = normalUser();
        when(userMapper.selectById(1001L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        String authorization = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        UserUpdateProfileRequest request = new UserUpdateProfileRequest();
        request.setUserName("后端开发者");
        request.setTargetPosition("Java 后端工程师");
        request.setTechDirection("Java/Spring Boot/AI 应用");
        request.setWorkYears(3);
        request.setCity("上海");
        request.setJobStatus("looking");

        LoginUserVO loginUserVO = userService.updateProfile(authorization, request);

        assertThat(loginUserVO.getUserName()).isEqualTo("后端开发者");
        assertThat(loginUserVO.getTargetPosition()).isEqualTo("Java 后端工程师");
        assertThat(loginUserVO.getWorkYears()).isEqualTo(3);
        verify(userMapper).updateById(user);
    }

    private UserRegisterRequest registerRequest(String account, String password, String checkPassword) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount(account);
        request.setUserPassword(password);
        request.setCheckPassword(checkPassword);
        return request;
    }

    private UserLoginRequest loginRequest(String account, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount(account);
        request.setUserPassword(password);
        return request;
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("developer_001");
        user.setUserName("开发者");
        user.setUserAvatar("");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setEmail("");
        user.setPhone("");
        user.setTargetPosition("");
        user.setTechDirection("");
        user.setWorkYears(0);
        user.setCity("");
        user.setJobStatus("");
        user.setIsDelete(0);
        return user;
    }
}
