package com.weib.dto.admin;

import lombok.Data;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 通用分页响应 DTO
 *
 * 将 Spring Data Page 对象转换为前端友好的 JSON 结构。
 * 使用静态工厂方法 PageResponse.of(page) 创建实例。
 *
 * @param <T> 分页内容类型
 */
@Data
public class PageResponse<T> {
    /** 当前页数据列表 */
    private List<T> content;
    /** 总记录数 */
    private long totalElements;
    /** 总页数 */
    private int totalPages;
    /** 当前页码（0-based） */
    private int currentPage;
    /** 每页大小 */
    private int pageSize;

    /**
     * 从 Spring Data Page 对象构建 PageResponse
     *
     * @param page Spring Data 分页结果
     * @param <T>  分页内容类型
     * @return PageResponse 实例
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setCurrentPage(page.getNumber());
        response.setPageSize(page.getSize());
        return response;
    }
}
