package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本快照 VO
 */
@Data
public class VersionSnapshotVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Integer version;

    private String changeSummary;

    private LocalDateTime createTime;

    private List<SectionVO> sections;
}
