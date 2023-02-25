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
import io.netty.buffer.Unpooled;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.serialization.OpcUaBinaryStreamDecoder;
import org.eclipse.milo.opcua.stack.core.serialization.TestSerializationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceCounterDataType;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class VariantSerializationTest extends BinarySerializationFixture {

    @DataProvider(name = "VariantProvider")
    public Object[][] getVariants() {
        return new Object[][]{
            {new Variant(null)},
            {new Variant("hello, world")},
            {new Variant(42)},
            {new Variant(new Integer[]{0, 1, 2, 3})},
            {new Variant(new Integer[][]{{0, 1}, {2, 3}})},
            {new Variant(new Long[]{0L, 1L, 2L, 3L})},
            {new Variant(new Long[][]{{0L, 1L}, {2L, 3L}})},
            {new Variant(new UInteger[]{uint(0), uint(1), uint(2), uint(3)})},
            {new Variant(new UInteger[][]{{uint(0), uint(1)}, {uint(2), uint(3)}})},
            {new Variant(new Variant[]{new Variant(0), new Variant(1), new Variant(2)})}
        };
    }

    @Test(dataProvider = "VariantProvider")
    public void testVariantRoundTrip(Variant variant) {
        writer.writeVariant(variant);
        Variant decoded = reader.readVariant();

        assertEquals(decoded, variant);
    }

    @Test
    public void testVariant_UaStructure() {
        ServiceCounterDataType sc1 = new ServiceCounterDataType(
            uint(1),
            uint(2)
        );

        Variant v = new Variant(sc1);
        writer.writeVariant(v);
        Variant decoded = reader.readVariant();

        ExtensionObject extensionObject = (ExtensionObject) decoded.getValue();
        ServiceCounterDataType sc2 = (ServiceCounterDataType) extensionObject.decode(new TestSerializationContext());

        Assert.assertEquals(sc1.getTotalCount(), sc2.getTotalCount());
        Assert.assertEquals(sc1.getErrorCount(), sc2.getErrorCount());
    }

    @DataProvider(name = "PrimitiveArrayVariantProvider")
    public Object[][] getPrimitiveArrayVariants() {
        return new Object[][]{
            {new Variant(new int[]{0, 1, 2, 3}),
                new Variant(new Integer[]{0, 1, 2, 3})},

            {new Variant(new int[][]{{0, 1}, {2, 3}}),
                new Variant(new Integer[][]{{0, 1}, {2, 3}})},

            {new Variant(new long[]{0L, 1L, 2L, 3L}),
                new Variant(new Long[]{0L, 1L, 2L, 3L})},

            {new Variant(new long[][]{{0L, 1L}, {2L, 3L}}),
                new Variant(new Long[][]{{0L, 1L}, {2L, 3L}})}
        };
    }

    @Test(dataProvider = "PrimitiveArrayVariantProvider",
        description = "Test that after primitive array types given to variants come out as expected after encoding/decoding.")
    public void testPrimitiveArrayVariantRoundTrip(Variant variant, Variant expected) {
        writer.writeVariant(variant);
        Variant decoded = reader.readVariant();

        assertEquals(decoded, expected);
    }

    @Test(description = "Test that a Variant containing a null array encoded with a negative array size to indicate a null value decodes properly.")
    public void testNullArrayEncodedWithNegativeArraySize() {
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeByte(BuiltinDataType.Int16.getTypeId() | (1 << 7));
        buffer.writeIntLE(-1);

        OpcUaBinaryStreamDecoder reader = new OpcUaBinaryStreamDecoder(new TestSerializationContext());
        reader.setBuffer(buffer);

        Variant v = reader.readVariant();

        assertNotNull(v);
        assertNull(v.getValue());
    }

}
