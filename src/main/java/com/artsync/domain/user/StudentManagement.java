package com.artsync.domain.user;

import java.util.HashSet;
import java.util.Set;

public class StudentManagement {

    private final Set<String> students = new HashSet<>();

    public void addStudent(String name) {
        if (students.contains(name)) {
            throw new IllegalArgumentException("이미 존재하는 학생입니다: " + name);
        }
        students.add(name);
    }

    public void removeStudent(String name) {
        if (!students.contains(name)) {
            throw new IllegalArgumentException("존재하지 않는 학생입니다: " + name);
        }
        students.remove(name);
    }

    public boolean hasStudent(String name) {
        return students.contains(name);
    }
}
