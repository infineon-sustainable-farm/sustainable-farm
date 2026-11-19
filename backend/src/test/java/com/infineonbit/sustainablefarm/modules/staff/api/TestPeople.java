package com.infineonbit.sustainablefarm.modules.staff.api;

/**
 * Test helper for the modules that reference people: the id of the active person carrying a
 * name, entered in the directory on first use as the owner module (visitor management Staff
 * screen) would. Consumers never create people; tests set them up here.
 */
public final class TestPeople {

    private TestPeople() {
    }

    /** {@code null} for a blank name. */
    public static Long idOf(PersonDirectory directory, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return null;
        }
        String name = fullName.trim();
        return directory.listActive().stream()
                .filter(person -> person.fullName().equalsIgnoreCase(name))
                .findFirst()
                .map(PersonDto::id)
                .orElseGet(() -> directory.create(new PersonDetails(name, null, null, null)).id());
    }
}
