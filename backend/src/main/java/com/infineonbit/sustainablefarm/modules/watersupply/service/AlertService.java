package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emission des alertes automatiques du module (qualite hors seuil, fuite probable,
 * irrigation reportee pour cause de pluie, maintenance a planifier).
 *
 * <p>Ces alertes ne proviennent d'aucun utilisateur connecte : elles sont rattachees au compte
 * technique {@link SystemUsers#IOT_SYSTEM_USER_ID}, puisqu'elles doivent etre visibles dans le
 * centre de notifications en attendant que la plateforme globale les reassigne.</p>
 */
@Service
public class AlertService {

    /** Fenetre anti-doublon : une meme alerte n'est pas repetee deux fois dans cet intervalle. */
    private static final Duration DEDUPLICATION_WINDOW = Duration.ofHours(12);

    private final NotificationRepository notificationRepository;

    public AlertService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Cree une alerte (notification) destinee au compte technique du module.
     *
     * @param type     niveau/type d'alerte : info, warning, critical
     * @param title    titre court
     * @param message  message detaille affiche dans le centre de notifications
     * @param actionUrl route de l'application a ouvrir pour traiter l'alerte (nullable)
     */
    @Transactional
    public Notification raise(String type, String title, String message, String actionUrl) {
        Notification notification = new Notification();
        notification.setUserId(SystemUsers.IOT_SYSTEM_USER_ID);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setActionUrl(actionUrl);
        return notificationRepository.save(notification);
    }

    /**
     * Variante anti-doublon utilisee pour les mesures repetitives des capteurs (une consommation
     * anormale toutes les minutes ne doit pas generer une alerte par mesure).
     *
     * @return l'alerte creee, ou un {@link Optional#empty()} si une alerte active identique existe deja
     */
    @Transactional
    public Optional<Notification> raiseOnce(String type, String title, String message, String actionUrl) {
        Instant threshold = Instant.now().minus(DEDUPLICATION_WINDOW);
        boolean alreadyRaised = notificationRepository.findAll().stream()
                .anyMatch(existing -> title.equals(existing.getTitle())
                        && Boolean.FALSE.equals(existing.getRead())
                        && existing.getCreatedAt() != null
                        && existing.getCreatedAt().isAfter(threshold));
        if (alreadyRaised) {
            return Optional.empty();
        }
        return Optional.of(raise(type, title, message, actionUrl));
    }
}
