package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;

/**
 * 用户服务
 */
public interface UserService {

    Long register(UserRegisterRequest request);

    UserLoginVO login(UserLoginRequest request);

    LoginUserVO getCurrentUser(String authorizationHeader);

    LoginUserVO updateProfile(String authorizationHeader, UserUpdateProfileRequest request);
}
