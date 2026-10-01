package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.SystemUsers;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Notification;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

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
        Notification saved = notificationRepository.save(notification);
        // Trace d'exploitation : sans elle, une alerte automatique ne laissait aucune trace
        // dans les journaux alors que c'est souvent la seule manifestation visible d'un incident.
        log.info("Alert raised [{}] {} (action: {})", type, title, actionUrl);
        return saved;
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
        // Verification en base (titre + alerte encore ouverte + fenetre recente) : plus de parcours
        // complet de la table a chaque mesure de capteur, et le classement reste deterministe.
        boolean alreadyRaised = notificationRepository
                .existsByTitleAndReadIsFalseAndCreatedAtAfter(title, threshold);
        if (alreadyRaised) {
            return Optional.empty();
        }
        return Optional.of(raise(type, title, message, actionUrl));
    }
}
