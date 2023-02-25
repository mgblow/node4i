package org.eclipse.milo.platform.compression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SwingDoorTest {
    SwingDoor swingDoor;

    @BeforeEach
    void setUp() {
        swingDoor=new SwingDoor(2);
    }

    @Test
    @DisplayName("Number , which is the first one , must be accepted.")
    void isAccepted_isFirstNumber_true() {
        final boolean accepted = swingDoor.isAccepted(2, 2);
        Assertions.assertTrue(accepted);
    }

    @Test
    @DisplayName("Number, which is the second one , must be accepted.")
    void isAccepted_isSecondNumber_true() {
        swingDoor.isAccepted(2, 2);
        final boolean accepted = swingDoor.isAccepted(5, 5);
        Assertions.assertTrue(accepted);
    }

    @Test
    @DisplayName("Number, which is in  the door, must be rejected.")
    void isAccepted_inDoor_false() {
        swingDoor.isAccepted(2, 2);
        swingDoor.isAccepted(5, 5);
        final boolean accepted = swingDoor.isAccepted(7, 10);
        Assertions.assertFalse(accepted);
    }

    @Test
    @DisplayName("Number, which is out of the door ,must be accepted.")
    void isAccepted_outOfDoor_true() {
        swingDoor.isAccepted(2, 2);
        swingDoor.isAccepted(5, 5);
        final boolean accepted = swingDoor.isAccepted(8, 100);
        Assertions.assertTrue(accepted);
    }


}