package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.user.UserLoginRequest;
import com.mianshiba.ai.model.dto.user.UserRegisterRequest;
import com.mianshiba.ai.model.dto.user.UserUpdateProfileRequest;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.LoginUserVO;
import com.mianshiba.ai.model.vo.UserLoginVO;
import com.mianshiba.ai.service.UserService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,32}$");
    private static final Set<String> VALID_JOB_STATUSES = Set.of("", "looking", "open", "not_looking");
    private static final String DEFAULT_USER_ROLE = "user";
    private static final int USER_STATUS_NORMAL = 0;
    private static final int USER_STATUS_DISABLED = 1;
    private static final int NOT_DELETED = 0;

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterRequest request) {
        validateRegisterRequest(request);
        Long count = userMapper.selectCount(Wrappers.lambdaQuery(User.class)
                .eq(User::getUserAccount, request.getUserAccount()));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        User user = new User();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(passwordEncoder.encode(request.getUserPassword()));
        user.setUserName("");
        user.setUserAvatar("");
        user.setUserRole(DEFAULT_USER_ROLE);
        user.setUserStatus(USER_STATUS_NORMAL);
        user.setEmail("");
        user.setPhone("");
        user.setTargetPosition("");
        user.setTechDirection("");
        user.setWorkYears(0);
        user.setCity("");
        user.setJobStatus("");
        user.setIsDelete(NOT_DELETED);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public UserLoginVO login(UserLoginRequest request) {
        validateAccountAndPassword(request == null ? null : request.getUserAccount(),
                request == null ? null : request.getUserPassword());
        User user = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getUserAccount, request.getUserAccount())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getUserPassword(), user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }
        ensureAvailableUser(user);
        String token = jwtUtils.generateToken(user.getId(), user.getUserAccount(), user.getUserRole());
        return new UserLoginVO(token, toLoginUserVO(user));
    }

    @Override
    public LoginUserVO getCurrentUser(String authorizationHeader) {
        User user = getAvailableUserByAuthorization(authorizationHeader);
        return toLoginUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUserVO updateProfile(String authorizationHeader, UserUpdateProfileRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = getAvailableUserByAuthorization(authorizationHeader);
        setIfNotNull(user::setUserName, request.getUserName());
        setIfNotNull(user::setUserAvatar, request.getUserAvatar());
        setIfNotNull(user::setTargetPosition, request.getTargetPosition());
        setIfNotNull(user::setTechDirection, request.getTechDirection());
        if (request.getWorkYears() != null) {
            user.setWorkYears(request.getWorkYears());
        }
        setIfNotNull(user::setCity, request.getCity());
        if (request.getJobStatus() != null) {
            String jobStatus = request.getJobStatus().trim();
            if (!VALID_JOB_STATUSES.contains(jobStatus)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "求职状态不合法");
            }
            user.setJobStatus(jobStatus);
        }
        userMapper.updateById(user);
        return toLoginUserVO(user);
    }

    private void validateRegisterRequest(UserRegisterRequest request) {
        validateAccountAndPassword(request == null ? null : request.getUserAccount(),
                request == null ? null : request.getUserPassword());
        if (!request.getUserPassword().equals(request.getCheckPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    private void validateAccountAndPassword(String userAccount, String userPassword) {
        if (!StringUtils.hasText(userAccount) || !ACCOUNT_PATTERN.matcher(userAccount).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式不合法");
        }
        if (!StringUtils.hasText(userPassword) || userPassword.length() < 8 || userPassword.length() > 64) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须为 8-64 位");
        }
    }

    private User getAvailableUserByAuthorization(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        ensureAvailableUser(user);
        return user;
    }

    private void ensureAvailableUser(User user) {
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(USER_STATUS_DISABLED).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
    }

    private void setIfNotNull(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private LoginUserVO toLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserRole(user.getUserRole());
        loginUserVO.setUserStatus(user.getUserStatus());
        loginUserVO.setEmail(user.getEmail());
        loginUserVO.setPhone(user.getPhone());
        loginUserVO.setTargetPosition(user.getTargetPosition());
        loginUserVO.setTechDirection(user.getTechDirection());
        loginUserVO.setWorkYears(user.getWorkYears());
        loginUserVO.setCity(user.getCity());
        loginUserVO.setJobStatus(user.getJobStatus());
        loginUserVO.setCreateTime(user.getCreateTime());
        return loginUserVO;
    }
}
