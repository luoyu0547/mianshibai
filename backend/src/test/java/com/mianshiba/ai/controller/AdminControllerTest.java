package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;
import com.mianshiba.ai.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @Test
    void getOverview_returnsOverview() throws Exception {
        AdminOverviewVO overview = new AdminOverviewVO();
        overview.setTotalUsers(10L);
        when(adminService.getOverview("Bearer token")).thenReturn(overview);

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalUsers").value(10));
    }

    @Test
    void listUsers_passesQuery() throws Exception {
        AdminUserListItemVO user = new AdminUserListItemVO();
        user.setId(2L);
        user.setUserAccount("candidate");
        when(adminService.listUsers(eq("Bearer token"), any(AdminUserQueryRequest.class))).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer token")
                        .param("keyword", "candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2));
    }

    @Test
    void getUserDetail_returnsDetail() throws Exception {
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        when(adminService.getUserDetail("Bearer token", 2L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/users/2").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void disableUser_callsService() throws Exception {
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        detail.setUserStatus(1);
        when(adminService.disableUser("Bearer token", 2L)).thenReturn(detail);

        mockMvc.perform(put("/api/admin/users/2/disable").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value(1));
    }

    @Test
    void enableUser_callsService() throws Exception {
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        detail.setUserStatus(0);
        when(adminService.enableUser("Bearer token", 2L)).thenReturn(detail);

        mockMvc.perform(put("/api/admin/users/2/enable").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value(0));
    }

    @Test
    void updateUserRole_callsService() throws Exception {
        AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
        request.setUserRole("admin");
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        detail.setUserRole("admin");
        when(adminService.updateUserRole(eq("Bearer token"), eq(2L), any(AdminUserRoleUpdateRequest.class))).thenReturn(detail);

        mockMvc.perform(put("/api/admin/users/2/role")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userRole").value("admin"));

        verify(adminService).updateUserRole(eq("Bearer token"), eq(2L), any(AdminUserRoleUpdateRequest.class));
    }
}
