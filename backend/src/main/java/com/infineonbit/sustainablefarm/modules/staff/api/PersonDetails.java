package com.infineonbit.sustainablefarm.modules.staff.api;

/**
 * Attributes of a person sent by the owner module to {@link PersonDirectory#create} and
 * {@link PersonDirectory#update}.
 *
 * @param fullName full name; required to create a person
 * @param email    email, unique when present; blank means none
 * @param phone    phone number; blank means none
 * @param active   a new person is active when {@code null}
 *
 * <p>On {@link PersonDirectory#update}, a {@code null} attribute keeps the current value and a
 * blank email or phone clears it.</p>
 */
public record PersonDetails(String fullName, String email, String phone, Boolean active) {
}
