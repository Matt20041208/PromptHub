package com.prompt.common.constant;

public class CommonConstants {

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    public static final String USER_STATUS_NORMAL = "1";
    public static final String USER_STATUS_DISABLED = "0";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String PROMPT_STATUS_DRAFT = "DRAFT";
    public static final String PROMPT_STATUS_PUBLISHED = "PUBLISHED";
    public static final String PROMPT_STATUS_OFFLINE = "OFFLINE";

    public static final String ORDER_STATUS_UNPAID = "UNPAID";
    public static final String ORDER_STATUS_PAID = "PAID";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_STATUS_REFUNDED = "REFUNDED";
}
