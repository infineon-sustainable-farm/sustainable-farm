-- Compte technique "systeme" (aucune connexion possible) :
-- les alertes generees automatiquement par les capteurs IoT (fuite, qualite hors seuil,
-- irrigation reportee pour cause de pluie) n'ont pas d'utilisateur connecte.
-- L'authentification etant deleguee au logiciel global, ces alertes sont rattachees a cet
-- identifiant technique fixe (le meme que irrigation_schedules.created_by) et pourront etre
-- reaffectees au bon utilisateur par la plateforme globale.
INSERT INTO users (id, created_at, first_name, last_name, email, password_hash, status)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    NOW(),
    'Systeme',
    'IoT',
    'systeme.iot@watersupply.local',
    'NO_LOGIN_SYSTEM_ACCOUNT',
    FALSE
)
ON CONFLICT (id) DO NOTHING;
