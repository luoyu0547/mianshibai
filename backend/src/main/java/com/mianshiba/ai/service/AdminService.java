package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;

import java.util.List;

public interface AdminService {

    AdminOverviewVO getOverview(String authorizationHeader);

    List<AdminUserListItemVO> listUsers(String authorizationHeader, AdminUserQueryRequest request);

    AdminUserDetailVO getUserDetail(String authorizationHeader, Long userId);

    AdminUserDetailVO disableUser(String authorizationHeader, Long userId);

    AdminUserDetailVO enableUser(String authorizationHeader, Long userId);

    AdminUserDetailVO updateUserRole(String authorizationHeader, Long userId, AdminUserRoleUpdateRequest request);
}
