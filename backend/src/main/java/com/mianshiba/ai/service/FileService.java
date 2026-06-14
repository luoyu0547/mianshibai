package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 */
public interface FileService {

    FileUploadVO uploadAvatar(MultipartFile file);
}
