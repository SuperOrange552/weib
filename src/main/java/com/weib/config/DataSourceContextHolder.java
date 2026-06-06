package com.weib.config;

/**
 * 数据源上下文持有者
 *
 * 使用 ThreadLocal 保存当前线程应使用的数据源类型，
 * 确保同一事务内的所有操作路由到正确的数据源。
 *
 * 注意：每个请求处理完成后必须清理，防止线程池复用导致的数据源泄漏。
 */
public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    /** 设置当前线程的数据源类型 */
    public static void set(DataSourceType type) {
        CONTEXT.set(type);
    }

    /** 获取当前线程的数据源类型，默认返回 MASTER（安全兜底） */
    public static DataSourceType get() {
        return CONTEXT.get() != null ? CONTEXT.get() : DataSourceType.MASTER;
    }

    /** 清理当前线程的数据源类型（必须调用，防止内存泄漏） */
    public static void clear() {
        CONTEXT.remove();
    }
}
