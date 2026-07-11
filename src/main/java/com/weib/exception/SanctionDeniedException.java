package com.weib.exception;

/** 当前用户存在有效处罚，禁止执行对应业务操作。 */
public class SanctionDeniedException extends RuntimeException {

    private final String sanctionType;

    public SanctionDeniedException(String sanctionType) {
        super("当前账号暂时无法执行该操作");
        this.sanctionType = sanctionType;
    }

    public String getSanctionType() {
        return sanctionType;
    }
}
