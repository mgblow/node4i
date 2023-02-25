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

import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LocalizedTextSerializationTest extends BinarySerializationFixture {

    @DataProvider
    public Object[][] getLocalizedTexts() {
        return new Object[][]{
            {new LocalizedText(null, null)},
            {new LocalizedText("locale", null)},
            {new LocalizedText(null, "text")},
            {LocalizedText.english("hello, world!")},
        };
    }

    @Test(dataProvider = "getLocalizedTexts", description = "LocalizedText is round-trip serializable.")
    public void testLocalizedText(LocalizedText localizedText) throws Exception {
        writer.writeLocalizedText(localizedText);
        LocalizedText decoded = reader.readLocalizedText();

        assertEquals(decoded, localizedText);
    }

}
