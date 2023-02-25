/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.stack.core.serialization.binary;

import io.netty.buffer.ByteBuf;
import org.eclipse.milo.opcua.stack.core.serialization.OpcUaBinaryStreamDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.OpcUaBinaryStreamEncoder;
import org.eclipse.milo.opcua.stack.core.serialization.TestSerializationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.util.BufferUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;
import static org.testng.Assert.assertEquals;

public class DataValueSerializationTest {

    private final OpcUaBinaryStreamEncoder encoder = new OpcUaBinaryStreamEncoder(new TestSerializationContext());
    private final OpcUaBinaryStreamDecoder decoder = new OpcUaBinaryStreamDecoder(new TestSerializationContext());

    @Test(dataProvider = "getValues")
    public void testDataValueRoundTrip(DataValue value) {
        ByteBuf buffer = BufferUtil.pooledBuffer();
        encoder.setBuffer(buffer);
        encoder.writeDataValue(value);

        decoder.setBuffer(buffer);
        DataValue decodedValue = decoder.readDataValue();

        assertEquals(decodedValue, value);
    }

    @DataProvider
    public Object[][] getValues() {
        return new Object[][]{
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.GOOD,
                    DateTime.now(),
                    ushort(1),
                    DateTime.now(),
                    ushort(1))
            },
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.GOOD,
                    DateTime.now(),
                    null,
                    DateTime.now(),
                    ushort(1))
            },
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.GOOD,
                    DateTime.now(),
                    ushort(1),
                    DateTime.now(),
                    null)
            },
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.GOOD,
                    DateTime.now(),
                    DateTime.now())
            },
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.GOOD,
                    DateTime.MIN_VALUE,
                    DateTime.now())
            },
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.GOOD,
                    DateTime.now(),
                    DateTime.MIN_VALUE)
            },
            {
                new DataValue(
                    new Variant(1),
                    StatusCode.GOOD,
                    DateTime.now(),
                    DateTime.MIN_VALUE)
            },
            {
                new DataValue(
                    new Variant(1),
                    StatusCode.BAD,
                    DateTime.now(),
                    DateTime.MIN_VALUE)
            },
            {
                new DataValue(
                    Variant.NULL_VALUE,
                    StatusCode.BAD,
                    DateTime.now(),
                    DateTime.MIN_VALUE)
            }
        };
    }

}
