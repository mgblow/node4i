/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.core;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

public enum AccessLevel {

    CurrentRead(0x01),
    CurrentWrite(0x02),
    HistoryRead(0x04),
    HistoryWrite(0x08),
    SemanticChange(0x10),
    StatusWrite(0x20),
    TimestampWrite(0x40);

    public static final ImmutableSet<AccessLevel> NONE = ImmutableSet.of();
    public static final ImmutableSet<AccessLevel> READ_ONLY = ImmutableSet.of(CurrentRead);
    public static final ImmutableSet<AccessLevel> WRITE_ONLY = ImmutableSet.of(CurrentWrite);
    public static final ImmutableSet<AccessLevel> READ_WRITE = ImmutableSet.of(CurrentRead, CurrentWrite);
    public static final ImmutableSet<AccessLevel> HISTORY_READ_ONLY = ImmutableSet.of(HistoryRead);
    public static final ImmutableSet<AccessLevel> HISTORY_READ_WRITE = ImmutableSet.of(HistoryRead, HistoryWrite);

    private final int value;

    AccessLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Create a Set of {@link AccessLevel} from the {@code accessLevel} value.
     *
     * @param accessLevel a {@link UByte} value.
     * @return the Set of {@link AccessLevel} indicated by {@code accessValue}.
     */
    public static EnumSet<AccessLevel> fromValue(UByte accessLevel) {
        return fromValue(accessLevel.intValue());
    }

    /**
     * Create a Set of {@link AccessLevel} from the {@code accessLevel} value.
     *
     * @param accessLevel an int value.
     * @return the Set of {@link AccessLevel} indicated by {@code accessValue}.
     */
    public static EnumSet<AccessLevel> fromValue(int accessLevel) {
        EnumSet<AccessLevel> e = EnumSet.noneOf(AccessLevel.class);
        for (AccessLevel al : values()) {
            if ((al.value & accessLevel) != 0) {
                e.add(al);
            }
        }
        return e;
    }

    /**
     * Convert {@code levels} to a {@link UByte} value for the AccessLevel or UserAccessLevel attribute.
     *
     * @param levels the {@link AccessLevel}s to include in the value.
     * @return a value for the AccessLevel or UserAccessLevel attribute.
     */
    public static UByte toValue(AccessLevel... levels) {
        short result = 0;
        for (AccessLevel level : levels) result |= level.value;
        return ubyte(result);
    }

    /**
     * Convert {@code levels} to a {@link UByte} value for the AccessLevel or UserAccessLevel attribute.
     *
     * @param levels the {@link AccessLevel}s to include in the value.
     * @return a value for the AccessLevel or UserAccessLevel attribute.
     */
    public static UByte toValue(Set<AccessLevel> levels) {
        int result = 0;
        for (AccessLevel level : levels) result |= level.value;
        return ubyte(result);
    }

}
