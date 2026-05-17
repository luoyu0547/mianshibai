package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerReturnsUserId() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount("developer_001");
        request.setUserPassword("password123");
        request.setCheckPassword("password123");
        when(userService.register(any(UserRegisterRequest.class))).thenReturn(1001L);

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1001));
    }

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserAccount("developer_001");
        request.setUserPassword("password123");
        LoginUserVO loginUserVO = loginUserVO();
        when(userService.login(any(UserLoginRequest.class))).thenReturn(new UserLoginVO("token-value", loginUserVO));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").value("token-value"))
                .andExpect(jsonPath("$.data.user.userAccount").value("developer_001"));
    }

    @Test
    void currentReturnsLoginUser() throws Exception {
        String authorization = "Bearer token-value";
        when(userService.getCurrentUser(authorization)).thenReturn(loginUserVO());

        mockMvc.perform(get("/api/user/current")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userAccount").value("developer_001"));
    }

    @Test
    void updateProfileReturnsUpdatedUser() throws Exception {
        String authorization = "Bearer token-value";
        UserUpdateProfileRequest request = new UserUpdateProfileRequest();
        request.setUserName("后端开发者");
        request.setTargetPosition("Java 后端工程师");
        LoginUserVO loginUserVO = loginUserVO();
        loginUserVO.setUserName("后端开发者");
        loginUserVO.setTargetPosition("Java 后端工程师");
        when(userService.updateProfile(eq(authorization), any(UserUpdateProfileRequest.class))).thenReturn(loginUserVO);

        mockMvc.perform(put("/api/user/profile")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userName").value("后端开发者"))
                .andExpect(jsonPath("$.data.targetPosition").value("Java 后端工程师"));
    }

    @Test
    void registerReturnsParamsErrorWhenAccountInvalid() throws Exception {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserAccount("bad-account!");
        request.setUserPassword("password123");
        request.setCheckPassword("password123");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    private LoginUserVO loginUserVO() {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(1001L);
        loginUserVO.setUserAccount("developer_001");
        loginUserVO.setUserName("开发者");
        loginUserVO.setUserRole("user");
        loginUserVO.setUserStatus(0);
        loginUserVO.setWorkYears(0);
        loginUserVO.setJobStatus("");
        return loginUserVO;
    }
}
