package com.infineonbit.sustainablefarm.modules.watersupply.config;

import java.util.UUID;

/**
 * Identifiants techniques du module watersupply.
 *
 * <p>L'authentification est deleguee au logiciel global : le module n'a donc aucun utilisateur
 * connecte. Deux besoins restent couverts par un identifiant fixe (ligne creee par la migration
 * {@code V4__iot_system_user.sql}, sans mot de passe utilisable, donc sans possibilite de connexion) :</p>
 * <ul>
 *   <li>{@code irrigation_schedules.created_by} : la colonne est obligatoire ;</li>
 *   <li>{@code notifications.user_id} : la colonne est obligatoire, les alertes issues des
 *       capteurs IoT (fuite, qualite hors seuil, report pour cause de pluie) doivent avoir
 *       un destinataire, reaffectable ensuite par la plateforme globale.</li>
 * </ul>
 */
public final class SystemUsers {

    /** Compte technique "systeme IoT" - jamais utilise pour se connecter. */
    public static final UUID IOT_SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private SystemUsers() {
    }
}
