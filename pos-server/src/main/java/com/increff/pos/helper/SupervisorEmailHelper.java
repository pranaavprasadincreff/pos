package com.increff.pos.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SupervisorEmailHelper {

    private static volatile Set<String> supervisors = Collections.emptySet();

    private SupervisorEmailHelper() {}

    // Called once from a Spring config initializer
    public static void init(String supervisorsCsv) {
        if (supervisorsCsv == null) {
            supervisors = Collections.emptySet();
            return;
        }
        supervisors = Arrays.stream(supervisorsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public static boolean isSupervisorEmail(String email) {
        if (email == null) return false;
        return supervisors.contains(email.trim().toLowerCase());
    }
}
