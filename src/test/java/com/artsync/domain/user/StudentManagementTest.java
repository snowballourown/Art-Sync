package com.artsync.domain.user;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StudentManagementTest {

    private static StudentManagement manager;

    @BeforeAll
    static void setUpBeforeClass() {
        manager = new StudentManagement();
    }

    @Test
    @Order(1)
    void testAddStudent() {
        manager.addStudent("홍길동");
        assertTrue(manager.hasStudent("홍길동"));
    }

    @Test
    @Order(2)
    void testDuplicateAddStudent() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.addStudent("홍길동");
        });
    }

    @Test
    @Order(3)
    void testRemoveStudent() {
        manager.removeStudent("홍길동");

        assertFalse(manager.hasStudent("홍길동"));
    }

    @Test
    @Order(4)
    void testRemoveNonExistingStudent() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.removeStudent("홍길동");
        });
    }
}