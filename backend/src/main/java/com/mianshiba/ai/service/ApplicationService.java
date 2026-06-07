package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationListQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationStatusUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationUpdateRequest;
import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;

import java.util.List;

/**
 * 求职投递服务接口
 */
public interface ApplicationService {

    JobApplicationVO createApplication(String authorizationHeader, ApplicationCreateRequest request);

    List<JobApplicationVO> listApplications(String authorizationHeader, ApplicationListQueryRequest request);

    JobApplicationVO getApplication(String authorizationHeader, Long id);

    JobApplicationVO updateApplication(String authorizationHeader, Long id, ApplicationUpdateRequest request);

    JobApplicationVO updateStatus(String authorizationHeader, Long id, ApplicationStatusUpdateRequest request);

    void deleteApplication(String authorizationHeader, Long id);

    ApplicationStatsVO getStats(String authorizationHeader);

    ApplicationTodoVO createApplicationTodo(String authorizationHeader, Long applicationId, ApplicationTodoCreateRequest request);

    ApplicationTodoVO createGlobalTodo(String authorizationHeader, ApplicationTodoCreateRequest request);

    List<ApplicationTodoVO> listTodos(String authorizationHeader, ApplicationTodoQueryRequest request);

    ApplicationTodoVO updateTodo(String authorizationHeader, Long todoId, ApplicationTodoUpdateRequest request);

    ApplicationTodoVO completeTodo(String authorizationHeader, Long todoId);

    ApplicationTodoVO reopenTodo(String authorizationHeader, Long todoId);

    void deleteTodo(String authorizationHeader, Long todoId);
}