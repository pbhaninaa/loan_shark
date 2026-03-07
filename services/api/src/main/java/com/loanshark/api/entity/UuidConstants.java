package com.loanshark.api.entity;

import java.util.UUID;

/** Fixed UUIDs for single-row config tables (see V13 migration). */
public final class UuidConstants {

    public static final UUID BUSINESS_CAPITAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID LOAN_INTEREST_SETTINGS_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private UuidConstants() {}
}
